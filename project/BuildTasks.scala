package policy
package building

import sbt._, Keys._
import complete.DefaultParsers._

object PolicyKeys {
  val repl              = inputKey[Unit]("run policy repl")
  val getScala          = inputKey[ScalaInstance]("download scala version, if not in ivy cache")
  val settingsDumpFile  = settingKey[File]("file into which to record all sbt settings") in ThisBuild
  val bootstrapModuleId = settingKey[ModuleID]("module id of bootstrap compiler") in ThisBuild

  val buildBase: FileKey   = baseDirectory in ThisBuild
  val projectBase: FileKey = baseDirectory in ThisProject
  val mainSource: FileKey  = scalaSource in Compile

  val mainOptions: TaskKey[Seq[String]] = scalacOptions in Compile
  val mainSourceDirs                    = unmanagedSourceDirectories in Compile
  val mainTestDirs                      = unmanagedSourceDirectories in Test
}

trait BuildTasks {
  private def testJavaOptions  = partestProperties map ("-Xmx1g" +: _.commandLineArgs)

  private def compilePath: TaskOf[Seq[File]] = (dependencyClasspath in Compile) |> (_.files filter isJar)
  private def explode(f: File, d: File) = IO.unzip(f, d, isSourceName _).toSeq

  def createUnzipTask: TaskOf[Seq[File]] = Def task (compilePath.value flatMap (f => explode(f, sourceManaged.value / "compat")))

  def generateProperties(): TaskOf[Seq[File]] = Def task {
    val id    = name.value split "[-]" last;
    val file  = (resourceManaged in Compile).value / s"$id.properties"
    val props = MutableProperties(file)
    props("version.number")              = version.value
    props("scala.version.number")        = scalaVersion.value
    props("scala.binary.version.number") = scalaBinaryVersion.value
    props("bootstrap.moduleid")          = PolicyKeys.bootstrapModuleId.value.toString
    props.save()
    Seq(file)
  }

  def runTestsWithArgs(args: List[String]): TaskOf[Int] = forkPartest map (_ apply (args: _*))
  def runAllTests: TaskOf[Unit] = forkPartest map (_ apply "--all")
  def runTests: InputTaskOf[Unit] = Def inputTask {
    spaceDelimited("<arg>").parsed match {
      case Nil  => forkPartest.value("--failed", "--show-diff") // testOnly with no args we'll take to mean --failed
      case args => forkPartest.value(args: _*)
    }
  }
}
