package policy
package building

import sbt._, Keys._
import complete.DefaultParsers._
import Runners._

trait BuildTasks {
  // def renameProjects(from: String, to: String)(state: State): State = {
  //   val (from, to) = parser0.parsed
  //   transformEveryKey(name)(_.replaceAllLiterally(from, to))(state)
  // }

  val parser0 = token(StringBasic <~ Space) ~ token(StringBasic)

  // val parser0: ParserOf[(String,String)] = Def setting {
  //   (state: State) => token(StringBasic <~ Space) ~ token(StringBasic)
  // }
  // def parser1[A]: ParserOf[A => A] = Def setting {
  //   (state: State) =>
  //     ( token("scala" <~ Space) ~ token(scalaVersion.value) ) |
  //     ( token("sbt" <~ Space) ~ token(sbtVersion.value) ) |
  //     ( token("commands" <~ Space) ~ token(state.remainingCommands.size.toString) )
  // }

  def transformInEveryScope[A](taskKey: TaskKey[A], s: State, transformer: A => A): State = {
    val extracted = Project extract s
    import extracted._
    val r            = Project.relation(extracted.structure, true)
    val allDefs      = r._1s.toSeq
    val projectScope = Load projectScope currentRef
    val scopes       = allDefs filter (_.key == taskKey.key) map (_.scope) distinct
    val redefined    = scopes map (scope => taskKey in scope ~= transformer)
    val session      = extracted.session appendRaw redefined

    s"show ${taskKey.key.label}" :: BuiltinCommands.reapply(session, structure, s)
  }

  def appendInEveryScope[A](taskKey: TaskKey[Seq[A]], s: State, args: Seq[A]): State =
    transformInEveryScope[Seq[A]](taskKey, s, xs => (xs filterNot args.contains) ++ args)

  def transformEveryKey[A](settingKey: SettingKey[A])(f: A => A)(s: State): State = {
    val extracted = Project extract s
    import extracted._
    val newSettings = structure.allProjectRefs map { project =>
      val scoped = settingKey in project
      val value  = extracted get scoped
      val key    = scoped.key
      val scope  = scoped.scope

      println(s"$key in $scope  ===>  $value")
      scoped ~= f
    }
    extracted.append(newSettings, s)
    // s
    // val s1 = s.update(name.key) {
    //   case Some(name) => name.replaceAll("policy", "bootstrap")
    //   case _          => "bootstrap"
    // }
    // s1.reload
    // "publishLocal" :: s
    // refs foreach println
    // /** Sets the value associated with `key` in the custom attributes map by transforming the current value.*/
    // def update[T](key: AttributeKey[T])(f: Option[T] => T): State
  }

  // def scalaInstanceTask: InputTaskOf[ScalaInstance] = Def inputTaskDyn scalaInstanceForVersion(scalaVersionParser.parsed)

  def unsuppressTraces = "-D" + scala.sys.SystemProperties.noTraceSupression.key

  def partestProperties = Def task ImmutableProperties(
    "partest.scalac_opts"      -> join((scalacOptions in Test).value),
    "partest.java_opts"        -> join((javaOptions in (Test, compile)).value),
    "partest.colors"           -> "256",
    "partest.threads"          -> (numCores / 2).toString,
    "partest.git_diff_options" -> "--word-diff",
    "partest.basedir"          -> buildBase.value.getPath,
    "partest.root"             -> testBase.value.getPath,
    "partest.testlib"          -> unmanagedBase.value.getPath // (buildBase.value / "partest" / "testlib").getPath
  )

  private def compatSourcesDir = sourceManaged map (_ / "compat")
  private def testJavaOptions  = partestProperties map ("-Xmx1g" +: _.commandLineArgs)

  def createUnzipTask: TaskOf[Seq[File]] = Def task (
    (dependencyClasspath in Compile).value.files filter isJar flatMap (IO.unzip(_, compatSourcesDir.value, isSourceName _).toSeq)
  )

  def generateProperties(): TaskOf[Seq[File]] = Def task {
    val id = name.value split "[-]" last;
    val file = (resourceManaged in Compile).value / s"$id.properties"
    val props = DynamicProperties(file)
    props.`version.number`              = version.value
    props.`scala.version.number`        = scalaVersion.value
    props.`scala.binary.version.number` = scalaBinaryVersion.value
    props.save()
    Seq(file)
  }

  def runTestsWithArgs(args: List[String]): TaskOf[Int] = Def task {
    logForkJava(logger.value)(("-classpath" +: classpathReadable.value +: testJavaOptions.value :+ PartestRunnerClass) ++ args)
    runForkJava(testClasspathString(":").value, testJavaOptions.value, PartestRunnerClass, args)
  }

  def runAllTests: TaskOf[Unit] = runTestsWithArgs(Nil) map (_ => ())

  def runTests    = Def.inputTask[Int] {
    // testOnly with no args we'll take to mean --failed
    val args = spaceDelimited("<arg>").parsed.toList match {
      case Nil  => List("--failed", "--show-diff")
      case args => args
    }
    logForkJava(logger.value)(("-classpath" +: classpathReadable.value +: testJavaOptions.value :+ PartestRunnerClass) ++ args)
    runForkJava(testClasspathString(":").value, testJavaOptions.value, PartestRunnerClass, args)
  }
}
