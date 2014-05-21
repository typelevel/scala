package policy
package build

import sbt._, Keys._

trait Runners {
  // envVars, workingDirectory, javaHome
  def forkOptions(f: ForkOptions => ForkOptions = identity): ForkOptions =
    f(ForkOptions(bootJars = Nil, connectInput = true, outputStrategy = Some(StdoutOutput)))

  def scalaInstanceTask: InTaskOf[ScalaInstance] = Def inputTaskDyn scalaInstanceForVersion(scalaVersionParser.parsed)

  def runInput(mainClass: String, jvmArgs: String*)(appArgs: String*) = Def.inputTask {
    val args     = spaceDelimited("<arg>").parsed.toList
    val cp       = (fullClasspath in Compile).value.files mkString ":"
    def all      = "-cp" :: cp :: jvmArgs.toList ::: (mainClass :: appArgs.toList ::: args)
    logger.value.info("java" :: all mkString " ")
    Fork.java(forkOptions(), all)
  }

  def runForkJava(classpath: String, jvmArgs: Seq[String], mainClass: String, mainArgs: Seq[String]): Int = {
    val options = ForkOptions(bootJars = Nil, connectInput = true, outputStrategy = Some(StdoutOutput))
    val args    = ("-classpath" +: classpath +: jvmArgs) ++ (mainClass +: mainArgs)
    Fork.java(options, args)
  }

  def testClasspathString(sep: String)       = fullClasspath in Test map (_ filterNot isScalaJar) map (_.files mkString sep)
  def classpathString(config: Configuration) = testClasspathString(":")
  def classpathReadable                      = testClasspathString("\n  ") map ("\n  " + _ + "\n")

  def isScalaJar(f: Attributed[File]) = f.data.getPath split "/" contains ScalaOrg
  // def noScalaLang(cp: Classpath): Classpath = cp filterNot (x => x.toString split "/" contains ScalaOrg)

  def logForkJava(logger: Logger)(args: Seq[String]): Unit = logger.info(args filterNot (_ == "") mkString ("java ", " ", ""))
}

object Runners extends Runners
