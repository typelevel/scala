package policy
package building

import sbt._, Keys._
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

  def ant           = "org.apache.ant"                 %            "ant"            % "1.9.4"
  def diffutils     = "com.googlecode.java-diff-utils" %         "diffutils"         % "1.3.0"
  def jline         = "jline"                          %           "jline"           %  "2.11"
  def slf4jApi      = "org.slf4j"                      %         "slf4j-api"         % "1.7.7"
  def logback       = "ch.qos.logback"                 %      "logback-classic"      % "1.1.2"
  def scalaParsers  = "org.scala-lang.modules"         %% "scala-parser-combinators" % "1.0.1"
  def scalaXml      = "org.scala-lang.modules"         %%        "scala-xml"         % "1.0.2"
  def scalacheck    = "org.scalacheck"                 %%        "scalacheck"        % "1.11.4"
  def testInterface = SbtOrg                           %       "test-interface"      %  "1.0"
}

final class ProjectOps(val p: Project) {
  def useSourcePath        = p settings (scalacOptions in Compile <++= sourcePathOpts)
  def noArtifacts          = p settings (publish := (), publishLocal := (), unmanagedSourceDirectories := Nil)
  def addMima(m: ModuleID) = p also mimaDefaultSettings also (previousArtifact := Some(m))

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
  final val Compiler = "compiler"
  final val Library  = "library"
  final val Compat   = "compat"

  def apply(id: String): SettingSeq = universal ++ (id match {
    case Root     => root
    case Compat   => compat
    case Compiler => compiler
    case Library  => library
  })

  // Boilerplate to get the prebuilt asm jar attached to the compiler metadata.
  val asmJarKey     = taskKey[File]("asm jar")
  def asm           = PolicyOrg % "asm" % asmVersion
  def asmVersion    = "5.0.4-SNAPSHOT"
  def asmJarTask    = Def task buildBase.value / "lib" / s"asm-$asmVersion.jar"
  def asmSettings   = Seq(asmJarKey <<= asmJarTask) ++ addArtifact(Artifact("asm"), asmJarKey).settings
  def asmAttributed = Def task newCpElem(asmJarTask.value, Artifact("asm"), asm, ScalaTool)

  // Assembled settings for projects which produce an artifact.
  def codeProject(others: Setting[_]*) = compiling ++ publishing ++ others

  // Settings added to every project.
  def universal = bintraySettings ++ List(
                           name ~=  (dash(PolicyName, _)),
                        version :=  "1.0.0-SNAPSHOT",
                   scalaVersion :=  ScalaKnownVersion,
             scalaBinaryVersion :=  "2.11",
                       licenses :=  Seq("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
               autoScalaLibrary :=  false,
                     crossPaths :=  false,
           managedScalaInstance :=  false,
                  sourcesInBase :=  false,
                    logBuffered :=  false,
                    showSuccess :=  false,
                     showTiming :=  true,
                     traceLevel :=  20,
              ivyConfigurations +=  ScalaTool,
                      resolvers +=  bintrayPaulpResolver,
       unmanagedJars in Compile ++= buildLevelJars.value,
                  scalaInstance <<= scalaInstance in ThisBuild
  )

  def compiler = codeProject(
    unmanagedSourceDirectories in Compile <++= allInSrc("compiler reflect repl"),
       unmanagedSourceDirectories in Test  +=  buildBase.value / "partest" / "src",
                    unmanagedBase in Test  :=  buildBase.value / "partest" / "testlib",
                             fork in Test  :=  true,
                                     test <<=  runAllTests,
                                 testOnly <<=  runTests
  )

  def compat   = List(sourceGenerators in Compile <+= createUnzipTask)

  def library = codeProject(
                   scalaSource in Compile <<=  inSrc(Library),
    unmanagedSourceDirectories in Compile <++= allInSrc("forkjoin library"),
                 scalacOptions in Compile ++=  Seq("-sourcepath", inSrc(Library).value.getPath),
                         previousArtifact  :=  Some(scalaLibrary),
                       binaryIssueFilters ++=  MimaPolicy.filters,
                                     test  :=  MimaKeys.reportBinaryIssues.value
  )
  def root = List(
                                 name :=  PolicyName,
            organization in ThisBuild :=  PolicyOrg,
                             getScala :=  scalaInstanceTask.evaluated,
                                  run :=  asInputTask(forkCompiler).evaluated,
                                 repl :=  asInputTask(forkRepl).evaluated,
           initialCommands in console :=  "import scala.reflect.runtime.universe._",
    initialCommands in consoleProject :=  "import policy.building._",
                         watchSources ++= sbtFilesInBuild.value ++ sourceFilesInProject.value,
       bootstrapModuleId in ThisBuild :=  chooseBootstrap,
                  libraryDependencies +=  bootstrapModuleId.value % ScalaTool.name,
           scalaInstance in ThisBuild <<= scalaInstanceFromModuleIDTask,
                             commands ++= bootstrapCommands

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
         javacOptions in (Test, compile) :=  wordSeq("-nowarn"),
        scalacOptions in (Test, compile) :=  wordSeq("-Xlint"),
                              incOptions :=  stdIncOptions
  )
}
