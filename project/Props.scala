package policy
package build

import sbt._, Keys._
import scala.sys.SystemProperties.noTraceSupression
import scala.language.dynamics

class Props private (file: File) extends scala.Dynamic {
  private[this] val props = new java.util.Properties

  private def exists = file.canRead
  private def get(key: String): String = props getProperty key
  private def set(key: String, value: String): String = try value finally props.setProperty(key, value)
  private def getOrElseUpdate(name: String, defaultValue: => String): String = get(name) match {
    case null => set(name, defaultValue)
    case v    => v
  }
  def load(): this.type = { IO.load(props, file) ; this }
  def save(): this.type = { IO.write(props, null, file) ; this }

  def applyDynamic(name: String)(alt: => String): String = getOrElseUpdate(name, alt)
  def updateDynamic(name: String)(value: String): Unit   = set(name, value)
  def selectDynamic(name: String): String                = get(name)
  def intProp(name: String, alt: => Int): Int            = getOrElseUpdate(name, alt.toString).toInt
}

object Props {
  def buildProps = apply(file("project/build.properties"))
  def apply(file: File): Props = new Props(file) load()

  def generateProperties() = Def task {
    val id = name.value stripPrefix "policy-" stripSuffix "-stable"
    val file = (resourceManaged in Compile).value / s"$id.properties"
    val props = Props(file)
    props.`version.number`              = version.value
    props.`scala.version.number`        = scalaVersion.value
    props.`scala.binary.version.number` = scalaBinaryVersion.value
    props.save()
    Seq(file)
  }

  def unsuppressTraces = "-D" + noTraceSupression.key

  def testingProperties = Def task newProps(
    "partest.scalac_opts"      -> join((scalacOptions in Test).value),
    "partest.java_opts"        -> join((javaOptions in (Test, compile)).value),
    "partest.colors"           -> "256",
    "partest.threads"          -> (numCores / 2).toString,
    "partest.git_diff_options" -> "--word-diff",
    "partest.basedir"          -> buildBase.value.getPath,
    "partest.root"             -> testBase.value.getPath,
    "partest.testlib"          -> fromBuild(_ / "partest" / "testlib").value.getPath
  )


  // def srcSpecLib     = findJar("instrumented", Directory(srcDir / "speclib"))
  // def srcCodeLib     = findJar("code",  Directory(srcDir / "codelib"), Directory(testRoot / "files" / "codelib") /* work with --srcpath pending */)
  // def agentLib       = findJar("scala-partest-javaagent", buildPackLibDir)
  // def scalaCheck     = findJar("scalacheck", buildPackLibDir, srcLibDir)
  // def testInterface  = findJar("test-interface", buildPackLibDir, srcLibDir)
  // def diffUtils      = findJar("diffutils", buildPackLibDir)
}
