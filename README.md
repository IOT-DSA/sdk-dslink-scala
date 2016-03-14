# sdk-dslink-scala

![alt tag](https://travis-ci.org/IOT-DSA/sdk-dslink-scala.svg?branch=master)
![Coverage Status](https://coveralls.io/repos/github/IOT-DSA/sdk-dslink-scala/badge.svg)

DSLink Scala SDK

## Features

- Provides DSAConnector and DSAConnection classes as a communication facade.
- Tracks DSA connection lifecycle through DSAEventListener interface.
- Implements DSAHelper object for basic DSA operations, such as invoke, subscribe, set, etc.
- Exposes DSA API through Reactive Streams paradigm.
- Implements fluent Scala layer to facilitate operations with DSA artifacts.
- Recognizes all existing Node API data types.

## Usage

### DSAConnector

You can create a DSAConnector passing individual settings or the entire argument list:

```scala
val brokerUrl = ...
val configPath = ...
val connector = DSAConnector("-b", brokerUrl, "-d", configPath)
```

or

```scala
  def main(args: Array[String]): Unit = {
    val connector = DSAConnector(args)    
  }
```

You can then add listeners and initiate a new connection do DSA:

```scala
connector.addListener(new DSAEventListener {
  override def onResponderConnected(link: DSLink) = println("responder link connected @ " + link.getPath)
})
val connection = connector.start(RESPONDER)
val root = connection.responderLink.getNodeManager.getSuperRoot
root createChild "counter" valueType ValueType.NUMBER value 0 build ()
...
connector.stop
```
