package org.dsa.iot.scala

import scala.collection.JavaConverters._

import org.dsa.iot.dslink.node.value.Value
import org.dsa.iot.dslink.util.json.{ JsonArray, JsonObject }
import org.scalacheck.{ Gen, Arbitrary }
import org.scalatest.{ BeforeAndAfterAll, Matchers, Suite, WordSpecLike }
import org.scalatest.prop.GeneratorDrivenPropertyChecks

/**
 * Base trait for test specifications, which includes matchers and scala check helpers.
 */
trait AbstractSpec extends Suite
    with WordSpecLike
    with Matchers
    with BeforeAndAfterAll
    with GeneratorDrivenPropertyChecks {

  import Arbitrary._

  object gen {
    val ids = Gen.identifier.map(_.take(10))
    val scalars = Gen.oneOf(arbitrary[Number], arbitrary[Boolean], arbitrary[String], arbitrary[Array[Byte]])
    val scalarLists = Gen.resize(10, Gen.listOf(scalars))
    val scalarJavaLists = scalarLists.map(_.asJava)
    val scalarMaps = Gen.resize(10, Gen.mapOf(Gen.zip(ids, scalars)))
    val scalarJavaMaps = scalarMaps.map(_.asJava)
    val anyLists = Gen.resize(10, Gen.listOf(Gen.oneOf(scalars, scalarLists, scalarMaps)))
    val anyMaps = Gen.resize(10, Gen.mapOf(Gen.zip(ids, Gen.oneOf(scalars, scalarLists, scalarMaps))))
    val any = Gen.oneOf(scalars, anyLists, anyMaps)
  }

  object valueGen {
    val bools = arbitrary[Boolean] map (new Value(_))

    val ints = arbitrary[Int] map (new Value(_))
    val longs = arbitrary[Long] map (new Value(_))
    val shorts = arbitrary[Short] map (new Value(_))
    val bytes = arbitrary[Byte] map (new Value(_))
    val doubles = arbitrary[Double] map (new Value(_))
    val floats = arbitrary[Float] map (new Value(_))
    val numbers = Gen.oneOf(ints, longs, shorts, bytes, doubles, floats)

    val strings = arbitrary[String] map (new Value(_))

    val binary = arbitrary[Array[Byte]] map (new Value(_))

    val scalarArrays = gen.scalarLists map (x => new JsonArray(x.asJava))

    val scalarMaps = gen.scalarMaps map (x => new JsonObject(x.asInstanceOf[Map[String, Object]].asJava))

    val arrays = scalarArrays map (new Value(_))

    val maps = scalarMaps map (new Value(_))
  }
}