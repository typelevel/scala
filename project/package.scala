package policy

import sbt._, Keys._
import Configurations.ScalaTool
import Opts.resolver._
import scala.sys.process.Process
import Classpaths.{ packaged, publishConfig }
// scalacOptions in Compile ++= strings("-sourcepath", (scalaSource in Compile).value),
//         previousArtifact :=  Some(scalaModuleId("library")),
//       binaryIssueFilters ++= MimaPolicy.filters


// A fully-qualified reference to a setting or task looks like:
//
// {<build-uri>}<project-id>/config:inkey::key

// (updateConfiguration in Compile) :=  new UpdateConfiguration(retrieveConfiguration.value, missingOk = true, ivyLoggingLevel.value),


// private type UpElem = (String, ModuleID, Artifact, File)

// final class UpdateReportOps(val report: UpdateReport) {
//   def all                                                        = report.allModules
//   def iterator                                                   = toSeq.iterator
//   def toSeq                                                      = report.toSeq
//   def map[A](f: UpElem => A): Seq[A]                             = toSeq map f
//   def withFilter(p: UpElem => Boolean): Seq[UpElem]              = toSeq filter p
//   def filter(p: UpElem => Boolean): Seq[UpElem]                  = this withFilter p
//   def find(p: UpElem => Boolean): Option[UpElem]                 = iterator find p
//   def collect[A](pf: PartialFunction[UpElem, A]): Seq[A]         = toSeq collect pf
//   def collectFirst[A](pf: PartialFunction[UpElem, A]): Option[A] = iterator collectFirst pf
// }

// final class ConfigurationReportOps(val optReport: Option[ConfigurationReport]) {
//   def name = optReport.fold("<error>")(_.configuration)
//   def all  = optReport.fold(Seq[ModuleID]())(_.allModules)
// }

// def onUpdate[A](f: UpdateReportOps => A)                               = Def task f(new UpdateReportOps(update.value))
// def onConfig[A](config: Configuration)(f: ConfigurationReportOps => A) = Def task f(new ConfigurationReportOps(update.value configuration config.name))
// def collectUpdate[A](pf: PartialFunction[UpElem, A])                   = onUpdate(_ collect pf)
// def collectUpdateOr[A](alt: => A)(pf: PartialFunction[UpElem, A])      = onUpdate(_ collectFirst pf getOrElse alt)

// // def mapReport[A](report: UpdateReport)(f: (String, ModuleID, Artifact, File) => A) = Def.task[Seq[A]](update.value.toSeq map f)

// def sbtLoader                                                   = appConfiguration map (_.provider.loader)
// def reportFiles(report: ConfigurationReport): Seq[File]         = report.modules flatMap (_.artifacts) map (_._2)
// def reportArtifacts(report: ConfigurationReport): Seq[Artifact] = report.modules flatMap (_.artifacts) map (_._1)
// def filesInConfig(config: Configuration)                        = Def task (update.value configuration config.name).fold(Seq[File]())(reportFiles)
// def artifactsInConfig(config: Configuration)                    = Def task (update.value configuration config.name).fold(Seq[Artifact]())(reportArtifacts)

// def isPolicyLibrary  = Def setting thisProject.value.id == "library"
// def isPolicyCompiler = Def setting thisProject.value.id == "compiler"

// implicit class StateOps(s: State) {
//   def extract: Extracted = Project extract s
//   def currentProject     = extract.currentProject
// }



