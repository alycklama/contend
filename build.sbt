name := "contend"

lazy val commonSettings = Seq(
  organization := "org.contend",
  version := "0.1.0",
  scalaVersion := "2.11.8"
)

lazy val root = project.in(file("."))
  .settings(commonSettings: _*)
  .aggregate(core)

lazy val core = project.in(file("contend-core"))
  .settings(commonSettings: _*)
  .settings(
    name := "contend-core",
    libraryDependencies += "com.typesafe.akka" % "akka-http-experimental_2.11" % "2.4.8",
    libraryDependencies += "com.typesafe.akka" %% "akka-http-testkit" % "2.4.8",
    libraryDependencies += "org.scalatest" % "scalatest_2.11" % "3.0.0-M15",

    libraryDependencies += "org.neo4j" % "neo4j" % "3.0.3",
    libraryDependencies += "org.neo4j" % "neo4j-bolt" % "3.0.3",
    libraryDependencies += "org.neo4j.driver" % "neo4j-java-driver" % "1.0.4",
    libraryDependencies += "org.neo4j" % "neo4j-kernel" % "3.0.3" % "test",

    libraryDependencies += "org.scalatra.scalate" % "scalate-core_2.11" % "1.7.1",
    libraryDependencies += "org.scala-lang" % "scala-compiler" % "2.11.8",

    libraryDependencies += "org.scalaz" % "scalaz-core_2.11" % "7.2.4",
    libraryDependencies += "com.typesafe.play" % "play-json_2.11" % "2.5.4"
  )

lazy val blog = project.in(file("samples/blog"))
  .settings(commonSettings: _*)
  .dependsOn(core)