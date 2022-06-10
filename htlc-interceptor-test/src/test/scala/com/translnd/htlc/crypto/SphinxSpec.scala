/*
 * CopySuccess 2019 ACINQ SAS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.translnd.htlc.crypto

import org.bitcoins.crypto._
import org.scalatest.funsuite.AnyFunSuite
import scodec.bits._

import scala.util.Success

/** Created by fabrice on 10/01/17.
  */
class SphinxSpec extends AnyFunSuite {

  import Sphinx._
  import SphinxSpec._

  test(
    "create payment packet with fixed-size payloads (reference test vector)") {
    val PacketAndSecrets(onion, sharedSecrets) =
      create(sessionKey,
             1300,
             publicKeys,
             referenceFixedSizePaymentPayloads,
             associatedData.map(_.bytes))

    val Success(DecryptedPacket(payload0, nextPacket0, sharedSecret0)) =
      peel(privKeys.head, associatedData, onion)
    val Success(DecryptedPacket(payload1, nextPacket1, sharedSecret1)) =
      peel(privKeys(1), associatedData, nextPacket0)
    val Success(DecryptedPacket(payload2, nextPacket2, sharedSecret2)) =
      peel(privKeys(2), associatedData, nextPacket1)
    val Success(DecryptedPacket(payload3, nextPacket3, sharedSecret3)) =
      peel(privKeys(3), associatedData, nextPacket2)
    val Success(DecryptedPacket(payload4, nextPacket4, sharedSecret4)) =
      peel(privKeys(4), associatedData, nextPacket3)
    assert(
      Seq(payload0,
          payload1,
          payload2,
          payload3,
          payload4) == referenceFixedSizePaymentPayloads)
    assert(
      Seq(sharedSecret0,
          sharedSecret1,
          sharedSecret2,
          sharedSecret3,
          sharedSecret4) == sharedSecrets.map(_._1))

    val packets =
      Seq(nextPacket0, nextPacket1, nextPacket2, nextPacket3, nextPacket4)
    assert(
      packets.head.hmac ==
        hex"a93aa4f40241cef3e764e24b28570a0db39af82ab5102c3a04e51bec8cca9394")
    assert(
      packets(1).hmac ==
        hex"5d1b11f1efeaa9be32eb1c74b113c0b46f056bb49e2a35a51ceaece6bd31332c")
    assert(
      packets(2).hmac ==
        hex"19ca6357b5552b28e50ae226854eec874bbbf7025cf290a34c06b4eff5d2bac0")
    assert(
      packets(3).hmac ==
        hex"16d4553c6084b369073d259381bb5b02c16bb2c590bbd9e69346cf7ebd563229")
    // this means that node #4 is the last node
    assert(
      packets(4).hmac ==
        hex"0000000000000000000000000000000000000000000000000000000000000000")
  }

  test(
    "create payment packet with variable-size payloads (reference test vector)") {
    val PacketAndSecrets(onion, sharedSecrets) =
      create(sessionKey,
             1300,
             publicKeys,
             referenceVariableSizePaymentPayloads,
             associatedData.map(_.bytes))

    val Success(DecryptedPacket(payload0, nextPacket0, sharedSecret0)) =
      peel(privKeys.head, associatedData, onion)
    val Success(DecryptedPacket(payload1, nextPacket1, sharedSecret1)) =
      peel(privKeys(1), associatedData, nextPacket0)
    val Success(DecryptedPacket(payload2, nextPacket2, sharedSecret2)) =
      peel(privKeys(2), associatedData, nextPacket1)
    val Success(DecryptedPacket(payload3, nextPacket3, sharedSecret3)) =
      peel(privKeys(3), associatedData, nextPacket2)
    val Success(DecryptedPacket(payload4, nextPacket4, sharedSecret4)) =
      peel(privKeys(4), associatedData, nextPacket3)
    assert(
      Seq(payload0,
          payload1,
          payload2,
          payload3,
          payload4) == referenceVariableSizePaymentPayloads)
    assert(
      Seq(sharedSecret0,
          sharedSecret1,
          sharedSecret2,
          sharedSecret3,
          sharedSecret4) == sharedSecrets.map(_._1))

    val packets =
      Seq(nextPacket0, nextPacket1, nextPacket2, nextPacket3, nextPacket4)
    assert(
      packets.head.hmac ==
        hex"4ecb91c341543953a34d424b64c36a9cd8b4b04285b0c8de0acab0b6218697fc")
    assert(
      packets(1).hmac ==
        hex"3d8e429a1e8d7bdb2813cd491f17771aa75670d88b299db1954aa015d035408f")
    assert(
      packets(2).hmac ==
        hex"30ad58843d142609ed7ae2b960c8ce0e331f7d45c7d705f67fd3f3978cd7b8f8")
    assert(
      packets(3).hmac ==
        hex"4ee0600ee609f1f3356b85b0af8ead34c2db4ae93e3978d15f983040e8b01acd")
    assert(
      packets(4).hmac ==
        hex"0000000000000000000000000000000000000000000000000000000000000000")
  }
}