package object build extends policy.build.Constants with policy.build.Bootstrap with policy.build.BuildSettings {
  lazy val newBuildVersion = Process("bin/unique-version").lines.mkString

  def newProps(in: (String, String)*): BetterProperties = new BetterProperties(in.toMap)

  class BetterProperties(in: Map[String, String]) extends java.util.Properties {
    locally {
      for ((k, v) <- in) super.setProperty(k, v)
    }

    override def setProperty(key: String, value: String): Nothing = throw new RuntimeException("Immutable properties")

    def filterKeys(p: String => Boolean): BetterProperties         = new BetterProperties(in filter (kv => p(kv._1)))
    def filter(p: ((String, String)) => Boolean): BetterProperties = new BetterProperties(in filter p)

    private def map[A](f: (String, String) => A): Seq[A] = for ((k, v) <- in.toSeq) yield f(k, v)

    def commandLineArgs: Seq[String] = this map ("-D%s=%s".format(_, _))
  }

  implicit class PropertyMapOps(val map: Map[String, Any]) {
    def toJavaProps: java.util.Properties = {
      val props = new java.util.Properties
      for ((k, v) <- map) props.setProperty(k, "" + v)
      props
    }
  }

  def dash(elems: Any*): String       = elems mkString "-"
  def strings(xs: Any*): List[String] = xs.toList map ("" + _)
  def join(xs: Seq[String]): String   = xs filterNot (_ == "") mkString " "
  def wordSet(s: String): Set[String] = (s.trim split "\\s+").toSet
  def wordSeq(s: String): Seq[String] = s.trim match {
    case "" => Nil
    case s  => (s split "\\s+").toSeq.sorted
  }

  def printResult[A](msg: String)(res: A): A = try res finally println(s"$msg: $res")

  def logger      = streams in Compile map (_.log)
  def javaRuntime = java.lang.Runtime.getRuntime
  def numCores    = javaRuntime.availableProcessors

  def ant           = "org.apache.ant"                 %            "ant"            % "1.9.4"
  // def asm           = "org.ow2.asm"                    %       "asm-debug-all"       % "5.0.2"
  def diffutils     = "com.googlecode.java-diff-utils" %         "diffutils"         % "1.3.0"
  def jline         = "jline"                          %           "jline"           %  "2.11"
  // def scalaParsers  = "org.scala-lang.modules"         %% "scala-parser-combinators" % "1.0.1"
  // def scalaXml      = "org.scala-lang.modules"         %%        "scala-xml"         % "1.0.1"
  def scalacheck    = "org.scalacheck"                 %%        "scalacheck"        % "1.11.3"
  def testInterface = SbtOrg                           %       "test-interface"      %  "1.0"

  def sbtModuleId(name: String)    = SbtOrg    %          name          %    SbtFixedVersion
  def scalaModuleId(name: String)  = ScalaOrg  % dash(ScalaName, name)  %   ScalaFixedVersion
  def policyModuleId(name: String) = PolicyOrg % dash(PolicyName, name) % PolicyBootstrapVersion

  def scalaLibraryId = scalaModuleId("library")

  def globally(xs: Setting[_]*)     = inScope(Global)(xs.toList)
  // def thisally(xs: Setting[_]*)     = inScope(ThisScope)(xs.toList)
  def intransitively(xs: ModuleID*) = xs.toList map (_.intransitive())

  implicit def mkAttributedFile(f: File): Attributed[File]             = Attributed blank f
  implicit def mkAttributedFiles(fs: Seq[File]): Seq[Attributed[File]] = Attributed blankSeq fs
  implicit def resolvedProjectToRef(p: ResolvedProject): ProjectRef    = ProjectRef(p.base, p.id)
  implicit def predicateToFileFilter(p: File => Boolean): FileFilter   = new FileFilter { def accept(f: File): Boolean = p(f) }

  def scalaInstanceTask: InTaskOf[ScalaInstance] = Def inputTaskDyn scalaInstanceForVersion(scalaVersionParser.parsed)

  // def policyThisBuildSettings = thisally(
  //   initialCommands in console :=  "import policy.build._",
  //                 watchSources +=  fromBuild(_ / "build.sbt").value,
  //                 watchSources ++= (fromBuild(_ / "project").value * "*.sbt").get
  // )

  // // Global settings.
  // def policyGlobalSettings = globally(
  //          organization :=  PolicyOrg,
  //          scalaVersion :=  ScalaFixedVersion,
  //    scalaBinaryVersion :=  ScalaFixedBinaryVersion,
  //      sbtBinaryVersion :=  SbtFixedBinaryVersion,
  //      bootstrapVersion :=  PolicyDynamicVersion,
  //               version :=  PolicyBuildVersion,
  //      autoScalaLibrary :=  false,
  //            crossPaths :=  false,
  //            incOptions ~=  (_ withRecompileOnMacroDef false) // withAntStyle true
  // )
  // private val Root     = "root"
  // private val Partest  = "partest"
  // private val Compat   = "compat"
  // private val Compiler = "compiler"
  // private val Library  = "library"

  def buildBase   = baseDirectory in ThisBuild
  def projectBase = baseDirectory in ThisProject
  def testBase    = Def setting ((baseDirectory in ThisBuild).value / "test")
  def srcBase     = Def setting ((baseDirectory in ThisBuild).value / "src")

  def allInSrc(words: String)      = Def setting (wordSeq(words) map (buildBase.value / "src" / _))
  def inSrc(name: String)          = Def setting (buildBase.value / "src" / name)
  def fromSrc(f: File => File)     = Def setting f(buildBase.value / "src")
  def fromBuild(f: File => File)   = Def setting f(buildBase.value)

  // private def compilerProjectSettings = codeProject("compiler reflect repl")(addToolJars, libraryDependencies += bootstrapLibraryId)

  // private def libraryProjectSettings = codeProject("forkjoin library")(addToolJars,
  //   scalacOptions in Compile ++= Seq("-sourcepath", inSrc("library").value.getPath),
  //        libraryDependencies +=  bootstrapCompilerId,
  //           previousArtifact :=  Some(scalaLibraryId),
  //         binaryIssueFilters ++= MimaPolicy.filters
  // )

  // private def partestProjectSettings = codeProject("")(
  //    fork in Test := true,
  //   unmanagedBase := baseDirectory.value / "testlib",
  //            test := build.Partest.runAllTests.value,
  //        testOnly := build.Partest.runTests.evaluated
  // )

  // def noArtifacts   = packagedArtifacts <<= packaged(Nil)
  // def addToolJars   = unmanagedJars in Compile <++= bootstrapJars

  // private def compatProjectSettings = List(
  //   sourceGenerators in Compile <+= build.Partest.createUnzipTask
  // )
  // private def rootProjectSettings = List(
  //                     name := PolicyName,
  //         autoScalaLibrary := false,
  //     // managedScalaInstance := false,
  //                 getScala := scalaInstanceTask.evaluated,
  //                      run := Runners.runInput(CompilerRunnerClass)("-usejavacp").evaluated,
  //                     repl := Runners.runInput(ReplRunnerClass, Props.unsuppressTraces)("-usejavacp").evaluated,
  //          fork in Runtime := true
  // )

  // private def codeProject(sourceDirs: String)(others: Setting[_]*) = {
  //   val add = unmanagedSourceDirectories in Compile <++= allInSrc(sourceDirs)
  //   add +: (codeProjectInitialSettings ++ others)
  // }

  // private def codeProjectInitialSettings = List(
  //                                   name ~=  (dash(PolicyName, _)),
  //                      ivyConfigurations +=  ScalaTool,
  //                              resolvers +=  Classpaths.typesafeResolver,
  //                             traceLevel :=  50,
  //                          sourcesInBase :=  false,
  //                            logBuffered :=  false,
  //                            showSuccess :=  false,
  //                   managedScalaInstance :=  false,
  //                   pomIncludeRepository :=  (_ => false),
  //                              publishTo :=  Some(mavenLocalFile),
  //                          scalaInstance <<= bootstrapInstance,
  //                      packagedArtifacts <<= packaged(Seq(packageBin in Compile)),
  //              publishLocalConfiguration ~=  (p => publishConfig(p.artifacts, p.ivyFile, p.checksums, p.resolverName, logging = UpdateLogging.Quiet, overwrite = false)),
  //                javacOptions in Compile ++= javacArgs,
  //    scalacOptions in (Compile, compile) ++= scalacArgs,
  //          resourceGenerators in Compile <+= Props.generateProperties(),
  //        javacOptions in (Test, compile) :=  wordSeq("-nowarn"),
  //       scalacOptions in (Test, compile) :=  wordSeq("-Xlint")
  // )

  // implicit class ProjectOps(val p: Project) {
  //   private def projectSettings = p.id match {
  //     case Root     => rootProjectSettings
  //     case Compat   => compatProjectSettings
  //     case Partest  => partestProjectSettings
  //     case Compiler => compilerProjectSettings
  //     case Library  => libraryProjectSettings
  //   }

  //   def configured                                 = p also projectSettings
  //   def also(ss: Traversable[Setting[_]]): Project = p settings (ss.toSeq: _*)
  //   def deps(ms: ModuleID*)                        = p settings (libraryDependencies ++= ms.toSeq)
  //   def intransitiveDeps(ms: ModuleID*)            = deps(intransitively(ms: _*): _*)
  //   def sbtDeps(ids: String*)                      = intransitiveDeps(ids map sbtModuleId: _*)
  //   def scalaDeps(ids: String*)                    = intransitiveDeps(ids map scalaModuleId: _*)
  // }
}
