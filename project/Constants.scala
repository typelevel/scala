package policy
package build

import sbt._, Keys._

trait Constants {
  def partestRunnerClass  = "scala.tools.partest.nest.ConsoleRunner"
  def replRunnerClass     = "scala.tools.nsc.MainGenericRunner"
  def compilerRunnerClass = "scala.tools.nsc.Main"

  def scalacArgs  = wordSeq("-optimize -Ywarn-unused -Ywarn-unused-import")
  def partestArgs = wordSeq("-optimize -deprecation -unchecked -Xlint")
  def javacArgs   = wordSeq("-nowarn")

  type Parser[+A] = sbt.complete.Parser[A]

  def mimaDefaultSettings = com.typesafe.tools.mima.plugin.MimaPlugin.mimaDefaultSettings
  def previousArtifact    = com.typesafe.tools.mima.plugin.MimaKeys.previousArtifact
  def binaryIssueFilters  = com.typesafe.tools.mima.plugin.MimaKeys.binaryIssueFilters

  def ScalaTool = sbt.Configurations.ScalaTool

  def spaceDelimited(argLabel: String = "<arg>"): Parser[Seq[String]] = sbt.complete.DefaultParsers.spaceDelimited(argLabel)

  // val bootstrapVersion  = settingKey[String]("bootstrap version") in ThisBuild
  val repl              = inputKey[Unit]("run policy repl")
  val tests             = inputKey[Unit]("run policy tests")
  val partestScalacArgs = settingKey[List[String]]("options to pass to scalac when compiling test cases")
}

