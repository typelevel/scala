package policy

import sbt._, Keys._
import Configurations.ScalaTool

package object build extends policy.build.Constants with policy.build.Bootstrap {
  def strings(xs: Any*): List[String] = xs.toList map ("" + _)
  def join(xs: Seq[String]): String   = xs filterNot (_ == "") mkString " "
  def wordSet(s: String): Set[String] = (s.trim split "\\s+").toSet
  def wordSeq(s: String): Seq[String] = (s.trim split "\\s+").toSeq.sorted

  def printResult[A](msg: String)(res: A): A = try res finally println(s"$msg: $res")

  def localMaven  = Resolver.file("Local Maven", file(Path.userHome.absolutePath+"/.m2/repository"))
  def logger      = Def task (streams in Compile).value.log
  def javaRuntime = java.lang.Runtime.getRuntime
  def numCores    = javaRuntime.availableProcessors

  def jline                       = "jline" % "jline" % "2.11"
  def buildBase                   = Def setting (baseDirectory in LocalProject("root")).value
  def scalaModuleId(name: String) = Def setting ("org.scala-lang" % s"scala-$name" % scalaVersion.value)
  def sbtModuleId(names: String*) = Def setting (names.toList map (name => ("org.scala-sbt" % name % sbtVersion.value % "test").intransitive()))

  def globally(xs: Setting[_]*)     = inScope(Global)(xs.toList)
  def intransitively(xs: ModuleID*) = xs.toList map (_.intransitive())

  implicit def mkAttributedFile(f: File): Attributed[File]             = Attributed blank f
  implicit def mkAttributedFiles(fs: Seq[File]): Seq[Attributed[File]] = Attributed blankSeq fs
  implicit def resolvedProjectToRef(p: ResolvedProject): ProjectRef    = ProjectRef(p.base, p.id)
  implicit def predicateToFileFilter(p: File => Boolean): FileFilter   = new FileFilter { def accept(f: File): Boolean = p(f) }

  def subProjectSettings: List[Setting[_]] = List(
                     name ~=  (n => s"policy-$n"),
                resolvers +=  Classpaths.typesafeResolver,
             watchSources ++= (buildBase.value * "*.sbt").get ++ (buildBase.value / "project" * "*.scala").get,
               traceLevel :=  50,
         autoScalaLibrary :=  false,
            sourcesInBase :=  false,
              logBuffered :=  false,
     managedScalaInstance :=  false,
              showSuccess :=  false,
               exportJars :=  true,
     pomIncludeRepository :=  (_ => false),
                publishTo :=  Some(localMaven),
        ivyConfigurations +=  ScalaTool,
      libraryDependencies +=  bootstrapCompiler.value,
            scalaInstance <<= bootstrapInstance
  )
  def scopedSubProjectSettings = subProjectSettings ++ List(
                      javacOptions in Compile ++= javacArgs,
                     scalacOptions in Compile ++= scalacArgs,
                resourceGenerators in Compile <+= Props.generateProperties(),
                      publishArtifact in Test :=  false,
        publishArtifact in (Test, packageBin) :=  false,
     publishArtifact in (Compile, packageDoc) :=  false,
     publishArtifact in (Compile, packageSrc) :=  false
  )

  implicit class ProjectOps(val p: Project) {
    private def legacy(id: String): List[Setting[_]] = List(
      scalaSource in Compile := buildBase.value / "src" / id,
       javaSource in Compile <<= scalaSource in Compile
    )

    def mima                                     = p settings (mimaDefaultSettings: _*)
    def sub                                      = p settings (scopedSubProjectSettings: _*)
    def deps(ms: ModuleID*): Project             = p settings (libraryDependencies ++= ms)
    def intransitiveDeps(ms: ModuleID*): Project = p settings (libraryDependencies ++= intransitively(ms: _*))
  }
}
