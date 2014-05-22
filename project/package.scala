package policy

import sbt._, Keys._
import Configurations.ScalaTool
import Opts.resolver._
import scala.sys.process.Process
import Classpaths.{ packaged, publishConfig }


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



package object build extends policy.build.Constants with policy.build.Bootstrap {
  lazy val newBuildVersion = Process("bin/unique-version").lines.mkString

  def newProps(in: (String, Any)*): BetterProperties = new BetterProperties(in.toMap map { case (k, v) => (k, "" + v) })

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
  def wordSeq(s: String): Seq[String] = (s.trim split "\\s+").toSeq.sorted

  def printResult[A](msg: String)(res: A): A = try res finally println(s"$msg: $res")

  def logger      = Def task (streams in Compile).value.log
  def javaRuntime = java.lang.Runtime.getRuntime
  def numCores    = javaRuntime.availableProcessors

  def ant           = "org.apache.ant"                 %            "ant"            % "1.9.4"
  def asm           = "org.ow2.asm"                    %       "asm-debug-all"       % "5.0.2"
  def diffutils     = "com.googlecode.java-diff-utils" %         "diffutils"         % "1.3.0"
  def jline         = "jline"                          %           "jline"           %  "2.11"
  def scalaParsers  = "org.scala-lang.modules"         %% "scala-parser-combinators" % "1.0.1"
  def scalaXml      = "org.scala-lang.modules"         %%        "scala-xml"         % "1.0.1"
  def scalacheck    = "org.scalacheck"                 %%        "scalacheck"        % "1.11.3"
  def testInterface = SbtOrg                           %       "test-interface"      %  "1.0"

  def buildBase = baseDirectory in ThisBuild
  def testBase  = buildBase map (_ / "test")

  def sbtModuleId(name: String)    = SbtOrg    %          name          %    SbtFixedVersion
  def scalaModuleId(name: String)  = ScalaOrg  % dash(ScalaName, name)  %   ScalaFixedVersion
  def policyModuleId(name: String) = PolicyOrg % dash(PolicyName, name) % PolicyBootstrapVersion

  def globally(xs: Setting[_]*)     = inScope(Global)(xs.toList)
  def thisally(xs: Setting[_]*)     = inScope(ThisScope)(xs.toList)
  def intransitively(xs: ModuleID*) = xs.toList map (_.intransitive())

  implicit def mkAttributedFile(f: File): Attributed[File]             = Attributed blank f
  implicit def mkAttributedFiles(fs: Seq[File]): Seq[Attributed[File]] = Attributed blankSeq fs
  implicit def resolvedProjectToRef(p: ResolvedProject): ProjectRef    = ProjectRef(p.base, p.id)
  implicit def predicateToFileFilter(p: File => Boolean): FileFilter   = new FileFilter { def accept(f: File): Boolean = p(f) }

  def scalaInstanceTask: InTaskOf[ScalaInstance] = Def inputTaskDyn scalaInstanceForVersion(scalaVersionParser.parsed)

  def policyThisBuildSettings = thisally(
    initialCommands in console :=  """import policy.build._""",
                  watchSources ++= (buildBase.value * "*.sbt").get,
                  watchSources ++= (buildBase.value / "project" * "*.sbt").get
  )

  // Global settings.
  def policyGlobalSettings = globally(
           organization :=  PolicyOrg,
           scalaVersion :=  ScalaFixedVersion,
     scalaBinaryVersion :=  ScalaFixedBinaryVersion,
       sbtBinaryVersion :=  SbtFixedBinaryVersion,
       bootstrapVersion :=  PolicyDynamicVersion,
                version :=  PolicyBuildVersion,
       autoScalaLibrary :=  false,
             crossPaths :=  false,
             incOptions ~=  (_ withRecompileOnMacroDef false)
//             incOptions ~=  (_ withAntStyle true withRecompileOnMacroDef false)
  )
  private val Root     = "root"
  private val Partest  = "partest"
  private val Compat   = "compat"
  private val Compiler = "compiler"
  private val Library  = "library"

  private def mainSources(p: Project) = p.id match {
    case Partest => Nil
    case _         =>
      List(
        scalaSource in Compile := (baseDirectory in ThisBuild).value / "src" / p.id,
         javaSource in Compile <<= scalaSource in Compile
      )
  }

  private def addSettings(p: Project) = p.id match {
    case Root     => p settings (rootProjectSettings: _*)
    case Compat   => p settings (sourceGenerators in Compile <+= build.Partest.createUnzipTask, noArtifacts)
    case Partest  => p deps bootstrapCompilerId settings (scopedSubProjectSettings: _*)
    case Compiler => p deps bootstrapLibraryId  settings (mainSources(p) ++ scopedSubProjectSettings: _*) settings (unmanagedJars in Compile ++= bootstrapJars.value)
    case Library  => p deps bootstrapCompilerId settings (mainSources(p) ++ scopedSubProjectSettings: _*)
  }

  // private def partestProjectSettings = List(
  // )

  def noArtifacts = packagedArtifacts <<= packaged(Nil)

  private def rootProjectSettings = List(
                 name :=  PolicyName,
      fork in Runtime :=  true,
                  run :=  Runners.runInput(CompilerRunnerClass)("-usejavacp").evaluated,
                 repl :=  Runners.runInput(ReplRunnerClass, Props.unsuppressTraces)("-usejavacp").evaluated
  )

  private def subProjectSettings: List[Setting[_]] = List(
                       name ~=  (dash(PolicyName, _)),
          ivyConfigurations +=  ScalaTool,
                  resolvers +=  Classpaths.typesafeResolver,
                 traceLevel :=  50,
              sourcesInBase :=  false,
                logBuffered :=  false,
                showSuccess :=  false,
       pomIncludeRepository :=  (_ => false),
                  publishTo :=  Some(mavenLocalFile),
       managedScalaInstance :=  false,
              scalaInstance <<= bootstrapInstance
  )
  private def scopedSubProjectSettings = subProjectSettings ++ List(
                             packagedArtifacts <<= packaged(Seq(packageBin in Compile)),
                     publishLocalConfiguration ~=  (p => publishConfig(p.artifacts, p.ivyFile, p.checksums, p.resolverName, logging = UpdateLogging.Quiet, overwrite = false)),
                       javacOptions in Compile ++= javacArgs,
           scalacOptions in (Compile, compile) ++= scalacArgs,
                 resourceGenerators in Compile <+= Props.generateProperties(),
               javacOptions in (Test, compile) :=  wordSeq("-nowarn"),
              scalacOptions in (Test, compile) :=  wordSeq("-Xlint"),
                       publishArtifact in Test :=  false,
      publishArtifact in (Compile, packageDoc) :=  false,
      publishArtifact in (Compile, packageSrc) :=  false
  )

  implicit class ProjectOps(val p: Project) {
    def sub                             = addSettings(p)
    def mima                            = p settings (mimaDefaultSettings: _*)
    def deps(ms: ModuleID*)             = p settings (libraryDependencies ++= ms.toSeq)
    def addSourceDirs(dirs: String*)    = p settings (unmanagedSourceDirectories in Compile ++= dirs map (buildBase.value / "src" / _))
    def intransitiveDeps(ms: ModuleID*) = p settings (libraryDependencies ++= intransitively(ms: _*))
    def sbtDeps(ids: String*)           = p settings (libraryDependencies ++= intransitively(ids map sbtModuleId: _*))
    def scalaDeps(ids: String*)         = p settings (libraryDependencies ++= intransitively(ids map scalaModuleId: _*))
  }
}
