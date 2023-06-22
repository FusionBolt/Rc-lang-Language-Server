ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.1.0"

lazy val root = (project in file("."))
  .settings(
    name := "Rc-lang-Language-Server"
  )


libraryDependencies ++= Seq(
  "org.eclipse.lsp4j" % "org.eclipse.lsp4j" % "0.20.1",
  "org.eclipse.lsp4j" % "org.eclipse.lsp4j.debug" % "0.20.1",
  "Rc-lang" % "rc-lang_3" % "0.1.0-SNAPSHOT"
)