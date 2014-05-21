package policy
package build

import sbt._, Keys._
import Runners._

trait Partest {
  def hasExtension(name: String)(exts: String*) = exts exists (name endsWith "." + _)
  def isJarName(name: String)                   = hasExtension(name)("jar", "zip")
  def isSourceName(name: String)                = hasExtension(name)("java", "scala")
  def isSource(file: File)                      = isSourceName(file.getName)
  def isJar(file: File)                         = isJarName(file.getName)

  def createUnzipTask: TaskOf[Seq[File]] = Def task (
    (dependencyClasspath in Compile).value.files filter isJar flatMap (IO.unzip(_, compatSourcesDir.value, isSourceName _).toSeq)
  )

  def compatSourcesDir = sourceManaged map (_ / "compat")

  def propertiesMapping = Def setting {
    val file = name.value + ".properties"
    val key = baseDirectory.value / file
    sLog.value.info(f"$key%60s -> $file")
    key -> file
  }

  def testJavaOptions = Props.testingProperties map ("-Xmx1g" +: _.commandLineArgs)

  def runTestsWithArgs(args: List[String]) = Def task {
    logForkJava(logger.value)(("-classpath" +: classpathReadable.value +: testJavaOptions.value :+ PartestRunnerClass) ++ args)
    runForkJava(testClasspathString(":").value, testJavaOptions.value, PartestRunnerClass, args)
  }

  def runAllTests = Def task (packageBin in Compile map (_ => runTestsWithArgs(Nil).value))
  def runTests    = Def.inputTask[Int] {
    (packageBin in Compile).value
    // testOnly with no args we'll take to mean --failed
    val args = spaceDelimited("<arg>").parsed.toList match {
      case Nil  => List("--failed", "--show-diff")
      case args => args
    }
    logForkJava(logger.value)(("-classpath" +: classpathReadable.value +: testJavaOptions.value :+ PartestRunnerClass) ++ args)
    runForkJava(testClasspathString(":").value, testJavaOptions.value, PartestRunnerClass, args)
  }
}

object Partest extends Partest
