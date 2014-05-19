package policy
package build

import sbt._, Keys._
import Runners._

trait Partest {
  def explodeSbtSources = Def task (
    (dependencyClasspath in Test).value.files
      filter (_.name startsWith "compiler-interface-src")
      flatMap (f => IO.unzip(f, (sourceManaged in Test).value / name.value, ((_: String) endsWith ".scala")).toSeq)
  )

  def propertiesMapping = Def setting {
    val file = name.value + ".properties"
    val key = baseDirectory.value / file
    sLog.value.info(f"$key%60s -> $file")
    key -> file
  }

  def bootstrapInstance: Def.Initialize[Task[ScalaInstance]] = Def.task {
    val toolReport = update.value.configuration("scala-tool").get
    val j1 :: j2 :: js = toolReport.modules.toList flatMap (_.artifacts map (_._2))
    ScalaInstance(bootVersion.value, j1, j2, js: _*)(state.value.classLoaderCache.apply)
  }

  def testJavaOptions = Def task {
    val propArgs = for ((k, v) <- Props.testingProperties.value) yield "-D%s=%s".format(k, v)
    "-Xmx1g" :: propArgs
  }

  def runTestsWithArgs(args: List[String]) = Def task {
    logForkJava(logger.value)(("-classpath" +: classpathReadable.value +: testJavaOptions.value :+ partestRunnerClass) ++ args)
    runForkJava(generalClasspathString(Test, ":").value, testJavaOptions.value, partestRunnerClass, args)
  }

  def runAllTests = Def task (packageBin in Compile map (_ => runTestsWithArgs(Nil).value))
  def runTests    = Def.inputTask[Int] {
    (packageBin in Compile).value
    // testOnly with no args we'll take to mean --failed
    val args = spaceDelimited("<arg>").parsed.toList match {
      case Nil  => List("--failed", "--show-diff")
      case args => args
    }
    logForkJava(logger.value)(("-classpath" +: classpathReadable.value +: testJavaOptions.value :+ partestRunnerClass) ++ args)
    runForkJava(classpathString(Test).value, testJavaOptions.value, partestRunnerClass, args)
  }
}

object Partest extends Partest
