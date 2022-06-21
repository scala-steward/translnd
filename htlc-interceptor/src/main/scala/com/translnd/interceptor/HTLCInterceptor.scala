package com.translnd.interceptor

import akka.actor.ActorSystem
import akka.stream.OverflowStrategy
import akka.stream.scaladsl._
import grizzled.slf4j.Logging
import org.bitcoins.core.util.FutureUtil
import org.bitcoins.lnd.rpc._
import routerrpc.ResolveHoldForwardAction._
import routerrpc._

import scala.concurrent.Future

class HTLCInterceptor(val lnd: LndRpcClient, handlers: Vector[HTLCHandler])(
    implicit system: ActorSystem)
    extends LndUtils
    with Logging {
  import system.dispatcher

  require(handlers.nonEmpty)

  def start(): Unit = {
    val parallelism = Runtime.getRuntime.availableProcessors()
    val (queue, source) =
      Source
        .queue[ForwardHtlcInterceptResponse](bufferSize = 200,
                                             OverflowStrategy.dropHead,
                                             maxConcurrentOffers = 10)
        .toMat(BroadcastHub.sink)(Keep.both)
        .run()

    val _ = lnd.router
      .htlcInterceptor(source)
      .mapAsync(parallelism) { request =>
        val init = ForwardHtlcInterceptResponse(request.incomingCircuitKey,
                                                ResolveHoldForwardAction.RESUME)

        val resultF = FutureUtil.foldLeftAsync(init, handlers) {
          case (accum, handler) =>
            // If the action is Fail or Unrecognized, use that otherwise let remaining
            // handlers check it
            val skipOpt = accum.action match {
              case FAIL | Unrecognized(_) => Some(accum)
              case SETTLE | RESUME        => None
            }

            skipOpt match {
              case Some(action) => Future.successful(action)
              case None =>
                handler.handleHTLC(request).map { resp =>
                  // if it is Resume, keep previous action
                  resp.action match {
                    case SETTLE | FAIL | Unrecognized(_) =>
                      resp
                    case RESUME => accum
                  }
                }
            }
        }

        resultF.flatMap(queue.offer)
      }
      .runWith(Sink.ignore)
  }
}
