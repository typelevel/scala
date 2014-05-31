package policy
package building

import sbt._, Keys._

trait Runners {
  def partestProperties = Def task ImmutableProperties(
    "partest.scalac_opts"      -> join((scalacOptions in Test).value),
    "partest.java_opts"        -> join((javaOptions in Test).value),
    "partest.colors"           -> "256",
    "partest.threads"          -> (numCores - 1).toString,
    "partest.git_diff_options" -> "--word-diff",
    "partest.basedir"          -> buildBase.value.getPath,
    "partest.root"             -> testBase.value.getPath,
    "partest.testlib"          -> unmanagedBase.value.getPath
  )

  def forkPartest  = Def task { ForkConfig(PartestRunnerClass, props = partestProperties.value) addJvmOptions ("-Xmx1g", "-cp", testClasspathString.value) }
  def forkRepl     = Def task { ForkConfig(ReplRunnerClass, props = newProps(NoTraceSuppression -> ""), programArgs = Seq("-usejavacp")) addJvmOptions ("-cp", compilerClasspathString.value) }
  def forkCompiler = Def task { ForkConfig(CompilerRunnerClass, programArgs = Seq("-usejavacp")) addJvmOptions ("-cp", compilerClasspathString.value) }

  def asInputTask(task: TaskOf[ForkConfig]): InputTaskOf[Int] = Def inputTask task.value(spaceDelimited("<arg>").parsed: _*)

  def classpathFiles(config: Configuration, project: Reference): TaskOf[Seq[File]] = fullClasspath in config in project map (_.files)

  def testClasspathFiles: TaskOf[Seq[File]]     = classpathFiles(Test, 'compiler) map (_ filterNot isScalaJar) map (_ preferring (_.getPath contains "testlib"))
  def compilerClasspathFiles: TaskOf[Seq[File]] = classpathFiles(Compile, 'compiler)
  def compilerClasspathString: TaskOf[String]   = compilerClasspathFiles map (_ mkString pathSeparator)
  def testClasspathString: TaskOf[String]       = testClasspathFiles map (_ mkString pathSeparator)
  def testClasspathReadable: TaskOf[String]     = testClasspathFiles map (_ mkString ("\n  ", "\n  ", "\n"))

  def stdForkOptions = ForkOptions(outputStrategy = Some(StdoutOutput), connectInput = true)
  def stdIncOptions  = sbtDefaultIncOptions withRecompileOnMacroDef false //withAntStyle true

  def sbtDefaultForkOptions = Def task ForkOptions(
    javaHome         = javaHome.value,
    outputStrategy   = outputStrategy.value,
    bootJars         = Nil,
    workingDirectory = Some(baseDirectory.value),
    runJVMOptions    = javaOptions.value,
    connectInput     = connectInput.value,
    envVars          = envVars.value
  )
  def sbtConstructorForkOptions = ForkOptions(
    javaHome         = None,
    outputStrategy   = None,
    bootJars         = Nil,
    workingDirectory = None,
    runJVMOptions    = Nil,
    connectInput     = false,
    envVars          = Map.empty
  )

  //    1. recompile changed sources
  // 2(3). recompile direct dependencies and transitive public inheritance dependencies of sources with API changes in 1(2).
  //    4. further changes invalidate all dependencies transitively to avoid too many steps
  def sbtDefaultIncOptions = sbt.inc.IncOptions.Default
  // IncOptions(
  //   transitiveStep       = 3,
  //   recompileAllFraction = 0.5,
  //   relationsDebug       = false,
  //   apiDebug             = false,
  //   apiDiffContextSize   = 5,
  //   apiDumpDirectory     = None,
  //   newClassfileManager  = ClassfileManager.deleteImmediately,
  //   recompileOnMacroDef  = recompileOnMacroDefDefault,
  //   nameHashing          = nameHashingDefault,
  //   antStyle             = false
  // )
}
