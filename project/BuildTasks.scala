package policy
package building

import sbt._, Keys._
import complete.DefaultParsers._

trait BuildTasks {
  val repl              = inputKey[Unit]("run policy repl")
  val getScala          = inputKey[ScalaInstance]("download scala version, if not in ivy cache")
  val bootstrapModuleId = settingKey[ModuleID]("module id of bootstrap compiler")

  private def testJavaOptions  = partestProperties map ("-Xmx1g" +: _.commandLineArgs)

  def createUnzipTask: TaskOf[Seq[File]] = Def task (
    (dependencyClasspath in Compile).value.files filter isJar flatMap (IO.unzip(_, sourceManaged.value / "compat", isSourceName _).toSeq)
  )

  def generateProperties(): TaskOf[Seq[File]] = Def task {
    val id    = name.value split "[-]" last;
    val file  = (resourceManaged in Compile).value / s"$id.properties"
    val props = MutableProperties(file)
    props("version.number")              = version.value
    props("scala.version.number")        = scalaVersion.value
    props("scala.binary.version.number") = scalaBinaryVersion.value
    props("bootstrap.moduleid")          = (bootstrapModuleId in ThisBuild).value.toString
    props.save()
    Seq(file)
  }

  def runTestsWithArgs(args: List[String]): TaskOf[Int] = forkPartest map (_ apply (args: _*))
  def runAllTests: TaskOf[Unit] = Def task forkPartest.value("--all")
  def runTests = Def inputTask {
    spaceDelimited("<arg>").parsed match {
      case Nil  => forkPartest.value("--failed", "--show-diff") // testOnly with no args we'll take to mean --failed
      case args => forkPartest.value(args: _*)
    }
  }
}
