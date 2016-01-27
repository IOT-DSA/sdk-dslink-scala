package org.dsa.iot.examples

import java.util.UUID

import scala.collection.JavaConverters.mapAsScalaMapConverter
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.util.{ Failure, Random, Success, Try }

import org.dsa.iot.{ DSAHelper, LinkMode, RichNodeBuilder, RichValueType }
import org.dsa.iot.dslink.link.{ Requester, Responder }
import org.dsa.iot.dslink.node.value.ValueType

import rx.lang.scala.{ Observable, Observer }

/**
 * Examples using DSAHelper functions.
 */
object DSAHelperTest extends App {

  val connector = createConnector(args)

  try {
    println("Starting requester test")
    val connection = connector start LinkMode.DUAL
    val requester = connection.requester
    val responder = connection.responder
    testInvoke(requester)
    testListAndWatch(requester)
    testNodes(requester, responder)
    println("Requester test complete")
  } finally {
    connector.stop
  }

  Thread.sleep(2000)
  System.exit(0)

  def testInvoke(implicit requester: Requester) = {
    val flowName1 = UUID.randomUUID().toString
    val obsCreate = DSAHelper.invoke("/downstream/dataflow/createDataflow", "name" -> flowName1)
    obsCreate.subscribe(new TestObserver(s"createFlow($flowName1)"))

    val flowName2 = UUID.randomUUID().toString
    val futCreate = DSAHelper.invokeAndWait("/downstream/dataflow/createDataflow", "name" -> flowName2)
    futCreate.onComplete(testFuture(s"createFlow($flowName2)"))

    val obsExport = obsCreate.flatMap(_ => DSAHelper invoke s"/downstream/dataflow/$flowName1/exportDataflow").share
    obsExport.subscribe(new TestObserver(s"exportFlow($flowName1)"))

    val futDelete = futCreate.flatMap(_ => DSAHelper invokeAndWait s"/downstream/dataflow/$flowName2/deleteDataflow")
    futDelete.onComplete(testFuture(s"deleteFlow($flowName2)"))

    val obsDelete = obsExport.flatMap(_ => DSAHelper invoke s"/downstream/dataflow/$flowName1/deleteDataflow").share
    obsDelete.subscribe(new TestObserver(s"deleteFlow($flowName1)"))

    waitToComplete(obsDelete)
    waitToComplete(futDelete)
  }

  def testListAndWatch(implicit requester: Requester) = {
    // list nodes under /downstream/System
    val obsSystem = DSAHelper list "/downstream/System"
    val sub = obsSystem subscribe (rsp => rsp.getUpdates.asScala foreach {
      case (node, flag) => println(s"""${node.getPath}${if (flag) " REMOVED" else ""}""")
    })

    // subscribe to updates for CPU and Memory usage
    val obsCpu = DSAHelper watch "/downstream/System/CPU_Usage"
    val obsCpu2 = DSAHelper watch "/downstream/System/CPU_Usage"
    val obsMem = DSAHelper watch "/downstream/System/Memory_Usage"
    val sub1 = obsCpu merge obsMem subscribe (sv => println(sv.getPath + " : " + sv.getValue))
    val sub2 = obsCpu2 subscribe (sv => println(sv.getPath + " : " + sv.getValue))

    // wait, then unsubscribe from MEM and one of CPU threads
    Thread sleep 3000
    sub1 unsubscribe

    // wait, then unsubscribe from the other CPU thread
    Thread sleep 2000
    sub2 unsubscribe

    Thread.sleep(1000)
    sub.unsubscribe
  }

  def testNodes(implicit requester: Requester, responder: Responder) = {
    // get children of /downstream
    DSAHelper getNodeChildren "/downstream" subscribe (node => println(node.getPath))

    // get values of System nodes
    for {
      of <- DSAHelper getNodeValue "/downstream/System/Open_Files"
      pl <- DSAHelper getNodeValue "/downstream/System/Platform"
      du <- DSAHelper getNodeValue "/downstream/System/Disk_Usage"
    } println(s"""System info:
    |  Open files: ${of._3}
    |  Platform: ${pl._3}
    |  Disk usage: ${du._3}
    """.stripMargin)

    // create and update own nodes
    val root = responder.getDSLink.getNodeManager.getSuperRoot
    val outNode = root createChild "out" build

    val c1 = outNode createChild "aaaa" display "Aaaa" valueType ValueType.STRING build ()

    val c2 = outNode createChild "bbbb" display "Bbbb" valueType ValueType.NUMBER build ()

    outNode createChild "setAaaa" display "Update Aaaa" action (_ => {
      DSAHelper updateNode "/out/aaaa" -> Random.nextInt(1000).toString
    }) build

    outNode createChild "setBbbb" display "Update Bbbb" action (
      parameters = List(ValueType.NUMBER("value")),
      handler = result => {
        val value = result.getParameter("value").getNumber
        DSAHelper updateNode "/out/bbbb" -> value
      }) build ()

    Thread.sleep(2000)
  }

  case class TestObserver(name: String) extends Observer[Any] {
    override def onNext(value: Any) = println(s"OBS [$name] onNext: $value")
    override def onError(error: Throwable) = Console.err.println(s"OBS [$name] error: $error")
    override def onCompleted = println(s"OBS [$name] completed")
  }

  def testFuture(name: String): PartialFunction[Try[_], Unit] = {
    case Success(x) => println(s"FUT [$name] completed: $x")
    case Failure(e) => println(s"FUT [$name] error: $e")
  }

  private def waitToComplete(obs: Observable[_]) = obs.toBlocking.toList

  private def waitToComplete(fut: Future[_]) = Await.ready(fut, Duration.Inf)
}