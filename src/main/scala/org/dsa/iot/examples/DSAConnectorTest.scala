package org.dsa.iot.examples

import java.io.{ File, PrintWriter }
import java.util.concurrent.CountDownLatch

import scala.collection.JavaConverters.mapAsScalaMapConverter
import scala.io.Source

import org.dsa.iot.{ DSAConnector, DSAEventListener, LinkMode, RichNodeBuilder }
import org.dsa.iot.{ intToValue, valueToInt }
import org.dsa.iot.dslink.DSLink
import org.dsa.iot.dslink.methods.requests.{ InvokeRequest, ListRequest }
import org.dsa.iot.dslink.methods.responses.{ InvokeResponse, ListResponse }
import org.dsa.iot.dslink.node.value.ValueType
import org.dsa.iot.dslink.util.handler.Handler

/**
 * Demonstrates connecting to the broker and doing basic operations.
 */

/**
 * Demonstrates connecting to the broker and doing basic operations.
 */
object DSAConnectorTest extends App {
  import org.dsa.iot.LinkMode._

  val DEFAULT_BROKER_URL = "http://localhost:8080/conn"

  private val brokerUrl = if (args.length < 1) {
    println(s"Broker URL not specified, using the default one: $DEFAULT_BROKER_URL")
    DEFAULT_BROKER_URL
  } else {
    println(s"User Broker URL: ${args(0)}")
    args(0)
  }

  {
    println("---------------------------------------------\nTesting Responder mode")
    val connector = createConnector
    println("Adding connection listener")
    connector.addListener(new DSAEventListener {
      override def onResponderConnected(link: DSLink) = println("responder link connected @ " + link.getPath)
    })
    println("Starting connector")
    val connection = connector.start(RESPONDER)
    assert(connection.isResponder)
    assert(!connection.isRequester)

    println("""Creating a child under name "Test Child" with value of 5""")
    val root = connection.responderLink.getNodeManager.getSuperRoot
    root createChild "test-child" display "Test Child" valueType ValueType.DYNAMIC value 5 build

    println("\nPress ENTER to continue")
    System.in.read
    connector.stop
  }

  {
    println("---------------------------------------------\nTesting Requester mode")
    val connector = createConnector
    println("Adding connection listener")
    connector.addListener(new DSAEventListener {
      override def onRequesterConnected(link: DSLink) = println("requester link connected: " + link.isConnected)
    })
    println("Starting connector")
    val connection = connector.start(REQUESTER)
    assert(!connection.isResponder)
    assert(connection.isRequester)

    println("Listing children of /sys node")
    val latch = new CountDownLatch(1)
    connection.requester.list(new ListRequest("/sys"), new Handler[ListResponse] {
      def handle(event: ListResponse) = {
        event.getUpdates.asScala.toMap foreach {
          case (node, flag) =>
            val vType = Option(node.getValueType) map (_.toJsonString) getOrElse "n/a"
            println(s"""${node.getPath}: $vType ${if (flag) " DELETED" else ""}""")
        }
        latch.countDown
      }
    })
    latch.await

    println("\nPress ENTER to continue")
    System.in.read
    connector.stop
  }

  {
    println("---------------------------------------------\nTesting Dual mode")
    val connector = createConnector
    println("Adding connection listener")
    connector.addListener(new DSAEventListener {
      override def onResponderConnected(link: DSLink) = println("responder link connected @ " + link.getPath)
      override def onRequesterConnected(link: DSLink) = println("requester link connected: " + link.isConnected)
    })
    println("Starting connector")
    val connection = connector.start(DUAL)
    assert(connection.isResponder)
    assert(connection.isRequester)

    // responder
    val root = connection.responderLink.getNodeManager.getSuperRoot
    root createChild "counter" valueType ValueType.NUMBER value 0 build ()
    root createChild "incCounter" display "Increment counter" action (event => {
      val node = event.getNode.getParent.getChild("counter")
      val oldVal = node.getValue: Int
      node.setValue(oldVal + 1)
    }) build ()

    // requester
    val req = new InvokeRequest(connection.requesterLink.getPath + "/incCounter")
    println(req.getPath)
    connection.requester.invoke(req, new Handler[InvokeResponse] {
      def handle(event: InvokeResponse) = {
        println("successfully invoked: " + event.getState)
      }
    })

    println("\nPress ENTER to continue")
    System.in.read
    connector.stop
  }

  System.exit(0)

  /**
   * Creates a new DSAConnector.
   */
  private def createConnector = {
    val dslinkJson = copyDslinkJson
    val nodesJsonPath = dslinkJson.getParent + "/nodes.json"
    DSAConnector("-b", brokerUrl, "-d", dslinkJson.getPath, "-n", nodesJsonPath)
  }

  /**
   * Copies the default dslink.json template to a temporary file.
   */
  private def copyDslinkJson = {
    val source = Source.fromInputStream(getClass.getResourceAsStream("/examples/dslink.json.template"))

    val dslinkFile = File.createTempFile("dslink", ".json")
    dslinkFile.deleteOnExit

    val target = new PrintWriter(dslinkFile)
    source.getLines foreach target.println
    target.close

    dslinkFile
  }
}