package policy
package building

import sbt._, Keys._, PolicyKeys._
import Opts.resolver._
import Classpaths.{ packaged, publishConfig }
import bintray.Plugin.{ bintraySettings, bintrayPublishSettings }
import com.typesafe.tools.mima.plugin.MimaKeys

trait Depends {
  def bintrayPaulpResolver = "bintray/paulp" at "https://dl.bintray.com/paulp/maven"

  def sbtModuleId(name: String)   = SbtOrg    %          name          %  SbtKnownVersion
  def scalaModuleId(name: String) = ScalaOrg  % dash(ScalaName, name)  % ScalaKnownVersion

  def scalaLibrary  = scalaModuleId("library")
  def scalaCompiler = scalaModuleId("compiler")

  // The source dependency has a one-character change vs. asm-debug-all 5.0.3.
  // Not using at present in favor of a binary blob in ~/lib.
  // lazy val asm = RootProject(uri("git://github.com/paulp/asm.git#scala-fixes"))
  // def asm = "org.ow2.asm" % "asm-debug-all" % "5.0.3"

  def spire         = "org.spire-math"                 %%          "spire"           % "0.7.5"
  def diffutils     = "com.googlecode.java-diff-utils" %         "diffutils"         % "1.3.0"
  def jline         = "jline"                          %           "jline"           %  "2.12"
  def slf4jApi      = "org.slf4j"                      %         "slf4j-api"         % "1.7.7"
  def logback       = "ch.qos.logback"                 %      "logback-classic"      % "1.1.2"
  def scalaParsers  = "org.scala-lang.modules"         %% "scala-parser-combinators" % "1.0.1"
  def scalaXml      = "org.scala-lang.modules"         %%        "scala-xml"         % "1.0.2"
  def scalacheck    = "org.scalacheck"                 %%        "scalacheck"        % "1.11.4"
  def testInterface = SbtOrg                           %       "test-interface"      %  "1.0"
}

final class ProjectOps(val p: Project) {
  def noArtifacts          = p settings (publish := (), publishLocal := ())
  def addMima(m: ModuleID) = p also fullMimaSettings(m)

  def fullMimaSettings(m: ModuleID) = mimaDefaultSettings ++ Seq(
    binaryIssueFilters ++= MimaPolicy.filters,
                  test <<= MimaKeys.reportBinaryIssues,
      previousArtifact :=  Some(m)
  )

  def rootSetup                                  = (p in file(".")).setup.noArtifacts
  def setup                                      = p also projectSettings(p.id)
  def also(ss: Traversable[Setting[_]]): Project = p settings (ss.toSeq: _*)
  def deps(ms: ModuleID*)                        = p settings (libraryDependencies ++= ms.toSeq)
  def intransitiveDeps(ms: ModuleID*)            = deps(ms map (_.intransitive()): _*)
  def intransitiveTestDeps(ms: ModuleID*)        = deps(ms map (m => (m % "test").intransitive): _*)
  def sbtDeps(ids: String*)                      = intransitiveDeps(ids map sbtModuleId: _*)
  def scalaDeps(ids: String*)                    = intransitiveDeps(ids map scalaModuleId: _*)
}

private object projectSettings {
  final val Root     = "root"
  // final val Repl     = "repl"
  final val Compiler = "compiler"
  final val Library  = "library"
  final val Compat   = "compat"

  def apply(id: String): SettingSeq = universal ++ (id match {
    case Root     => root
    // case Repl     => repl
    case Compat   => compat
    case Compiler => compiler
    case Library  => library
  })

  // Boilerplate to get the prebuilt asm jar attached to the compiler metadata.
  val asmJarKey     = taskKey[File]("asm jar")
  def asm           = PolicyOrg % "asm" % asmVersion
  def asmVersion    = "5.0.4-SNAPSHOT"
  def asmJarSetting = fromBase(s"lib/asm-$asmVersion.jar")
  def asmSettings   = Seq(asmJarKey <<= asmJarSetting.task) ++ addArtifact(Artifact("asm"), asmJarKey).settings
  def asmAttributed = asmJarSetting |> (newCpElem(_, Artifact("asm"), asm, ScalaTool))

