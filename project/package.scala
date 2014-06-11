package policy

import sbt._, Keys._, building._
import bintray.Plugin.{ bintraySettings, bintrayPublishSettings }

sealed abstract class PolicyPackage extends Constants with PluginRelatedCode with BuildTasks with Helpers with Depends with Runners

// Append some settings and run commands while under their thrall.
case class CommandContext(settings: Seq[Setting[_]], commands: Seq[String]) {
  def apply(state: State): State            = commands ::: (Project extract state).append(settings, state)
  def set (xs: Setting[_]*): CommandContext = copy(settings = settings ++ xs)
  def add (xs: String*): CommandContext     = copy(commands = commands ++ xs)
}

class WState(val state: State) {
  val extracted            = Project extract state
  def info(msg: => String) = state.log.info(msg)

  def runWith(settings: Seq[Setting[_]], commands: Seq[String]): WState =
    new WState(CommandContext(settings, commands)(state))
}

object WState {
  def apply(f: WState => WState): State => State = s => f(new WState(s)).state
}

package object building extends PolicyPackage {
  locally {
    import org.slf4j.{ LoggerFactory, Logger }, ch.qos.logback.classic
    LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME).asInstanceOf[classic.Logger] setLevel classic.Level.INFO
  }

  // All interesting implicits should be in one place.
  implicit def symToLocalProject(sym: scala.Symbol): LocalProject                 = LocalProject(sym.name)
  implicit def projectToProjectOps(p: Project): ProjectOps                        = new ProjectOps(p)
  implicit def optionToOptionOps[A](opt: Option[A]): ScalaOptionOps[A]            = new ScalaOptionOps(opt)
  implicit def taskValueDiscarding[A](task: TaskOf[A]): TaskOf[Unit]              = task map (_ => ())
  implicit def inputTaskValueDiscarding[A](in: InputTaskOf[A]): InputTaskOf[Unit] = in map (_ => ()) // mapTask taskValueDiscarding)
  implicit def sequenceOps[A](xs: Seq[A]): SequenceOps[A]                         = new SequenceOps[A](xs)

  def saveBootstrapVersion(state: State, args: Seq[String]): State = {
    val newModule = (args.toList match {
      case Nil    => lookupBootstrapId(state)
      case v :: _ => PolicyOrg % "bootstrap-compiler" % v
    }).toString
    state.log.info(s"Updating $BootstrapModuleProperty to $newModule in " + localProps.file.getName)
    localProps.write(BootstrapModuleProperty, newModule)
    sys.props(BootstrapModuleProperty) = newModule
    state
  }

  def lookupBootstrapId(state: State): ModuleID = (Project extract state) get (bootstrapModuleId in ThisBuild)

  def bootstrapNames = Seq(
     name in LocalProject("library") := "bootstrap-library",
    name in LocalProject("compiler") := "bootstrap-compiler"
  )
  def bootstrapVersions(newVersion: String) = Seq(
     version in LocalProject("library") := newVersion,
    version in LocalProject("compiler") := newVersion
  )
  // Creates a fresh version number, publishes bootstrap jars with that number to the local repository.
  // Records the version in project/local.properties where it has precedence over build.properties.
  // Reboots sbt under the new jars.
  val publishLocalBootstrap = WState { ws =>
    val newVersion  = dash(PolicyBaseVersion, runSlurp("bin/unique-version"))
    val newSettings = bootstrapNames ++ bootstrapVersions(newVersion)
    ws.runWith(newSettings, List("publishLocal", s"saveBootstrapVersion $newVersion", "reboot full"))
  }
  val publishBootstrap = WState(_.runWith(bootstrapNames, List("publishLocal", "publish", "reboot full")))

  def runWith(xs: Setting[_]*): CommandContext = CommandContext(xs.toSeq, Nil)
}

