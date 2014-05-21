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

  def runForkJava(classpath: String, jvmArgs: List[String], mainClass: String, mainArgs: List[String]): Int = {
    val options = ForkOptions(bootJars = Nil, connectInput = true, outputStrategy = Some(StdoutOutput))
    val args    = ("-classpath" :: classpath :: jvmArgs) ::: (mainClass :: mainArgs)
    Fork.java(options, args)
  }

  def generalClasspathString(config: Configuration, join: String) =
    Def.task[String](noScalaLangInClasspath((fullClasspath in config).value).files mkString join)

  def noScalaLangInClasspath(cp: Classpath): Classpath = cp filterNot (x => x.toString split "/" contains ScalaOrg)

  def classpathString(config: Configuration) =
    Def.task[String](generalClasspathString(config, ":").value)

  def classpathReadable =
    Def.task[String]("\n  " + generalClasspathString(Test, "\n  ").value + "\n")

  def logForkJava(logger: Logger)(args: Seq[String]): Unit = logger.info(args filterNot (_ == "") mkString ("java ", " ", ""))
}

object Runners extends Runners
