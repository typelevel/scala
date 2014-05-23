package policy
package building

import sbt._, Keys._
import sbt.complete._
import DefaultParsers._

trait Constants {
  type ParserOf[A]          = Def.Initialize[State => Parser[A]]
  type SettingOf[A]         = Def.Initialize[A]
  type TaskOf[A]            = Def.Initialize[Task[A]]
  type InputTaskOf[A]       = Def.Initialize[InputTask[A]]
  type InputStream          = java.io.InputStream
  type ByteArrayInputStream = java.io.ByteArrayInputStream
  type jMap[K, V]           = java.util.Map[K, V]
  type jFile                = java.io.File

  val buildProps              = DynamicProperties(file("project/build.properties"))
  val PartestRunnerClass      = "scala.tools.partest.nest.ConsoleRunner"
  val ReplRunnerClass         = "scala.tools.nsc.MainGenericRunner"
  val CompilerRunnerClass     = "scala.tools.nsc.Main"
  val SbtFixedVersion         = buildProps.`sbt.version`("0.13.2")
  val ScalaFixedVersion       = buildProps.`scala.version`("2.11.1")
  val SbtFixedBinaryVersion   = SbtFixedVersion split "[.]" take 2 mkString "."
  val ScalaFixedBinaryVersion = ScalaFixedVersion split "[.]" take 2 mkString "."
  val PolicyDynamicVersion    = "latest.release"
  val PolicyOrg               = "org.improving"
  val ScalaOrg                = "org.scala-lang"
  val SbtOrg                  = "org.scala-sbt"
  val PolicyName              = "policy"
  val ScalaName               = "scala"
  val UnknownVersion          = "<unknown>"
  val NoFiles: Seq[File]      = Nil
}