  // Assembled settings for projects which produce an artifact.
  def codeProject(others: Setting[_]*) = compiling ++ publishing ++ others

  // Settings added to every project.
  def universal = bintraySettings ++ List(
                           name  ~=  (dash(PolicyName, _)),
                        version  :=  "1.0.0-M3",
                   scalaVersion  :=  ScalaKnownVersion,
             scalaBinaryVersion  :=  "2.11",
                       licenses  :=  Seq("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
               autoScalaLibrary  :=  false,
                     crossPaths  :=  false,
           managedScalaInstance  :=  false,
                  sourcesInBase  :=  false,
                    logBuffered  :=  false,
                    showSuccess  :=  false,
                     showTiming  :=  true,
                     traceLevel  :=  20,
              ivyConfigurations  +=  ScalaTool,
                      resolvers  +=  bintrayPaulpResolver,
       unmanagedJars in Compile <++= buildLevelJars.task,
                  scalaInstance <<=  scalaInstance in ThisBuild
  )

  def compiler = codeProject(
           mainSourceDirs <++= allInSrc("compiler reflect repl"),
             mainTestDirs <+=  fromBase("partest/src"),
    unmanagedBase in Test <<=  fromBase("partest/testlib"),
             fork in Test  :=  true,
                     test <<=  runAllTests,
                 testOnly <<=  runTests
  )

  // def repl = codeProject(mainSource <<= inSrc(Repl))

  def compat   = List(sourceGenerators in Compile <+= createUnzipTask)

  def library = codeProject(
            mainSource <<=  inSrc(Library),
        mainSourceDirs <++= allInSrc("forkjoin library"),
           mainOptions ++=  Seq("-sourcepath", mainSource.value.getPath),
      previousArtifact  :=  Some(scalaLibrary)
  )

  private def replJar = (artifactPath in (Compile, packageBin) in 'repl) |> Attributed.blank

  def root = List(
                                 name  :=  PolicyName,
                             jarPaths  :=  printResult("jars")(jarPathsTask.evaluated),
                             getScala <<=  scalaInstanceTask,
                      PolicyKeys.repl <<=  asInputTask(forkRepl),
                                  run <<=  asInputTask(forkCompiler),
           initialCommands in console  :=  "import scala.reflect.runtime.universe._",
    initialCommands in consoleProject  :=  "import policy.building._",
                         watchSources <++= sbtFilesInBuild.task,
                         watchSources <++= sourceFilesInProject.task,
                    bootstrapModuleId  :=  chooseBootstrap,
                  libraryDependencies <+=  bootstrapModuleId |> (_ % ScalaTool.name),
           scalaInstance in ThisBuild <<=  scalaInstanceFromModuleIDTask,
                             commands ++=  bootstrapCommands
  )
  def publishing = List(
                     checksums in publishLocal := Nil,
                             publishMavenStyle := true,
      publishArtifact in (Compile, packageDoc) := false,
      publishArtifact in (Compile, packageSrc) := false,
                     publishLocalConfiguration ~= (p => publishConfig(p.artifacts, p.ivyFile, p.checksums, p.resolverName, logging = UpdateLogging.Quiet, overwrite = false)),
                           updateConfiguration ~= (uc => new UpdateConfiguration(uc.retrieve, uc.missingOk, logging = UpdateLogging.Quiet))
  )
  def compiling = List(
           resourceGenerators in Compile <+= generateProperties(),
      javacOptions in (Compile, compile) ++= stdJavacArgs,
     scalacOptions in (Compile, compile) ++= stdScalacArgs,
         javacOptions in (Test, compile) :=  Seq("-nowarn"),
        scalacOptions in (Test, compile) :=  Seq("-Xlint"),
                              incOptions :=  stdIncOptions
  )
}
