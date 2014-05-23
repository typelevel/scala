package policy
package building

import sbt._, Keys._
import Opts.resolver._
import Classpaths.{ packaged, publishConfig }

trait Depends {
  def sbtModuleId(name: String)   = SbtOrg   %         name          %  SbtFixedVersion
  def scalaModuleId(name: String) = ScalaOrg % dash(ScalaName, name) % ScalaFixedVersion

  // There is an unmanaged asm-debug-all jar - should be able to use the standard
  // asm-all artifact if/when the fixes against 5.0.2 enter the mainline.
  def ant           = "org.apache.ant"                 %            "ant"            % "1.9.4"
  // def asm           = "org.ow2.asm"                    %       "asm-debug-all"       % "5.0.2"
  def diffutils     = "com.googlecode.java-diff-utils" %         "diffutils"         % "1.3.0"
  def jline         = "jline"                          %           "jline"           %  "2.11"
  def scalaParsers  = "org.scala-lang.modules"         %% "scala-parser-combinators" % "1.0.1"
  def scalaXml      = "org.scala-lang.modules"         %%        "scala-xml"         % "1.0.1"
  def scalacheck    = "org.scalacheck"                 %%        "scalacheck"        % "1.11.3"
  def testInterface = SbtOrg                           %       "test-interface"      %  "1.0"

  def scalaLibrary      = scalaModuleId("library")
  def scalaCompiler     = scalaModuleId("compiler")
  def bootstrapCompiler = "org.improving" % "bootstrap-compiler" % "latest.release"


  def intransitively(xs: ModuleID*) = xs.toList map (_.intransitive())
}

trait BuildSettings extends PluginRelatedCode {
  private val Root     = "root"
  private val Compiler = "compiler"
  private val Library  = "library"
  private val Partest  = "partest"
  private val Compat   = "compat"

  def buildLevelJars = Def setting (buildBase.value / "lib" * "*.jar").get
  def localIvy: File = Path.userHome / ".ivy2" / "local" / "org.improving"
  def localBootstrapArtifacts: Seq[File] = localIvy / "bootstrap-compiler" ** "*.jar" get
  def chooseBootstrap = if (localBootstrapArtifacts.isEmpty) scalaCompiler else bootstrapCompiler

  def scalaInstanceFromVersion(app: xsbti.AppConfiguration, version: String): ScalaInstance =
    ScalaInstance(version, app.provider.scalaProvider.launcher getScala version)

  def scalaInstanceFromModuleIDTask: TaskOf[ScalaInstance] = Def task {
    val report = update.value configuration ScalaTool.name getOrElse sys.error("No update report")
    val modReports = report.modules.toList
    val pairs = modReports flatMap (_.artifacts)
    val files = pairs map (_._2)
    def firstRevision = modReports.head.module.revision

    printResult("scala-tool jars") {
      files match {
        case lib :: comp :: others => ScalaInstance(firstRevision, lib, comp, others ++ buildLevelJars.value: _*)(state.value.classLoaderCache.apply)
        case _                     => ScalaInstance(scalaVersion.value, appConfiguration.value.provider.scalaProvider.launcher getScala scalaVersion.value)
      }
    }
  }

  //   update.value configuration config flatMap { r =>
  //    -//       r.modules.toList flatMap (_.artifacts map (_._2)) sortWith jarSorter match {
  //   *-//         case lib :: comp :: others => Some(ScalaInstance(version, lib, comp, others: _*)(state.value.classLoaderCache.apply))
  //    -//         case _                     => None
  //    -//       }
  //    -//     }
  // }

  def asLines(s: String): List[String] = augmentString(s.trim).lines.toList map (_.trim)

  def publishBootstrapScript: Seq[String] = asLines("""
    show name
    compile
    renameProjects policy bootstrap
    show name
    publishLocal
    reboot full
  """)

  // Settings to be included outside of any project.
  def buildLevelSettings = List(
           initialCommands in console  :=  "import policy.building._",
    initialCommands in consoleProject  :=  "import policy.building._",
                         watchSources <++= sbtFilesInBuild,
                         watchSources <++= sourceFilesInProject
  )

