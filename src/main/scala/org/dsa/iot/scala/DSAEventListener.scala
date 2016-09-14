package org.dsa.iot.scala

import org.dsa.iot.dslink.DSLink

/**
 * Listener for DSA connection lifecycle events.
 */
trait DSAEventListener {

  def onResponderInitialized(link: DSLink) = {}

  def onResponderConnected(link: DSLink) = {}

  def onResponderDisconnected(link: DSLink) = {}

  def onRequesterInitialized(link: DSLink) = {}

  def onRequesterConnected(link: DSLink) = {}

  def onRequesterDisconnected(link: DSLink) = {}
}