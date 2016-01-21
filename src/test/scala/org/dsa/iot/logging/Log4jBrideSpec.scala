package org.dsa.iot.logging

import org.dsa.iot.AbstractSpec
import org.dsa.iot.dslink.util.log.LogLevel

/**
 * Log4jBridge test suite.
 */
class Log4jBrideSpec extends AbstractSpec {

  "Log4jBride" should {
    "set/get logging level" in LogLevel.values.foreach { level =>
      Log4jBridge.setLevel(level)
      Log4jBridge.getLevel shouldBe level
    }
    "be configurable" in {
      Log4jBridge.configure(null)
    }
  }
}