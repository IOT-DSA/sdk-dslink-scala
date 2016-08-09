package org.dsa.iot

import scala.collection.JavaConverters.collectionAsScalaIterableConverter

import org.dsa.iot.dslink.node.{ Node, Permission }
import org.dsa.iot.dslink.node.actions.{ ActionResult, EditorType, Parameter, ResultType }
import org.dsa.iot.dslink.node.value.ValueType

/**
 * Action utilities test suite.
 */
class ActionSupportSpec extends AbstractSpec {

  "action" should {
    "create new actions with default attributes" in {
      var result = ""
      val handler = (event: ActionResult) => { result = "done" }
      val a = createAction(handler)
      jsonArrayToList(a.getParams) shouldBe empty
      jsonArrayToList(a.getColumns) shouldBe empty
      a.getPermission shouldBe Permission.READ
      a.getResultType shouldBe ResultType.VALUES
      a.isHidden() shouldBe false
      a.invoke(null)
      result shouldBe "done"
    }
    "create new actions with all attributes" in {
      for {
        perm <- Permission.values
        rt <- ResultType.values
        hid <- List(false, true)
        a = createAction(
          handler = (event: ActionResult) => {},
          permission = perm,
          resultType = rt,
          hidden = hid)
      } {
        a.getPermission shouldBe perm
        a.getResultType shouldBe rt
        a.isHidden shouldBe hid
      }
    }
    "create new actions with parameters and results" in {
      val a = createAction(
        parameters = List(new Parameter("a", ValueType.STRING), new Parameter("b", ValueType.NUMBER)),
        results = List(new Parameter("x", ValueType.BOOL), new Parameter("y", ValueType.BINARY)),
        handler = (event: ActionResult) => {})
      a.parameters shouldBe List(Map("name" -> "a", "type" -> "string"), Map("name" -> "b", "type" -> "number"))
      a.results shouldBe List(Map("name" -> "x", "type" -> "bool"), Map("name" -> "y", "type" -> "binary"))
    }
  }

  "RichParameter" should {
    "add parameter attributes" in {
      val param = new Parameter("abc", ValueType.STRING)
      param default "100" description "ABC" editorType EditorType.PASSWORD placeHolder "a-b-c" meta Map("x" -> "y")

      param.getName shouldBe "abc"
      param.getType shouldBe ValueType.STRING
      param.getDefault shouldBe anyToValue("100")
      param.getDescription shouldBe "ABC"
      param.getEditorType shouldBe EditorType.PASSWORD
      param.getPlaceHolder shouldBe "a-b-c"
      jsonObjectToMap(param.getMetaData) shouldBe Map("x" -> "y")
    }
  }

  "RichValueType" should {
    import ValueType._

    def checkParam(param: Parameter, pName: String, pType: ValueType) =
      (param.getName, param.getType) shouldBe Tuple2(pName, pType)

    "create fixed type parameters" in {
      checkParam(STRING("a"), "a", STRING)
      checkParam(NUMBER("a"), "a", NUMBER)
      checkParam(BOOL("a"), "a", BOOL)
      checkParam(BINARY("a"), "a", BINARY)
      checkParam(ARRAY("a"), "a", ARRAY)
      checkParam(MAP("a"), "a", MAP)
    }
    "create dynamic parameters" in {
      checkParam(DYNAMIC("a"), "a", DYNAMIC)
    }
    "create enumerated parameters" in {
      val p1 = ENUMS("x", "y", "z")("a")
      p1.getName shouldBe "a"
      p1.getType.getRawName shouldBe "enum"
      p1.getType.getEnums.asScala.toList shouldBe List("x", "y", "z")

      object MyEnum extends Enumeration {
        type MyEnum = Value
        val E1, E2, E3 = Value
      }

      val p2 = ENUMS(MyEnum)("b")
      p2.getName shouldBe "b"
      p2.getType.getRawName shouldBe "enum"
      p2.getType.getEnums.asScala.toList shouldBe List("E1", "E2", "E3")
    }
  }

  "RichActionResult" should {
    val node = new Node("a", null, null)
    val json = jsonObject("params" -> jsonObject("a" -> "abc", "x" -> 123, "y" -> true,
      "z" -> "1234".getBytes, "v" -> jsonArray(1, 2, 3), "w" -> jsonObject("c" -> "C")))
    val ar = new ActionResult(node, json)

    "extract parameters without validation" in {
      ar.getParam[String]("a") shouldBe "abc"
      ar.getParam[Number]("x") shouldBe 123
      ar.getParam[Boolean]("y") shouldBe true
      ar.getParam[Array[Byte]]("z") shouldBe "1234".getBytes
      ar.getParam[List[Any]]("v") shouldBe List(1, 2, 3)
      ar.getParam[Map[String, Any]]("w") shouldBe Map("c" -> "C")
    }

    "extract numeric parameters with concrete types" in {
      ar.getParam[Int]("x") shouldBe 123
      ar.getParam[Double]("x") shouldBe 123.0
      a[NullPointerException] should be thrownBy ar.getParam[Int]("xxx")
    }

    "extract parameters with validation" in {
      ar.getParam[Number]("x", _.doubleValue > 100) shouldBe 123
      an[IllegalArgumentException] should be thrownBy ar.getParam[Int]("x", _ < 100)
      ar.getParam[String]("a", _.size == 3) shouldBe "abc"
      an[IllegalArgumentException] should be thrownBy ar.getParam[String]("a", _.size == 4)
    }
  }
}