package policy
package building

import sbt._, Keys._
import Opts.resolver._
import Classpaths.{ packaged, publishConfig }

trait Depends {
  def sbtModuleId(name: String)   = SbtOrg    %          name          %  SbtKnownVersion
  def scalaModuleId(name: String) = ScalaOrg  % dash(ScalaName, name)  % ScalaKnownVersion

  def scalaLibrary  = scalaModuleId("library")
  def scalaCompiler = scalaModuleId("compiler")

  // The source dependency has a one-character change vs. asm-debug-all 5.0.3.
  // Not using at present in favor of a binary blob in ~/lib.
  // lazy val asm = RootProject(uri("git://github.com/paulp/asm.git#scala-fixes"))
  // def asm = "org.ow2.asm" % "asm-debug-all" % "5.0.3"
  def asm           = "org.improving"                  %            "asm"            % "5.0.4-SNAPSHOT"
  def ant           = "org.apache.ant"                 %            "ant"            %     "1.9.4"
  def diffutils     = "com.googlecode.java-diff-utils" %         "diffutils"         %     "1.3.0"
  def jline         = "jline"                          %           "jline"           %      "2.11"
  def scalaParsers  = "org.scala-lang.modules"         %% "scala-parser-combinators" %     "1.0.1"
  def scalaXml      = "org.scala-lang.modules"         %%        "scala-xml"         %     "1.0.2"
  def scalacheck    = "org.scalacheck"                 %%        "scalacheck"        %     "1.11.4"
  def testInterface = SbtOrg                           %       "test-interface"      %      "1.0"
}

final class ProjectOps(val p: Project) extends ProjectSettings {
  private def projectSettings = p.id match {
    case Root     => sets.root
    case Compat   => sets.compat
    case Partest  => sets.partest
    case Compiler => sets.compiler
    case Library  => sets.library
  }

  def useSourcePath        = p settings (scalacOptions in Compile <++= sourcePathOpts)
  def noArtifacts          = p settings (publish := (), publishLocal := (), unmanagedSourceDirectories := Nil)
  def addMima(m: ModuleID) = p also mimaDefaultSettings also (previousArtifact := Some(m))

  def setup                                      = p also (sets.universal ++ projectSettings)
  def also(ss: Traversable[Setting[_]]): Project = p settings (ss.toSeq: _*)
  def deps(ms: ModuleID*)                        = p settings (libraryDependencies ++= ms.toSeq)
  def intransitiveDeps(ms: ModuleID*)            = deps(ms map (_.intransitive()): _*)
  def sbtDeps(ids: String*)                      = intransitiveDeps(ids map sbtModuleId: _*)
  def scalaDeps(ids: String*)                    = intransitiveDeps(ids map scalaModuleId: _*)
}

sealed trait ProjectSettings {
  final val Root     = "root"
  final val Compiler = "compiler"
  final val Library  = "library"
  final val Partest  = "partest"
  final val Compat   = "compat"

  protected object sets {
    // Boilerplate to get the prebuilt asm jar attached to the compiler metadata.
    val asmJarKey     = taskKey[File]("asm jar")
    def asmVersion    = "5.0.4-SNAPSHOT"
    def asmJarTask    = Def task buildBase.value / "lib" / s"asm-$asmVersion.jar"
    def asmSettings   = Seq(asmJarKey <<= asmJarTask) ++ addArtifact(Artifact("asm"), asmJarKey).settings
    def asmAttributed = Def task newCpElem(asmJarTask.value, Artifact("asm"), asm, ScalaTool)

    // Assembled settings for projects which produce an artifact.
    def codeProject(others: Setting[_]*) = compiling ++ publishing ++ others

    // Settings added to every project.
    def universal = List(
                             name ~=  (dash(PolicyName, _)),
                     organization :=  PolicyOrg,
                          version :=  "1.0.1-SNAPSHOT",
                     scalaVersion :=  ScalaKnownVersion,
               scalaBinaryVersion :=  "2.11",
                 autoScalaLibrary :=  false,
                       crossPaths :=  false,
             managedScalaInstance :=  false,
                    sourcesInBase :=  false,
                      logBuffered :=  false,
                      showSuccess :=  false,
                       traceLevel :=  25,
                ivyConfigurations +=  ScalaTool,
         unmanagedJars in Compile ++= buildLevelJars.value,
                    scalaInstance <<= scalaInstance in ThisBuild
    )

    def compiler = codeProject(
      unmanagedSourceDirectories in Compile <++= allInSrc("compiler reflect repl")
    )

    def compat   = List(sourceGenerators in Compile <+= createUnzipTask)

    def library = codeProject(
                     scalaSource in Compile <<=  inSrc(Library),
      unmanagedSourceDirectories in Compile <++= allInSrc("forkjoin library"),
                   scalacOptions in Compile ++=  Seq("-sourcepath", inSrc(Library).value.getPath),
                           previousArtifact  :=  Some(scalaLibrary),
                         binaryIssueFilters ++=  MimaPolicy.filters
    )
    def partest = codeProject(
       fork in Test :=  true,
      unmanagedBase :=  baseDirectory.value / "testlib",
               test <<= runAllTests,
           testOnly :=  runTests.evaluated
    )
    def root = publishing ++ List(
                                     name :=  PolicyName,
                                 getScala :=  scalaInstanceTask.evaluated,
                                      run :=  asInputTask(forkCompiler).evaluated,
                                     repl :=  asInputTask(forkRepl).evaluated,
                          fork in Runtime :=  true,
               initialCommands in console :=  "import scala.reflect.runtime.universe._",
        initialCommands in consoleProject :=  "import policy.building._",
                             watchSources ++= sbtFilesInBuild.value ++ sourceFilesInProject.value,
           bootstrapModuleId in ThisBuild :=  printResult("bootstrap")(chooseBootstrap),
                      libraryDependencies +=  bootstrapModuleId.value % ScalaTool.name,
               scalaInstance in ThisBuild <<= scalaInstanceFromModuleIDTask
    )
    def publishing = List(
      checksums in publishLocal :=  Nil,
           pomIncludeRepository :=  (_ => false),
              packagedArtifacts <<= packaged(Seq(packageBin in Compile)),
      publishLocalConfiguration ~=  (p => publishConfig(p.artifacts, p.ivyFile, p.checksums, p.resolverName, logging = UpdateLogging.Quiet, overwrite = false)),
            updateConfiguration ~=  (uc => new UpdateConfiguration(uc.retrieve, uc.missingOk, logging = UpdateLogging.Quiet))
    )
    def compiling = List(
             resourceGenerators in Compile <+= generateProperties(),
        javacOptions in (Compile, compile) ++= stdJavacArgs,
       scalacOptions in (Compile, compile) ++= stdScalacArgs,
           javacOptions in (Test, compile) :=  wordSeq("-nowarn"),
          scalacOptions in (Test, compile) :=  wordSeq("-Xlint"),
                                incOptions ~=  (_ withRecompileOnMacroDef false) // withAntStyle true
    )
  }
}
