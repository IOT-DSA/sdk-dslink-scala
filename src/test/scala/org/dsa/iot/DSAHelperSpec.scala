package org.dsa.iot

import scala.concurrent.ExecutionContext.Implicits.global

import org.dsa.iot.dslink.DSLink
import org.dsa.iot.dslink.link.Requester
import org.dsa.iot.dslink.methods.requests.{ InvokeRequest, ListRequest, RemoveRequest, SetRequest }
import org.dsa.iot.dslink.methods.responses.{ CloseResponse, InvokeResponse, ListResponse, RemoveResponse, SetResponse, UnsubscribeResponse }
import org.dsa.iot.dslink.node.{ Node, NodeManager, NodePair }
import org.dsa.iot.dslink.node.value.SubscriptionValue
import org.dsa.iot.dslink.util.handler.Handler
import org.mockito.ArgumentCaptor
import org.mockito.Matchers.{ any, anyString }
import org.mockito.Mockito
import org.mockito.Mockito.{ doNothing, never, verify, when }
import org.scalatest.mock.MockitoSugar

/**
 * DSAHelper test suite.
 */
class DSAHelperSpec extends AbstractSpec with MockitoSugar {

  val requester = mock[Requester]
  when(requester.invoke(any[InvokeRequest], any[Handler[InvokeResponse]])).thenReturn(1)
  when(requester.list(any[ListRequest], any[Handler[ListResponse]])).thenReturn(1)
  doNothing.when(requester).subscribe(anyString, any[Handler[SubscriptionValue]])
  doNothing.when(requester).unsubscribe(anyString, any[Handler[UnsubscribeResponse]])
  doNothing.when(requester).set(any[SetRequest], any[Handler[SetResponse]])
  doNothing.when(requester).remove(any[RemoveRequest], any[Handler[RemoveResponse]])

  val node = new Node("node", null, null)
  val nodePair = mock[NodePair]
  when(nodePair.getNode).thenReturn(node)
  val nodeManager = mock[NodeManager]
  when(nodeManager.getNode(anyString, any[Boolean])).thenReturn(nodePair)
  val rspLink = mock[DSLink]
  when(rspLink.getNodeManager).thenReturn(nodeManager)
  val responder = mock[org.dsa.iot.dslink.link.Responder]
  when(responder.getDSLink).thenReturn(rspLink)

  "DSAHelper.invoke" should {
    "call requester.invoke" in {
      Mockito.reset(requester)
      DSAHelper.invoke("path", "a" -> "b", "c" -> 5)(requester)
      val captor = ArgumentCaptor.forClass(classOf[InvokeRequest])
      verify(requester).invoke(captor.capture, any[Handler[InvokeResponse]])
      captor.getValue.getPath shouldBe "path"
    }
  }

  "DSAHelper.invokeAndWait" should {
    "call requester.invoke" in {
      Mockito.reset(requester)
      DSAHelper.invokeAndWait("path", "a" -> "b", "c" -> 5)(requester)
      val captor = ArgumentCaptor.forClass(classOf[InvokeRequest])
      verify(requester).invoke(captor.capture, any[Handler[InvokeResponse]])
      captor.getValue.getPath shouldBe "path"
    }
  }

  "DSAHelper.list" should {
    "call requester.list" in {
      Mockito.reset(requester)
      DSAHelper.list("path")(requester).subscribe
      val captor = ArgumentCaptor.forClass(classOf[ListRequest])
      verify(requester).list(captor.capture, any[Handler[ListResponse]])
      captor.getValue.getPath shouldBe "path"
    }
  }

  "DSAHelper.watch" should {
    "call requester.subscribe on first subscription" in {
      Mockito.reset(requester)
      DSAHelper.watch("path1")(requester).subscribe
      val captor = ArgumentCaptor.forClass(classOf[String])
      verify(requester).subscribe(captor.capture, any[Handler[SubscriptionValue]])
      captor.getValue shouldBe "path1"
    }
    "skip requester.subscribe on subsequent subscriptions" in {
      Mockito.reset(requester)
      DSAHelper.watch("path1")(requester).subscribe
      verify(requester, never).subscribe(anyString, any[Handler[SubscriptionValue]])
    }
    "call requester.unsubscribe when the last subscription is terminated" in {
      Mockito.reset(requester)
      val obs = DSAHelper.watch("path2")(requester)
      val sub1 = obs.subscribe
      val sub2 = obs.subscribe
      sub1.unsubscribe
      sub2.unsubscribe
      verify(requester).unsubscribe(anyString, any[Handler[UnsubscribeResponse]])
    }
  }

  "DSAHelper.unwatch" should {
    "call requester.unsubscribe" in {
      Mockito.reset(requester)
      DSAHelper.unwatch("path1")(requester)
      verify(requester).unsubscribe(anyString, any[Handler[UnsubscribeResponse]])
    }
  }

  "DSAHelper.set" should {
    "call requester.set" in {
      Mockito.reset(requester)
      DSAHelper.set("path1", 123)(requester)
      val captor = ArgumentCaptor.forClass(classOf[SetRequest])
      verify(requester).set(captor.capture, any[Handler[SetResponse]])
      captor.getValue.getPath shouldBe "path1"
    }
  }

  "DSAHelper.remove" should {
    "call requester.remove" in {
      Mockito.reset(requester)
      DSAHelper.remove("path1")(requester)
      val captor = ArgumentCaptor.forClass(classOf[RemoveRequest])
      verify(requester).remove(captor.capture, any[Handler[RemoveResponse]])
      captor.getValue.getPath shouldBe "path1"
    }
  }

  "DSAHelper.close" should {
    "call requester.closeStream" in {
      Mockito.reset(requester)
      DSAHelper.close(1)(requester)
      val captor = ArgumentCaptor.forClass(classOf[Int])
      verify(requester).closeStream(captor.capture, any[Handler[CloseResponse]])
      captor.getValue shouldBe 1
    }
  }

  "DSAHelper.getNodeChildren" should {
    "call requester.list" in {
      Mockito.reset(requester)
      DSAHelper.getNodeChildren("path")(requester).subscribe
      val captor = ArgumentCaptor.forClass(classOf[ListRequest])
      verify(requester).list(captor.capture, any[Handler[ListResponse]])
      captor.getValue.getPath shouldBe "path"
    }
  }

  "DSAHelper.getNodeValue" should {
    "call requester.subscribe" in {
      Mockito.reset(requester)
      DSAHelper.getNodeValue("path3")(requester, global)
      val captor = ArgumentCaptor.forClass(classOf[String])
      verify(requester).subscribe(captor.capture, any[Handler[SubscriptionValue]])
      captor.getValue shouldBe "path3"
    }
  }

  "DSAHelper.updateNode" should {
    "call responder.getDSLink" in {
      DSAHelper.updateNode("path" -> 3)(responder)
      verify(responder).getDSLink
      verify(rspLink).getNodeManager
      verify(nodeManager).getNode("path", true)
      verify(nodePair).getNode
    }
  }
}