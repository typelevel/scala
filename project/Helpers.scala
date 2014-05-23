package policy
package building

import sbt._, Keys._

trait Helpers extends IndependentHelpers with SbtHelpers {
  object NullLogger extends AbstractLogger {
    def getLevel: Level.Value                                  = Level.Error
    def setLevel(newLevel: Level.Value)                        = ()
    def getTrace                                               = 0
    def setTrace(flag: Int)                                    = ()
    def successEnabled                                         = false
    def setSuccessEnabled(flag: Boolean)                       = ()
    def control(event: ControlEvent.Value, message: => String) = ()
    def logAll(events: Seq[LogEvent])                          = ()
    def trace(t: => Throwable)                                 = ()
    def success(message: => String)                            = ()
    def log(level: Level.Value, message: => String)            = ()
  }
}

/** Helper functions independent of sbt.
 */
trait IndependentHelpers {
  // Predicates
  def hasExtension(name: String)(exts: String*) = exts exists (name endsWith "." + _)
  def isJarName(name: String)                   = hasExtension(name)("jar", "zip")
  def isSourceName(name: String)                = hasExtension(name)("java", "scala")
  def isSource(file: jFile)                     = isSourceName(file.getName)
  def isJar(file: jFile)                        = isJarName(file.getName)

  // Strings
  def dash(elems: Any*): String       = elems mkString "-"
  def strings(xs: Any*): List[String] = xs.toList map ("" + _)
  def join(xs: Seq[String]): String   = xs filterNot (_ == "") mkString " "
  def wordSet(s: String): Set[String] = (s.trim split "\\s+").toSet
  def wordSeq(s: String): Seq[String] = s.trim match {
    case "" => Nil
    case s  => (s split "\\s+").toSeq.sorted
  }

  // System
  def javaRuntime = java.lang.Runtime.getRuntime
  def numCores    = javaRuntime.availableProcessors

  // Debug
  def printResult[A](msg: String)(res: A): A = try res finally println(s"$msg: $res")
}

/** Helper functions bound up in sbt.
 */
trait SbtHelpers {
  import sbt._, Keys._
  import sbt.complete._
  import DefaultParsers._

  def ScalaTool = sbt.Configurations.ScalaTool

  // Files
  def filesIn(dir: File, extension: String): Seq[File]   = dir * s"*.$extension" get
  def filesIn(dir: File, extensions: String*): Seq[File] = extensions flatMap (ext => filesIn(dir, ext))
  def sbtFilesIn(dir: File): Seq[File]                   = filesIn(dir, "sbt")
  def sourceFilesIn(dir: File): Seq[File]                = filesIn(dir, "scala", "java")

  // Settings
  def sbtFilesInBuild      = buildBase map sbtFilesIn
  def sourceFilesInProject = buildBase map (_ / "project") map sourceFilesIn
  def sourcePathOpts       = scalaSource in Compile map (p => Seq("-sourcepath", p.getPath))
  def logger               = streams in Compile map (_.log)
  def buildBase            = baseDirectory in ThisBuild
  def projectBase          = baseDirectory in ThisProject
  def testBase             = Def setting (buildBase.value / "test")
  def srcBase              = Def setting (buildBase.value / "src")

  def allInSrc(words: String)      = Def setting (wordSeq(words) map (buildBase.value / "src" / _))
  def inSrc(name: String)          = Def setting (buildBase.value / "src" / name)
  def fromSrc(f: File => File)     = Def setting f(buildBase.value / "src")
  def fromBuild(f: File => File)   = Def setting f(buildBase.value)

  // Parsers
  def scalaVersionParser: Parser[String] = token(Space) ~> token(NotSpace, "a scala version")
  def spaceDelimited(label: String = "<arg>"): Parser[Seq[String]] = DefaultParsers spaceDelimited label
  def tokenDisplay[T](t: Parser[T], display: String): Parser[T] = DefaultParsers.tokenDisplay(t, display)
  def NotSpace = DefaultParsers.NotSpace
}
