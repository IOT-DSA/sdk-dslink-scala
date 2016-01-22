package org.dsa

import scala.collection.JavaConverters.{ asScalaSetConverter, mapAsScalaMapConverter, seqAsJavaListConverter }

import org.dsa.iot.Having
import org.dsa.iot.dslink.node.{ Node, NodeBuilder, Permission, Writable }
import org.dsa.iot.dslink.node.actions.{ Action, ActionResult, EditorType, Parameter, ResultType }
import org.dsa.iot.dslink.node.value.{ Value, ValueType }
import org.dsa.iot.dslink.util.handler.Handler
import org.dsa.iot.dslink.util.json.JsonObject
import org.dsa.iot.util.ValueUtils

/**
 * DSA helper types and functions.
 */
package object iot extends ValueUtils {

  /**
   * Function passed as action handler.
   */
  type ActionHandler = ActionResult => Unit

  /**
   * Extension to Node class which provides automatic Java->Scala collection converters.
   */
  implicit class RichNode(val node: Node) extends AnyVal {
    def attributes = node.getAttributes.asScala.toMap mapValues valueToAny
    def configurations = node.getConfigurations.asScala.toMap mapValues valueToAny
    def interfaces = Option(node.getInterfaces).map(_.asScala.toSet) getOrElse Set.empty
    def roConfiguration = node.getRoConfigurations.asScala.toMap mapValues valueToAny
    def children = node.getChildren.asScala.toMap
  }

  /**
   * Extension to NodeBuilder which provides Scala fluent syntax.
   */
  implicit class RichNodeBuilder(val nb: NodeBuilder) extends AnyVal {

    def display(name: String) = nb having (_.setDisplayName(name))

    def attributes(tpls: (String, Any)*) = {
      tpls foreach (t => nb.setAttribute(t._1, anyToValue(t._2)))
      nb
    }

    def config(configs: (String, Any)*) = {
      configs foreach (c => nb.setConfig(c._1, anyToValue(c._2)))
      nb
    }

    def roConfig(configs: (String, Any)*) = {
      configs foreach (c => nb.setRoConfig(c._1, anyToValue(c._2)))
      nb
    }

    def interfaces(ifaces: String*) = nb having { nb =>
      nb.setInterfaces(null)
      if (!ifaces.isEmpty)
        nb.setInterfaces(ifaces.mkString("|"))
    }

    def valueType(vType: ValueType) = nb having (_.setValueType(vType))

    def value(v: Value): NodeBuilder = nb having (_.setValue(v))

    def value(v: Any): NodeBuilder = value(anyToValue(v))

    def hidden(flag: Boolean) = nb having (_.setHidden(flag))

    def profile(p: String) = nb having (_.setProfile(p))

    def meta(md: Any) = nb having (_.setMetaData(md))

    def serializable(flag: Boolean) = nb having (_.setSerializable(flag))

    def writable(w: Writable) = nb having (_.setWritable(w))

    def action(action: Action): NodeBuilder = nb having (_.setAction(action))

    def action(handler: ActionHandler, permission: Permission = Permission.READ): NodeBuilder =
      action(new Action(permission, new Handler[ActionResult] {
        def handle(event: ActionResult) = handler(event)
      }))
  }

  /**
   * Creates a new action.
   */
  def action(handler: ActionHandler,
             parameters: Iterable[Parameter] = Nil,
             results: Iterable[Parameter] = Nil,
             permission: Permission = Permission.READ,
             resultType: ResultType = ResultType.VALUES,
             hidden: Boolean = false): Action = {
    val a = new Action(permission, new Handler[ActionResult] {
      def handle(event: ActionResult) = handler(event)
    })
    parameters foreach a.addParameter
    results foreach a.addResult
    a.setResultType(resultType)
    a.setHidden(hidden)
    a
  }

  /**
   * Extension to Action which allows to retrieve parameters and results.
   */
  implicit class RichAction(val action: Action) extends AnyVal {
    def parameters = jsonArrayToList(action.getParams)
    def results = jsonArrayToList(action.getColumns)
  }

  /**
   * Extension to Parameter which provides fluent syntax for building parameters.
   */
  implicit class RichParameter(val param: Parameter) extends AnyVal {
    def default(value: Any) = param having (_.setDefaultValue(anyToValue(value)))
    def description(value: String) = param having (_.setDescription(value))
    def editorType(value: EditorType) = param having (_.setEditorType(value))
    def placeHolder(value: String) = param having (_.setPlaceHolder(value))
    def meta(value: JsonObject): Parameter = param having (_.setMetaData(value))
    def meta(value: Map[String, Any]): Parameter = meta(mapToJsonObject(value))
  }

  /**
   * Creates a new ENUM value type from Scala enumeration.
   */
  def ENUMS(enum: Enumeration): ValueType = ENUMS(enum.values.map(_.toString).toSeq: _*)

  /**
   * Creates a new ENUM value type from a collection of strings.
   */
  def ENUMS(values: String*): ValueType = ValueType.makeEnum(values.asJava)

  /**
   * Extension to ValueType which allows creating new parameters with the given type and name.
   */
  implicit class RichValueType(val vt: ValueType) extends AnyVal {
    def apply(name: String) = new Parameter(name, vt)
  }

  /**
   * Extension to ActionResult which allows to extract parameters and results.
   */
  implicit class RichActionResult(val event: ActionResult) extends AnyVal {

    /**
     * Extracts a parameter of the specified type, optionally checking if it is valid.
     * @param T the parameter type. An implicit extractor Value=>T must exist for type T.
     * @param name parameter name.
     * @param check function which checks if the parameter value is valid. If it returns `false`,
     * `IllegalArgumentException` is thrown.
     * @param msg message to pass to the exception, if the value fails validation.
     * @return the parameter value.
     * @throws IllegalArgumentException if the parameter value fails validation.
     */
    def getParam[T](name: String, check: T => Boolean = (_: T) => true, msg: String = "")(implicit ex: Value => T): T = {
      val value = ex(event.getParameter(name))

      if (!check(value))
        throw new IllegalArgumentException(msg)
      else
        value
    }
  }

  /**
   * Helper class providing a simple syntax to add side effects to the returned value:
   *
   * {{{
   * def square(x: Int) = {
   *            x * x
   * } having (r => println "returned: " + r)
   * }}}
   *
   * or simplified
   *
   * {{{
   * def square(x: Int) = (x * x) having println
   * }}}
   */
  final implicit class Having[A](val result: A) extends AnyVal {
    def having(body: A => Unit): A = {
      body(result)
      result
    }
    def having(body: => Unit): A = {
      body
      result
    }
  }
}