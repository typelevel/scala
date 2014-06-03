package policy

import sbt._, Keys._, building._

sealed abstract class PolicyPackage extends Constants with PluginRelatedCode with BuildTasks with Helpers with Depends with Runners

package object building extends PolicyPackage {
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

  def bootstrap(state: State): State = {
    val extracted        = Project extract state
    val newVersion       = dash(PolicyBaseVersion, runSlurp("bin/unique-version"))
    val currentBootstrap = lookupBootstrapId(state)

    if (newVersion == currentBootstrap.revision) {
      state.log.info(s"Bootstrap compiler is already $newVersion - aborting")
      return state
    }
    state.log.info(s"Building $newVersion to replace bootstrap compiler $currentBootstrap.")

    def perProject(p: String) = Seq(name in LocalProject(p) := s"bootstrap-$p", version in LocalProject(p) := newVersion)
    val newSettings = wordSeq("library compiler") flatMap perProject
    val newCommands = List("publishLocal", s"saveBootstrapVersion $newVersion", "reboot full")
    newCommands ::: extracted.append(newSettings, state)
  }
}
