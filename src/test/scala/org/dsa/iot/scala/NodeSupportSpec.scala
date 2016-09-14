package org.dsa.iot.scala

import org.dsa.iot.dslink.link.Linkable
import org.dsa.iot.dslink.node.{ Node, SubscriptionManager, Writable }
import org.dsa.iot.dslink.node.actions.ActionResult
import org.dsa.iot.dslink.node.value.ValueType
import org.mockito.Mockito.when
import org.scalacheck.Gen
import org.scalatest.mock.MockitoSugar

/**
 * Node utilities test suite.
 */
class NodeSupportSpec extends AbstractSpec with MockitoSugar {

  val subMgr = mock[SubscriptionManager]
  val link = mock[Linkable]
  when(link.getSubscriptionManager) thenReturn subMgr

  "RichNode(Builder)" should {
    val node = new Node("node", null, link)
    val builder = node.createFakeBuilder

    "get/set display name" in {
      (builder display "full name" build).getDisplayName shouldBe "full name"
    }
    "get/set attributes" in {
      builder.build.attributes shouldBe Map.empty
      val attrs = Map("a" -> 1, "b" -> List(3, 4), "c" -> true)
      (builder attributes (attrs.toSeq: _*) build).attributes shouldBe attrs
    }
    "get/set interfaces" in forAll(Gen.resize(10, Gen.listOf(gen.ids))) { ifaces =>
      (builder interfaces (ifaces: _*) build).interfaces shouldBe ifaces.toSet
    }
    "get/set config" in {
      builder.build.configurations shouldBe Map.empty
      val config = Map("a" -> "abc", "b" -> Map("x" -> 4, "y" -> "z"))
      (builder config (config.toSeq: _*) build).configurations shouldBe config
    }
    "get/set roConfig" in {
      builder.build.roConfiguration shouldBe Map.empty
      val roConfig = Map("a" -> "abc", "b" -> Map("x" -> 4, "y" -> "z"))
      (builder roConfig (roConfig.toSeq: _*) build).roConfiguration shouldBe roConfig
    }
    "get/set value type" in {
      (builder valueType ValueType.ARRAY build).getValueType shouldBe ValueType.ARRAY
      (builder valueType ValueType.NUMBER build).getValueType shouldBe ValueType.NUMBER
    }
    "get/set value" in forAll(gen.any) { x =>
      val v = anyToValue(x)
      (builder valueType v.getType value v build).getValue shouldBe v
      valueToAny((builder valueType v.getType value x build).getValue) shouldBe x
    }
    "get/set hidden" in {
      (builder hidden true build) shouldBe 'hidden
      (builder hidden false build) should not be 'hidden
    }
    "get/set profile" in {
      (builder profile "custom" build).getProfile shouldBe "custom"
    }
    "get/set metadata" in {
      (builder meta "something" build).getMetaData[String] shouldBe "something"
      (builder meta 123 build).getMetaData[Int] shouldBe 123
    }
    "get/set serializable" in {
      (builder serializable true build) shouldBe 'serializable
      (builder serializable false build) should not be 'serializable
    }
    "get/set writable" in Writable.values.foreach { w =>
      (builder writable w build).getWritable shouldBe w
    }
    "get/set action" in {
      var result = 0
      val handler = (event: ActionResult) => { result = 5 }
      (builder action (handler) build).getAction should not be null
      (builder action (handler) build).getAction.invoke(null)
      result shouldBe 5
    }
    "get children" in {
      node.children shouldBe Map.empty
      
      val child1 = node createChild "child1" build
      val child2 = node createChild "child2" build

      node.children shouldBe Map("child1" -> child1, "child2" -> child2)
    }
  }

  "Having" should {
    "work with parameter" in {
      var len = 0
      val r = "result" having (x => len = x.size)
      r shouldBe "result"
      len shouldBe r.size
    }
    "work without parameter" in forAll { (x: Int) =>
      var side = 0
      val r = "result" having (side = x)
      r shouldBe "result"
      side shouldBe x
    }
  }
}