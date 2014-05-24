package policy

import sbt._, Keys._, building._

sealed abstract class PolicyPackage extends Constants with PluginRelatedCode with BuildTasks with Helpers with Depends with Runners

package object building extends PolicyPackage {
  // All interesting implicits should be in one place.
  implicit def symToLocalProject(sym: scala.Symbol): LocalProject      = LocalProject(sym.name)
  implicit def projectToProjectOps(p: Project): ProjectOps             = new ProjectOps(p)
  implicit def optionToOptionOps[A](opt: Option[A]): ScalaOptionOps[A] = new ScalaOptionOps(opt)
}
