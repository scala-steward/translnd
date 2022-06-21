package com.translnd.interceptor

import routerrpc.{ForwardHtlcInterceptRequest, ForwardHtlcInterceptResponse}

import scala.concurrent.Future

trait HTLCHandler {

  def handleHTLC(
      htlc: ForwardHtlcInterceptRequest): Future[ForwardHtlcInterceptResponse]
}
