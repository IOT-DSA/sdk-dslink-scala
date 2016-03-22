// properties

val APP_VERSION = "0.2.0-SNAPSHOT"

val SCALA_VERSION = "2.10.5"

val DSA_VERSION = "0.13.0"

// settings

name := "sdk-dslink-scala"

organization := "org.iot-dsa"

version := APP_VERSION

scalaVersion := SCALA_VERSION

scalacOptions ++= Seq("-feature", "-unchecked", "-deprecation", "-Xlint", 
	"-Ywarn-dead-code", "-language:_", "-target:jvm-1.7", "-encoding", "UTF-8")

run in Compile <<= Defaults.runTask(fullClasspath in Compile, mainClass in (Compile, run), runner in (Compile, run))

// scoverage options
coverageExcludedPackages := "org\\.dsa\\.iot\\.netty\\.*;org\\.dsa\\.iot\\.examples\\.*;.*DSAConnector;.*DSAEventListener"
coverageMinimum := 80
coverageFailOnMinimum := true

// publishing options

publishMavenStyle := true
publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}
pomIncludeRepository := { _ => false }
pomExtra := (
  <url>https://github.com/IOT-DSA/sdk-dslink-scala</url>
  <licenses>
    <license>
      <name>The Apache License, Version 2.0</name>
      <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
      <distribution>repo</distribution>
    </license>
  </licenses>
  <scm>
    <url>scm:git:https://github.com/IOT-DSA/sdk-dslink-scala.git</url>
    <connection>scm:git:git@github.com:IOT-DSA/sdk-dslink-scala.git</connection>
  </scm>
  <developers>
    <developer>
      <id>snark</id>
      <name>Vlad Orzhekhovskiy</name>
      <email>vlad@uralian.com</email>
      <url>http://uralian.com</url>
    </developer>
  </developers>)

pgpSecretRing := file("local.secring.gpg")

pgpPublicRing := file("local.pubring.gpg")

// dependencies
	
libraryDependencies ++= Seq(
  "com.typesafe"        % "config"                  % "1.3.0",
  "org.slf4j"           % "slf4j-log4j12"           % "1.6.1",    		
  "org.iot-dsa"         % "dslink"                  % DSA_VERSION
  		exclude("org.slf4j", "*")
  		exclude("org.iot-dsa", "logging")
  		exclude("io.netty", "*"),
  "io.netty"            % "netty-all"               % "4.0.33.Final",
  "io.reactivex"       %% "rxscala"                 % "0.25.1",
  "org.scalatest"      %% "scalatest"               % "2.2.1"         % "test",
  "org.scalacheck"     %% "scalacheck"              % "1.12.1"        % "test",
  "org.mockito"         % "mockito-core"            % "1.10.19"       % "test"
)