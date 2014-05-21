package policy
package build

import sbt._, Keys._
import sbt.classpath._
import ClasspathUtilities._

final class PolicyClassLoader(id: String, compilerVersion: String, jars: Seq[File]) extends java.net.URLClassLoader(jars.map(_.toURI.toURL).toArray, rootLoader) with RawResources with NativeCopyLoader {
  val nativeTemp = IO.createTemporaryDirectory
  override val config = new NativeCopyConfig(nativeTemp, jars, javaLibraryPaths)
  def resources = Map("compiler.properties" -> s"version.number=$compilerVersion")
  override def toString = jars.mkString(s"Created PolicyClassLoader($id, $compilerVersion\n  ", "\n  ", "\n)")
}

// trait BootstrapEnv {
//   protected def moduleForName(name: String): ModuleId
//   def org: String
//   def rev: String
//   def libraryId: ModuleID = moduleForName("library") % "scala-tool"
//   def compilerId: ModuleID = moduleForName("compiler") % "scala-tool"
//   def libraryJar: File
//   def compilerJar: File
//   def otherJars: Seq[File]
//   def loader: ClassLoader
// }

// class ScalaBootstrapEnv(version: String) extends BootstrapEnv {
//   def moduleForName(name: String)  = ScalaOrg                   % s"scala-$name"  %  ScalaFixedVersion

//   def org = ScalaOrg
//   def rev = version
//   def loader = new PolicyClassLoader(id, version, libraryJar +: compilerJar +: otherJars)
// }

trait Bootstrap {
  private def findModuleId(name: String): ModuleID = if (isScalaBootstrap) scalaModuleId(name) else policyModuleId(name)
  def bootstrapCompilerId: ModuleID                = findModuleId("compiler")
  def bootstrapLibraryId: ModuleID                 = findModuleId("library")

  private def matches(m1: ModuleID, m2: ModuleID): Boolean     = (m1.organization == m2.organization) && (m1.name == m2.name)
  private def matches(m1: ModuleReport, m2: ModuleID): Boolean = matches(m1.module, m2)

  // private def transitiveReports = transitiveUpdate map (_ flatMap (_ configuration Bootstrap.name toList))
  // private def transitiveModules = transitiveReports map (_ flatMap (_.modules))
  // private def reports           = update map (_ configuration Bootstrap.name toList)
  private def reports           = update map (_ configuration ScalaTool.name toList)
  private def modules           = reports map (_ flatMap (_.modules))

  private def nonEmpty[A](xs: Seq[A], ys: => Seq[A]): Seq[A]   = if (xs.isEmpty) ys else xs
  private def single[A](xs: Seq[A]): Option[A]                 = xs.toList match {
    case x :: Nil => Some(x)
    case xs       => None
  }

  private def mfold[A, B](task: TaskOf[Option[A]], z: B)(f: A => B) = task map (_.fold(z)(f))
  private def mfind[A, B](task: TaskOf[_ <: Seq[A]])(p: A => Boolean) = task map (_ find p)

  private def reportFor(m: ModuleID): TaskOf[Option[ModuleReport]] = mfind(modules)(matches(_, m))
  private def revisionFor(m: ModuleID): TaskOf[String]             = mfold(reportFor(m), UnknownVersion)(_.module.revision)
  private def filesFor(m: ModuleID): TaskOf[Seq[File]]             = mfold(reportFor(m), NoFiles)(_.artifacts map (_._2))
  private def fileFor(m: ModuleID): TaskOf[Option[File]]           = filesFor(m) map single

  private def policyRevision: TaskOf[String]    = revisionFor(bootstrapCompilerId)
  private def policyBaseJars: TaskOf[Seq[File]] = filesFor(policyModuleId("compiler"))
  private def scalaBaseJars: TaskOf[Seq[File]]  = filesFor(scalaModuleId("compiler"))

  private def newLoader(id: String, compilerVersion: String, jars: Seq[File]) = printResult("new loader")(new PolicyClassLoader(id, compilerVersion, jars))

  private def bootstrapId(mod: ModuleID) = Def task {
    def us   = if (isScalaBootstrap) None else fileFor(mod).value
    def them = fileFor(mod).value
    us orElse them getOrElse sys.error(s"Not found: $mod")
  }

  private def jarSorter(f1: File, f2: File): Boolean = (f1 != f2) && (
       (f1.getName contains "-library")
    || ((f1.getName contains "-compiler") && !(f2.getName contains "-library"))
  )

  private def makeInstance(version: String, config: String) = Def.task[Option[ScalaInstance]] {
    update.value configuration config flatMap { r =>
      r.modules.toList flatMap (_.artifacts map (_._2)) sortWith jarSorter match {
        case lib :: comp :: others => Some(ScalaInstance(version, lib, comp, others: _*)(state.value.classLoaderCache.apply))
        case _                     => None
      }
    }
  }

  def bootstrapRevision: TaskOf[String]    = policyRevision
  def bootstrapJars: TaskOf[Seq[File]]     = Def task nonEmpty(policyBaseJars.value, scalaBaseJars.value)
  def bootstrapLoader: TaskOf[ClassLoader] = Def task newLoader(thisProject.value.id, bootstrapRevision.value, bootstrapJars.value)
  def bootstrapCompiler: TaskOf[File]      = bootstrapId(bootstrapCompilerId)
  def bootstrapLibrary: TaskOf[File]       = bootstrapId(bootstrapLibraryId)

  def scalaInstanceForVersion(version: String): TaskOf[ScalaInstance] =
    Def task ScalaInstance(version, appConfiguration.value.provider.scalaProvider.launcher getScala version)

  def bootstrapInstance: TaskOf[ScalaInstance] = Def task {
    def fallback() = scalaInstanceForVersion(ScalaFixedVersion).value
    if (isScalaBootstrap) fallback()
    else makeInstance(PolicyBootstrapVersion, "scala-tool").value match {
      case Some(r) => r
      case _       => fallback()
    }
  }

  def bootstrapSummary: TaskOf[String] = Def task s"""
    |    revision  ${ bootstrapRevision.value }
    |    library   ${ bootstrapLibrary.value }
    |    compiler  ${ bootstrapCompiler.value }
  """
}
