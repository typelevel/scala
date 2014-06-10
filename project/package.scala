package policy

import sbt._, Keys._, building._
import bintray.Plugin.{ bintraySettings, bintrayPublishSettings }

sealed abstract class PolicyPackage extends Constants with PluginRelatedCode with BuildTasks with Helpers with Depends with Runners

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
    state.log.info(s"Updating $BootstrapModuleProperty to $newModule in build.properties")
    buildProps.write(BootstrapModuleProperty, newModule)
    sys.props(BootstrapModuleProperty) = newModule
    state
  }

  def lookupBootstrapId(state: State): ModuleID = (Project extract state) get (bootstrapModuleId in ThisBuild)

  def bootstrap(state: State, args: Seq[String]): State = {
    val (newVersion, nextCommands) = args.toList match {
      case Nil       => dash(PolicyBaseVersion, runSlurp("bin/unique-version")) -> Nil
      case v :: Nil  => v -> Nil
      case v :: cmds => v -> cmds
    }
    def next = List("publishLocal", s"saveBootstrapVersion $newVersion") ++ nextCommands ++ List("reboot full")
    bootstrap(state, newVersion, next)
  }

  def bootstrap(state: State, newVersion: String, nextCommands: Seq[String]): State = {
    val extracted        = Project extract state
    val currentBootstrap = lookupBootstrapId(state)

    if (newVersion == currentBootstrap.revision)
      state.log.info(s"Bootstrap compiler is already $newVersion, skipping to commands.")
    else
      state.log.info(s"Building $newVersion to replace bootstrap compiler $currentBootstrap.")

    def perProject(p: String) = bintray.Plugin.bintrayPublishSettings ++ Seq(
         name in LocalProject(p) := s"bootstrap-$p",
                    organization := "org.improving",
      version in LocalProject(p) := newVersion
    )

    val newSettings = wordSeq("library compiler") flatMap perProject
    nextCommands ::: extracted.append(newSettings, state)
  }

  def publishBootstrap(state: State): State = {
    // import bintray.Keys._
    // ++ bintrayPublishSettings

    val newSettings = (List("library", "compiler") flatMap (p => Seq(name in LocalProject(p) := s"bootstrap-$p")))
    state.log.info(s"publishing bootstrap compiler with temporary settings:\n  " + newSettings.mkString("\n  "))
    CommandContext(newSettings, List("publishLocal", "publish", "reboot full"))(state)
  }

  case class CommandContext(settings: Seq[Setting[_]], commands: Seq[String]) {
    def apply(state: State): State            = commands ::: (Project extract state).append(settings, state)
    def set (xs: Setting[_]*): CommandContext = copy(settings = settings ++ xs)
    def add (xs: String*): CommandContext     = copy(commands = commands ++ xs)
  }

  def runWith(xs: Setting[_]*): CommandContext = CommandContext(xs.toSeq, Nil)
}
