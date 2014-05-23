package policy

import sbt._, Keys._, building._

sealed abstract class PolicyPackage extends Constants with BuildSettings with BuildTasks with Helpers with Depends {
  lazy val PolicyBuildVersion  = "1.0.0-" + Process("bin/unique-version").lines.mkString

  def scalacArgs  = wordSeq("-Ywarn-unused") // -Ywarn-unused-import")
  def partestArgs = wordSeq("-deprecation -unchecked") //-Xlint")
  def javacArgs   = wordSeq("-nowarn -XDignore.symbol.file")
}

package object building extends PolicyPackage {
  val bootstrapInfo     = taskKey[Unit]("summary of bootstrapping")
  val newBootstrap      = taskKey[State]("newBootstrapTask")
  val repl              = inputKey[Unit]("run policy repl")
  val getScala          = inputKey[ScalaInstance]("download scala version, if not in ivy cache")
  val bootstrapModuleId = settingKey[ModuleID]("module id of bootstrap compiler")
  val partestScalacArgs = settingKey[List[String]]("compile test cases with these options")
}
