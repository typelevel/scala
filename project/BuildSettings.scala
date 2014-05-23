package policy
package build

import sbt._, Keys._
import Opts.resolver._
import Classpaths.{ packaged, publishConfig }
// import Configurations.ScalaTool
// import scala.sys.process.Process

trait PluginRelatedCode {
  def mimaDefaultSettings = com.typesafe.tools.mima.plugin.MimaPlugin.mimaDefaultSettings
}

trait BuildSettings extends PluginRelatedCode {
  private val Root     = "root"
  private val Compiler = "compiler"
  private val Library  = "library"
  private val Partest  = "partest"
  private val Compat   = "compat"

  // Settings to be included outside of any project.
  def buildLevelSettings = List(
    initialCommands in console  :=  "import policy.build._",
                  watchSources <++= sbtFilesInBuild,
                  watchSources <++= sourceFilesInProject
  )

  private object sets {
    def codeProject(others: Setting[_]*) = code ++ scoped ++ others

    // Settings added to every project.
    def universal = globally(
             organization := PolicyOrg,
             scalaVersion := ScalaFixedVersion,
       scalaBinaryVersion := ScalaFixedBinaryVersion,
         sbtBinaryVersion := SbtFixedBinaryVersion,
         bootstrapVersion := PolicyDynamicVersion,
                  version := PolicyBuildVersion,
         autoScalaLibrary := false,
               crossPaths := false,
               incOptions ~= (_ withRecompileOnMacroDef false) // withAntStyle true
    )
    def compiler = codeProject(
      unmanagedSourceDirectories in Compile <++= allInSrc("compiler reflect repl"),
      libraryDependencies += bootstrapLibraryId
    )
    def library = codeProject(
      unmanagedSourceDirectories in Compile <++= allInSrc("forkjoin library"),
                        libraryDependencies  +=  bootstrapCompilerId,
                   scalacOptions in Compile ++=  Seq("-sourcepath", inSrc(Library).value.getPath),
                           previousArtifact  :=  Some(scalaLibraryId),
                         binaryIssueFilters ++=  MimaPolicy.filters
    )
    def partest = codeProject(
       fork in Test := true,
      unmanagedBase := baseDirectory.value / "testlib",
               test := build.Partest.runAllTests.value,
           testOnly := build.Partest.runTests.evaluated
    )
    def compat = List(
                             name :=  dash(PolicyName, Compat),
      sourceGenerators in Compile <+= build.Partest.createUnzipTask
    )
    def root = List(
                  name := PolicyName,
              getScala := scalaInstanceTask.evaluated,
                   run := Runners.runInput(CompilerRunnerClass)("-usejavacp").evaluated,
                  repl := Runners.runInput(ReplRunnerClass, Props.unsuppressTraces)("-usejavacp").evaluated,
       fork in Runtime := true
    )
    def code = List(
                            name ~=  (dash(PolicyName, _)),
               ivyConfigurations +=  ScalaTool,
                       resolvers +=  Classpaths.typesafeResolver,
                      traceLevel :=  50,
                   sourcesInBase :=  false,
                     logBuffered :=  false,
                     showSuccess :=  false,
            managedScalaInstance :=  false,
            pomIncludeRepository :=  (_ => false),
                       publishTo :=  Some(mavenLocalFile),
                   scalaInstance <<= bootstrapInstance,
               packagedArtifacts <<= packaged(Seq(packageBin in Compile)),
       publishLocalConfiguration ~=  (p => publishConfig(p.artifacts, p.ivyFile, p.checksums, p.resolverName, logging = UpdateLogging.Quiet, overwrite = false))
    )
    def scoped = List(
        javacOptions in (Compile, compile) ++= javacArgs,
       scalacOptions in (Compile, compile) ++= scalacArgs,
             resourceGenerators in Compile <+= Props.generateProperties(),
           javacOptions in (Test, compile) :=  wordSeq("-nowarn"),
          scalacOptions in (Test, compile) :=  wordSeq("-Xlint")
    )
  }

  implicit class ProjectOps(val p: Project) {
    private def projectSettings = p.id match {
      case Root     => sets.root
      case Compat   => sets.compat
      case Partest  => sets.partest
      case Compiler => sets.compiler
      case Library  => sets.library
    }

    def noArtifacts          = p settings (packagedArtifacts <<= packaged(Nil))
    def addToolJars          = p settings (unmanagedJars in Compile <++= bootstrapJars)
    def addMima(m: ModuleID) = p also mimaDefaultSettings also (previousArtifact := Some(m))

    def setup                                      = p also (sets.universal ++ projectSettings)
    def also(ss: Traversable[Setting[_]]): Project = p settings (ss.toSeq: _*)
    def deps(ms: ModuleID*)                        = p settings (libraryDependencies ++= ms.toSeq)
    def intransitiveDeps(ms: ModuleID*)            = deps(intransitively(ms: _*): _*)
    def sbtDeps(ids: String*)                      = intransitiveDeps(ids map sbtModuleId: _*)
    def scalaDeps(ids: String*)                    = intransitiveDeps(ids map scalaModuleId: _*)
  }
}