object SphinxSpec {

  val privKeys: Seq[ECPrivateKey] = Seq(
    ECPrivateKey(
      hex"4141414141414141414141414141414141414141414141414141414141414141"),
    ECPrivateKey(
      hex"4242424242424242424242424242424242424242424242424242424242424242"),
    ECPrivateKey(
      hex"4343434343434343434343434343434343434343434343434343434343434343"),
    ECPrivateKey(
      hex"4444444444444444444444444444444444444444444444444444444444444444"),
    ECPrivateKey(
      hex"4545454545454545454545454545454545454545454545454545454545454545")
  )
  val publicKeys: Seq[ECPublicKey] = privKeys.map(_.publicKey)
  assert(
    publicKeys == Seq(
      ECPublicKey(
        hex"02eec7245d6b7d2ccb30380bfbe2a3648cd7a942653f5aa340edcea1f283686619"),
      ECPublicKey(
        hex"0324653eac434488002cc06bbfb7f10fe18991e35f9fe4302dbea6d2353dc0ab1c"),
      ECPublicKey(
        hex"027f31ebc5462c1fdce1b737ecff52d37d75dea43ce11c74d25aa297165faa2007"),
      ECPublicKey(
        hex"032c0b7cf95324a07d05398b240174dc0c2be444d96b159aa6c7f7b1e668680991"),
      ECPublicKey(
        hex"02edabbd16b41c8371b92ef2f04c1185b4f03b6dcd52ba9b78d9d7c89c8f221145")
    ))

  val sessionKey: ECPrivateKey = ECPrivateKey(
    hex"4141414141414141414141414141414141414141414141414141414141414141")

  // This test vector uses payloads with a fixed size.
  // origin -> node #0 -> node #1 -> node #2 -> node #3 -> node #4
  val referenceFixedSizePaymentPayloads: Seq[ByteVector] = Seq(
    hex"000000000000000000000000000000000000000000000000000000000000000000",
    hex"000101010101010101000000000000000100000001000000000000000000000000",
    hex"000202020202020202000000000000000200000002000000000000000000000000",
    hex"000303030303030303000000000000000300000003000000000000000000000000",
    hex"000404040404040404000000000000000400000004000000000000000000000000"
  )

  // This test vector uses variable-size payloads intertwined with fixed-size payloads.
  // origin -> node #0 -> node #1 -> node #2 -> node #3 -> node #4
  val referenceVariableSizePaymentPayloads: Seq[ByteVector] = Seq(
    hex"000000000000000000000000000000000000000000000000000000000000000000",
    hex"140101010101010101000000000000000100000001",
    hex"fd0100000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f202122232425262728292a2b2c2d2e2f303132333435363738393a3b3c3d3e3f404142434445464748494a4b4c4d4e4f505152535455565758595a5b5c5d5e5f606162636465666768696a6b6c6d6e6f707172737475767778797a7b7c7d7e7f808182838485868788898a8b8c8d8e8f909192939495969798999a9b9c9d9e9fa0a1a2a3a4a5a6a7a8a9aaabacadaeafb0b1b2b3b4b5b6b7b8b9babbbcbdbebfc0c1c2c3c4c5c6c7c8c9cacbcccdcecfd0d1d2d3d4d5d6d7d8d9dadbdcdddedfe0e1e2e3e4e5e6e7e8e9eaebecedeeeff0f1f2f3f4f5f6f7f8f9fafbfcfdfeff",
    hex"140303030303030303000000000000000300000003",
    hex"000404040404040404000000000000000400000004000000000000000000000000"
  )

  val associatedData: Some[Sha256Digest] = Some(
    Sha256Digest(
      hex"4242424242424242424242424242424242424242424242424242424242424242"))
}
