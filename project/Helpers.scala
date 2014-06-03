package policy
package building

import sbt._, Keys._

trait Helpers extends IndependentHelpers with SbtHelpers

/** Helper functions independent of sbt.
 */
trait IndependentHelpers {
  // Predicates
  def hasExtension(name: String)(exts: String*) = exts exists (name endsWith "." + _)
  def isJarName(name: String)                   = hasExtension(name)("jar", "zip")
  def isSourceName(name: String)                = hasExtension(name)("java", "scala")
  def isSource(file: jFile)                     = isSourceName(file.getName)
  def isJar(file: jFile)                        = isJarName(file.getName)
  def isScalaJar(f: File)                       = f.getPath split "/" contains ScalaOrg

  // Strings
  def oempty(xs: String*): Seq[String] = xs.toSeq map (_.trim) filterNot (_ == "")
  def strings(xs: Any*): Seq[String]   = oempty(xs.toList map ("" + _): _*)
  def wordSeq(s: String): Seq[String]  = oempty(s split "\\s+": _*)
  def wordSet(s: String): Set[String]  = wordSeq(s).toSet
  def asLines(s: String): Seq[String]  = augmentString(s.trim).lines.toSeq map (_.trim)

  def dash(elems: Any*): String = join(strings(elems: _*), sep = "-")
  def join(xs: Seq[String], sep: String = " "): String = oempty(xs: _*) mkString sep

  // System
  def javaRuntime                     = java.lang.Runtime.getRuntime
  def numCores                        = javaRuntime.availableProcessors
  def runSlurp(cmd: String): String   = Process(cmd).lines.mkString
  def fail(msg: String)               = throw new RuntimeException(msg)
  def newProps(in: (String, String)*) = ImmutableProperties(in: _*)

  // Debug
  def printResult[A](msg: String)(res: A): A = try res finally println(s"$msg: $res")
}

/** Helper functions bound up in sbt.
 */
trait SbtHelpers {
  import sbt._, Keys._
  import sbt.complete._
  import DefaultParsers._

  def ScalaTool = sbt.Configurations.ScalaTool

  // metadata keys on Classpath elems
  // analysis, artifact, moduleId, configuration
  type CpElem = Attributed[File]
  def newCpElem(file: File, art: Artifact, id: ModuleID, config: Configuration): CpElem =
    Attributed(file)(AttributeMap.empty.put(artifact.key, art).put(moduleID.key, id).put(configuration.key, config))

  // Files
  def filesIn(dir: File, extension: String): Seq[File]   = dir * s"*.$extension" get
  def filesIn(dir: File, extensions: String*): Seq[File] = extensions flatMap (ext => filesIn(dir, ext))
  def sbtFilesIn(dir: File): Seq[File]                   = filesIn(dir, "sbt")
  def sourceFilesIn(dir: File): Seq[File]                = filesIn(dir, "scala", "java")

  // Settings
  def sbtFilesInBuild      = buildBase map sbtFilesIn
  def sourceFilesInProject = buildBase map (_ / "project") map sourceFilesIn
  def sourcePathOpts       = scalaSource in Compile map (p => Seq("-sourcepath", p.getPath))
  def logger               = streams in Compile map (_.log)
  def buildBase            = baseDirectory in ThisBuild
  def projectBase          = baseDirectory in ThisProject
  def testBase             = Def setting (buildBase.value / "test")
  def srcBase              = Def setting (buildBase.value / "src")

  def allInSrc(words: String)      = Def setting (wordSeq(words) map (buildBase.value / "src" / _))
  def inSrc(name: String)          = Def setting (buildBase.value / "src" / name)
  def fromSrc(f: File => File)     = Def setting f(buildBase.value / "src")
  def fromBuild(f: File => File)   = Def setting f(buildBase.value)

  // Parsers
  def scalaVersionParser: Parser[String] = token(Space) ~> token(NotSpace, "a scala version")
  def spaceDelimited(label: String = "<arg>"): Parser[Seq[String]] = DefaultParsers spaceDelimited label
  def tokenDisplay[T](t: Parser[T], display: String): Parser[T] = DefaultParsers.tokenDisplay(t, display)
  def NotSpace = DefaultParsers.NotSpace

  // String in a:b:c form to ModuleID
  def moduleId(s: String): ModuleID = (s.trim split ':').toList match {
    case org :: artifact :: Nil                => org % artifact % "latest.release"
    case org :: artifact :: rev :: Nil         => org % artifact % rev
    case org :: artifact :: rev :: classifiers => classifiers.foldLeft(org % artifact % rev)(_ classifier _)
    case _                                     => sys.error(s"Cannot determine module id from: $s")
  }

  // Hairier sbt-internal stuff.

  def sysOrBuild(name: String): Option[String] = (sys.props get name) orElse (buildProps get name)
  def buildLevelJars = Def setting (buildBase.value / "lib" * "*.jar").get
  def localIvy: File = Path.userHome / ".ivy2" / "local" / "org.improving"
  def localBootstrapArtifacts: Seq[File] = localIvy / "bootstrap-compiler" ** "*.jar" get
  def chooseBootstrap = sysOrBuild(BootstrapModuleProperty).fold(scalaModuleId("compiler"))(moduleId)

  def scalaInstanceForVersion(version: String): TaskOf[ScalaInstance] =
    Def task scalaInstanceFromAppConfiguration(appConfiguration.value)(version)

  def scalaInstanceTask: InputTaskOf[ScalaInstance] =
    Def inputTask scalaInstanceFromAppConfiguration(appConfiguration.value)(scalaVersionParser.parsed)

  def scalaInstanceFromAppConfiguration(appConf: xsbti.AppConfiguration): String => ScalaInstance =
    version => ScalaInstance(version, appConf.provider.scalaProvider.launcher getScala version)

  def scalaInstanceFromModuleIDTask: TaskOf[ScalaInstance] = Def task {
    val report = update.value configuration ScalaTool.name getOrElse sys.error("No update report")
    val modReports = report.modules.toList
    val pairs = modReports flatMap (_.artifacts)
    val files = pairs map (_._2)
    def firstRevision = modReports.head.module.revision

    files match {
      case lib :: comp :: others => ScalaInstance(firstRevision, lib, comp, others ++ buildLevelJars.value: _*)(state.value.classLoaderCache.apply)
      case _                     => ScalaInstance(scalaVersion.value, appConfiguration.value.provider.scalaProvider.launcher getScala scalaVersion.value)
    }
  }

  def twoWords = token(StringBasic <~ Space) ~ token(StringBasic)
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
      scoped ~= f
    }
    extracted.append(newSettings, s)
  }
}
