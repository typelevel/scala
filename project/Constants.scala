package policy
package building

import sbt._, Keys._
import sbt.complete._
import DefaultParsers._

trait Constants {
  lazy val buildProps = MutableProperties(file("project/build.properties"))
  lazy val localProps = MutableProperties(file("project/local.properties"))

  def sysOrBuild(name: String): Option[String] = (
    (sys.props get name) orElse (localProps get name) orElse (buildProps get name)
  )
  def SbtKnownVersion   = sysOrBuild("sbt.version") | "0.13.5"
  def ScalaKnownVersion = sysOrBuild("scala.version") | "2.11.2"

  type ParserOf[A]          = Def.Initialize[State => Parser[A]]
  type SettingOf[A]         = Def.Initialize[A]
  type TaskOf[A]            = Def.Initialize[Task[A]]
  type InputTaskOf[A]       = Def.Initialize[InputTask[A]]
  type FileKey              = SettingKey[File]
  type SKey[A]              = SettingKey[A]
  type SettingSeq           = Seq[Setting[_]]
  type StateMap             = State => State
  type InputStream          = java.io.InputStream
  type ByteArrayInputStream = java.io.ByteArrayInputStream
  type jMap[K, V]           = java.util.Map[K, V]
  type jFile                = java.io.File

  def BootstrapModuleProperty = "bootstrap.module"
  def PartestRunnerClass      = "scala.tools.partest.nest.ConsoleRunner"
  def ReplRunnerClass         = "scala.tools.nsc.MainGenericRunner"
  def CompilerRunnerClass     = "scala.tools.nsc.Main"
  def PolicyOrg               = "org.improving"
  def ScalaOrg                = "org.scala-lang"
  def SbtOrg                  = "org.scala-sbt"
  def PolicyName              = "policy"
  def ScalaName               = "scala"
  def NoTraceSuppression      = scala.sys.SystemProperties.noTraceSupression.key

  def stdScalacArgs  = Nil //wordSeq("-Ywarn-unused -Ywarn-unused-import -Xdev")
  def stdPartestArgs = wordSeq("-deprecation -unchecked -Xlint")
  def stdJavacArgs   = wordSeq("-nowarn -XDignore.symbol.file")
  def pathSeparator  = java.io.File.pathSeparator
  def separator      = java.io.File.separator
}
