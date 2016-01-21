package org.dsa.iot.netty

import scala.collection.JavaConverters.mapAsJavaMapConverter

import org.dsa.iot.AbstractSpec
import org.dsa.iot.dslink.connection.NetworkClient
import org.dsa.iot.dslink.util.URLInfo
import org.dsa.iot.dslink.util.http.WsClient
import org.dsa.iot.dslink.util.json.{ EncodingFormat, JsonObject }
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar

/**
 * Custom Netty artifacts test suite.
 */
class CustomNettySpec extends AbstractSpec with MockitoSugar {

  val http = new CustomHttpProvider

  "CustomHttpProvider.post" should {
    "fail for missing url" in {
      a[NullPointerException] should be thrownBy http.post(null, null, null)
    }
    "fail for bad url" in {
      val url = new URLInfo("unknown", "unknown", 80, "/", false)
      a[RuntimeException] should be thrownBy http.post(url, null, null)
    }
    "correctly return for unknown path" in {
      val url = new URLInfo("http", "www.google.com", 80, "/unknown_path", false)
      http.post(url, "none".getBytes, null)
    }
    "fail for security error" in {
      val url = new URLInfo("https", "www.google.com", 80, "/unknown_path", true)
      a[RuntimeException] should be thrownBy http.post(url, "none".getBytes, null)
    }
    "work for valid arguments" in {
      val url = new URLInfo("http", "www.google.com", 80, "/", false)
      http.post(url, "none".getBytes, Map("header1" -> "value1").asJava)
    }
  }

  val ws = new CustomWsProvider

  val client1 = mock[WsClient]
  when(client1.getUrl).thenReturn(null)

  val client2 = mock[WsClient]
  when(client2.getUrl).thenReturn(new URLInfo("%", "unknown", 80, "/", false))

  val client3 = mock[WsClient]
  when(client3.getUrl).thenReturn(new URLInfo("http", "www.google.com", 80, "/", true))

  val client4 = new WsClient(new URLInfo("ws", "echo.websocket.org", 80, "/", false)) {
    def onData(data: Array[Byte], offset: Int, length: Int) = {}
    def onConnected(writer: NetworkClient) = {
      if (writer.isConnected) {}
      if (writer.writable) {}
      val msg = new JsonObject("""{"message":"hello"}""")
      writer.write(EncodingFormat.JSON, msg)
      writer.write(EncodingFormat.MESSAGE_PACK, msg)
      writer.close
    }
    def onDisconnected() = {}
    def onThrowable(throwable: Throwable) = {}
  }

  "CustomWsProvider" should {
    "fail for missing client" in {
      a[NullPointerException] should be thrownBy ws.connect(null)
    }
    "fail for missing client url" in {
      a[NullPointerException] should be thrownBy ws.connect(client1)
    }
    "fail for bad client url" in {
      a[RuntimeException] should be thrownBy ws.connect(client2)
    }
    "fail for security error" in {
      a[Exception] should be thrownBy ws.connect(client3)
    }
    "work for valid parameters" in {
      ws.connect(client4)
    }
  }
}