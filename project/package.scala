package policy

import sbt._, Keys._
import Configurations.ScalaTool
import Opts.resolver._
import scala.sys.process.Process
import Classpaths.publishConfig


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

  def strings(xs: Any*): List[String] = xs.toList map ("" + _)
  def join(xs: Seq[String]): String   = xs filterNot (_ == "") mkString " "
  def wordSet(s: String): Set[String] = (s.trim split "\\s+").toSet
  def wordSeq(s: String): Seq[String] = (s.trim split "\\s+").toSeq.sorted

  def printResult[A](msg: String)(res: A): A = try res finally println(s"$msg: $res")

  def logger      = Def task (streams in Compile).value.log
  def javaRuntime = java.lang.Runtime.getRuntime
  def numCores    = javaRuntime.availableProcessors

  def ant           = "org.apache.ant"                 %            "ant"            % "1.9.4"
  def asm           = "org.ow2.asm"                    %          "asm-all"          % "5.0.2"
  def diffutils     = "com.googlecode.java-diff-utils" %         "diffutils"         % "1.3.0"
  def jline         = "jline"                          %           "jline"           %  "2.11"
  def scalaParsers  = "org.scala-lang.modules"         %% "scala-parser-combinators" % "1.0.1"
  def scalaXml      = "org.scala-lang.modules"         %%        "scala-xml"         % "1.0.1"
  def scalacheck    = "org.scalacheck"                 %%        "scalacheck"        % "1.11.3"
  def testInterface = SbtOrg                           %       "test-interface"      %  "1.0"

  def buildBase                    = baseDirectory in ThisBuild

  def scalaModuleId(name: String)  = ScalaOrg  % s"scala-$name"  %   ScalaFixedVersion
  def sbtModuleId(name: String)    = SbtOrg    %    s"$name"     %    SbtFixedVersion
  def policyModuleId(name: String) = PolicyOrg % s"policy-$name" % PolicyBootstrapVersion

  // % Bootstrap.name

  def globally(xs: Setting[_]*)     = inScope(Global)(xs.toList)
  def thisally(xs: Setting[_]*)     = inScope(ThisScope)(xs.toList)
  def intransitively(xs: ModuleID*) = xs.toList map (_.intransitive())

  implicit def mkAttributedFile(f: File): Attributed[File]             = Attributed blank f
  implicit def mkAttributedFiles(fs: Seq[File]): Seq[Attributed[File]] = Attributed blankSeq fs
  implicit def resolvedProjectToRef(p: ResolvedProject): ProjectRef    = ProjectRef(p.base, p.id)
  implicit def predicateToFileFilter(p: File => Boolean): FileFilter   = new FileFilter { def accept(f: File): Boolean = p(f) }

  def scalaInstanceTask: InTaskOf[ScalaInstance] = Def inputTaskDyn scalaInstanceForVersion(scalaVersionParser.parsed)

  // Global settings.
  def policyBuildSettings = globally(
           organization :=  PolicyOrg,
           scalaVersion :=  ScalaFixedVersion,
     scalaBinaryVersion :=  ScalaFixedBinaryVersion,
       sbtBinaryVersion :=  SbtFixedBinaryVersion,
       bootstrapVersion :=  PolicyDynamicVersion,
                version :=  PolicyBuildVersion,
       autoScalaLibrary :=  false,
             crossPaths :=  false,
           watchSources ++= (buildBase.value * "*.sbt").get,
           watchSources ++= (buildBase.value / "project" * "*.sbt").get,
             incOptions ~=  (_ withRecompileOnMacroDef false)
//             incOptions ~=  (_ withAntStyle true withRecompileOnMacroDef false)
  )

  private def rootProjectSettings = List(
     publishArtifact := false,
         fork in run := true
  )

  private val Partest = "partest"
  private def mainSources(p: Project) = p.id match {
    case Partest => Nil
    case _         =>
      List(
        scalaSource in Compile := (baseDirectory in ThisBuild).value / "src" / p.id,
         javaSource in Compile <<= scalaSource in Compile
      )
  }
  private def addSettings(p: Project) = p.id match {
    case "root"     => p settings (rootProjectSettings: _*)
    case Partest    => p deps bootstrapCompilerId settings (scopedSubProjectSettings: _*)
    case "compiler" => p deps bootstrapLibraryId  settings (mainSources(p) ++ scopedSubProjectSettings: _*) settings (unmanagedJars in Compile ++= bootstrapJars.value)
    case _          => p deps bootstrapCompilerId settings (mainSources(p) ++ scopedSubProjectSettings: _*)
  }

  private def subProjectSettings: List[Setting[_]] = List(
                       name ~=  (n => s"policy-$n"),
          ivyConfigurations ++= Seq(ScalaTool, Bootstrap),
                 resolvers +=  Classpaths.typesafeResolver,
                 traceLevel :=  50,
              sourcesInBase :=  false,
                logBuffered :=  false,
                showSuccess :=  false,
                 exportJars :=  true,
       pomIncludeRepository :=  (_ => false),
                  publishTo :=  Some(mavenLocalFile),
       managedScalaInstance :=  false,
              scalaInstance <<= bootstrapInstance
  )
  private def scopedSubProjectSettings = subProjectSettings ++ List(
              // (updateConfiguration in Compile) :=  new UpdateConfiguration(retrieveConfiguration.value, missingOk = true, ivyLoggingLevel.value),
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
    def sbtTestDeps(ids: String*)       = p settings (libraryDependencies ++= intransitively(ids map sbtModuleId map (_ % "test"): _*))
    def scalaDeps(ids: String*)         = p settings (libraryDependencies ++= intransitively(ids map scalaModuleId: _*))
  }
}
