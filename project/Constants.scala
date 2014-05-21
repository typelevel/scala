package policy
package build

import sbt._, Keys._
import sbt.complete._
import DefaultParsers._

trait Constants {
  type SettingOf[A]         = Def.Initialize[A]
  type TaskOf[A]            = Def.Initialize[Task[A]]
  type InTaskOf[A]          = Def.Initialize[InputTask[A]]
  type Parser[+A]           = sbt.complete.Parser[A]
  type InputStream          = java.io.InputStream
  type ByteArrayInputStream = java.io.ByteArrayInputStream

  val PartestRunnerClass      = "scala.tools.partest.nest.ConsoleRunner"
  val ReplRunnerClass         = "scala.tools.nsc.MainGenericRunner"
  val CompilerRunnerClass     = "scala.tools.nsc.Main"
  val SbtFixedVersion         = Props.buildProps.`sbt.version`("0.13.2")
  val ScalaFixedVersion       = Props.buildProps.`scala.version`("2.11.1")
  val SbtFixedBinaryVersion   = SbtFixedVersion split "[.]" take 2 mkString "."
  val ScalaFixedBinaryVersion = ScalaFixedVersion split "[.]" take 2 mkString "."
  val PolicyDynamicVersion    = "latest.release"
  val PolicyOrg               = "org.improving"
  val ScalaName               = "scala"
  val PolicyName              = "policy"
  val SbtOrg                  = "org.scala-sbt"
  val ScalaOrg                = "org.scala-lang"
  val UnknownVersion          = "<unknown>"
  val NoFiles: Seq[File]      = Nil

  // Values configurable via system property
  lazy val PolicyBootstrapVersion = sys.props.getOrElse("policy.bootstrap", "scala")
  lazy val PolicyBuildVersion     = sys.props.getOrElse("policy.build", generateVersion())

  // Keys
  val bootstrapInfo     = taskKey[Unit]("summary of bootstrapping")
  val bootstrapVersion  = taskKey[String]("bootstrap version")
  val repl              = inputKey[Unit]("run policy repl")
  val tests             = inputKey[Unit]("run policy tests")
  val getScala          = inputKey[ScalaInstance]("download scala version, if not in ivy cache")
  val partestScalacArgs = settingKey[List[String]]("compile test cases with these options")

  private def generateVersion(): String =  s"1.0.0-$newBuildVersion"

  def isScalaBootstrap = PolicyBootstrapVersion == "scala"
  def scalacArgs       = wordSeq("-Ywarn-unused") // -Ywarn-unused-import")
  def partestArgs      = wordSeq("-deprecation -unchecked") //-Xlint")
  def javacArgs        = wordSeq("-nowarn -XDignore.symbol.file")

  def mimaDefaultSettings = com.typesafe.tools.mima.plugin.MimaPlugin.mimaDefaultSettings
  def previousArtifact    = com.typesafe.tools.mima.plugin.MimaKeys.previousArtifact
  def binaryIssueFilters  = com.typesafe.tools.mima.plugin.MimaKeys.binaryIssueFilters
  def ScalaTool           = sbt.Configurations.ScalaTool

  def scalaVersionParser: Parser[String] = token(Space) ~> token(NotSpace, "a scala version")
  def spaceDelimited(label: String = "<arg>"): Parser[Seq[String]] = DefaultParsers spaceDelimited label
  def tokenDisplay[T](t: Parser[T], display: String): Parser[T] = DefaultParsers.tokenDisplay(t, display)
  def NotSpace = DefaultParsers.NotSpace
}

