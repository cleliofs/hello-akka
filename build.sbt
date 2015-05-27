name := """hello-akka"""

version := "1.0"

scalaVersion := "2.11.6"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.3.11",
  "com.typesafe.akka" %% "akka-testkit" % "2.3.11",
  "com.typesafe.akka" % "akka-stream-experimental_2.11" % "1.0-RC3",
  "com.typesafe.akka" % "akka-http-core-experimental_2.11" % "1.0-RC3",
  "com.typesafe.akka" % "akka-http-experimental_2.11" % "1.0-RC3",
  "org.scalatest" %% "scalatest" % "2.2.4" % "test",
  "junit" % "junit" % "4.12" % "test",
  "com.novocode" % "junit-interface" % "0.11" % "test"
)

testOptions += Tests.Argument(TestFrameworks.JUnit, "-v")


fork in run := true

Revolver.settings