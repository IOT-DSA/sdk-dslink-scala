package org.dsa.iot.scala

import org.dsa.iot.dslink.{ DSLink, DSLinkProvider }
import org.dsa.iot.dslink.link.{ Requester, Responder }
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar

/**
 * DSAConnector test suite.
 */
class DSAConnectorSpec extends AbstractSpec with MockitoSugar {

  val provider = mock[DSLinkProvider]

  val responder = mock[Responder]
  val rspLink = mock[DSLink]
  when(rspLink.getResponder) thenReturn responder

  val requester = mock[Requester]
  val reqLink = mock[DSLink]
  when(reqLink.getRequester) thenReturn requester
  
  val listener = mock[DSAEventListener]

  "LinkMode" should {
    "support RESPONDER" in {
      LinkMode.RESPONDER shouldBe 'responder
      LinkMode.RESPONDER should not be 'requester
    }
    "support REQUESTER" in {
      LinkMode.REQUESTER shouldBe 'requester
      LinkMode.REQUESTER should not be 'responder
    }
    "support DUAL" in {
      LinkMode.DUAL shouldBe 'requester
      LinkMode.DUAL shouldBe 'responder
    }
    "perform implicit Enumeration.Value conversion" in {
      LinkMode.valueToLinkMode(LinkMode.RESPONDER) shouldBe LinkMode.RESPONDER
      LinkMode.valueToLinkMode(LinkMode.REQUESTER) shouldBe LinkMode.REQUESTER
      LinkMode.valueToLinkMode(LinkMode.DUAL) shouldBe LinkMode.DUAL
    }
  }

  "DSAConfig" should {
    "process config options" in {
      DSAConfig.connectTimeout shouldBe 10000
    }
  }

  "DSAConnection" should {
    "support RESPONDER mode" in {
      noException should be thrownBy DSAConnection(LinkMode.RESPONDER, provider, rspLink, null)
      val conn = DSAConnection(LinkMode.RESPONDER, provider, rspLink, null)
      conn shouldBe 'responder
      conn should not be 'requester
      conn.provider should not be null
      conn.responderLink shouldBe rspLink
      conn.requesterLink shouldBe null
      conn.responder shouldBe responder
      conn.requester shouldBe null
      an[AssertionError] should be thrownBy DSAConnection(LinkMode.RESPONDER, provider, rspLink, reqLink)
      an[AssertionError] should be thrownBy DSAConnection(LinkMode.RESPONDER, provider, null, reqLink)
      an[AssertionError] should be thrownBy DSAConnection(LinkMode.RESPONDER, provider, null, null)
    }
    "support REQUESTER mode" in {
      noException should be thrownBy DSAConnection(LinkMode.REQUESTER, provider, null, reqLink)
      val conn = DSAConnection(LinkMode.REQUESTER, provider, null, reqLink)
      conn shouldBe 'requester
      conn should not be 'responder
      conn.provider should not be null
      conn.responderLink shouldBe null
      conn.requesterLink shouldBe reqLink
      conn.responder shouldBe null
      conn.requester shouldBe requester
      an[AssertionError] should be thrownBy DSAConnection(LinkMode.REQUESTER, provider, rspLink, reqLink)
      an[AssertionError] should be thrownBy DSAConnection(LinkMode.REQUESTER, provider, rspLink, null)
      an[AssertionError] should be thrownBy DSAConnection(LinkMode.REQUESTER, provider, null, null)
    }
    "support DUAL mode" in {
      noException should be thrownBy DSAConnection(LinkMode.DUAL, provider, rspLink, reqLink)
      val conn = DSAConnection(LinkMode.DUAL, provider, rspLink, reqLink)
      conn shouldBe 'requester
      conn shouldBe 'responder
      conn.provider should not be null
      conn.responderLink shouldBe rspLink
      conn.requesterLink shouldBe reqLink
      conn.responder shouldBe responder
      conn.requester shouldBe requester
      an[AssertionError] should be thrownBy DSAConnection(LinkMode.DUAL, provider, null, reqLink)
      an[AssertionError] should be thrownBy DSAConnection(LinkMode.DUAL, provider, rspLink, null)
      an[AssertionError] should be thrownBy DSAConnection(LinkMode.DUAL, provider, null, null)
    }
  }
}