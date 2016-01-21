package org.dsa

import scala.collection.JavaConverters.{ asScalaSetConverter, mapAsScalaMapConverter }

import org.dsa.iot.dslink.node.{ Node, NodeBuilder, Permission, Writable }
import org.dsa.iot.dslink.node.actions.{ Action, ActionResult }
import org.dsa.iot.dslink.node.value.{ Value, ValueType }
import org.dsa.iot.dslink.util.handler.Handler
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