  private object sets {
    // Assembled settings for projects which produce an artifact.
    def codeProject(others: Setting[_]*) = developing ++ compiling ++ publishing ++ others

    // Settings added to every project.
    def universal = List(
                  organization :=  PolicyOrg,
                       version :=  PolicyBuildVersion,
                  scalaVersion :=  ScalaFixedVersion,
            scalaBinaryVersion :=  ScalaFixedBinaryVersion,
              sbtBinaryVersion :=  SbtFixedBinaryVersion,
              autoScalaLibrary :=  false,
                    crossPaths :=  false,
          managedScalaInstance :=  false,
             ivyConfigurations +=  ScalaTool,
           libraryDependencies +=  (bootstrapModuleId in ThisBuild).value % "scala-tool",
      unmanagedJars in Compile ++= buildLevelJars.value,
                 scalaInstance <<= scalaInstanceFromModuleIDTask
    )

    def compiler = codeProject(
      unmanagedSourceDirectories in Compile <++= allInSrc("compiler reflect repl")
                        // libraryDependencies += bootstrapModule.value
    )
    def library = codeProject(
                     scalaSource in Compile <<=  inSrc(Library),
      unmanagedSourceDirectories in Compile <++= allInSrc("forkjoin library"),
                        // libraryDependencies  +=  bootstrapModule.value,
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
    def compat = List(
                             name :=  dash(PolicyName, Compat),
      sourceGenerators in Compile <+= createUnzipTask
    )
    def root = List(
                     name := PolicyName,
                 // getScala := scalaInstanceTask.evaluated,
                      run := Runners.runInput(CompilerRunnerClass)("-usejavacp").evaluated,
                     repl := Runners.runInput(ReplRunnerClass, unsuppressTraces)("-usejavacp").evaluated,
          fork in Runtime := true
    )
    def developing = List(
                      name ~=  (dash(PolicyName, _)),
                traceLevel :=  25,
             sourcesInBase :=  false,
               logBuffered :=  false,
               showSuccess :=  false
    )
    def publishing = List(
                       // publishTo :=  Some(mavenLocalFile),
       //      checksums in publish :=  Nil,
       // checksums in publishLocal :=  Nil,
            pomIncludeRepository :=  (_ => false),
               packagedArtifacts <<= packaged(Seq(packageBin in Compile))
       // publishLocalConfiguration ~=  (p => publishConfig(p.artifacts, p.ivyFile, p.checksums, p.resolverName, logging = UpdateLogging.Quiet, overwrite = false))
    )
    def compiling = List(
             resourceGenerators in Compile <+= generateProperties(),
        javacOptions in (Compile, compile) ++= javacArgs,
       scalacOptions in (Compile, compile) ++= scalacArgs,
           javacOptions in (Test, compile) :=  wordSeq("-nowarn"),
          scalacOptions in (Test, compile) :=  wordSeq("-Xlint"),
                                incOptions ~=  (_ withRecompileOnMacroDef false) // withAntStyle true
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

    def useSourcePath        = p settings (scalacOptions in Compile <++= sourcePathOpts)
    def noArtifacts          = p settings (publishArtifact := false, packagedArtifacts <<= packaged(Nil), unmanagedSourceDirectories := Nil)
    // def addToolJars          = p settings (unmanagedJars in Compile <++= bootstrapJars)
    def addMima(m: ModuleID) = p also mimaDefaultSettings also (previousArtifact := Some(m))

    def setup                                      = p also (sets.universal ++ projectSettings)
    def also(ss: Traversable[Setting[_]]): Project = p settings (ss.toSeq: _*)
    def deps(ms: ModuleID*)                        = p settings (libraryDependencies ++= ms.toSeq)
    def intransitiveDeps(ms: ModuleID*)            = deps(intransitively(ms: _*): _*)
    def sbtDeps(ids: String*)                      = intransitiveDeps(ids map sbtModuleId: _*)
    def scalaDeps(ids: String*)                    = intransitiveDeps(ids map scalaModuleId: _*)
  }
}
