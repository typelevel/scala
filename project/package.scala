package policy

import sbt._, Keys._, building._

sealed abstract class PolicyPackage extends Constants with PluginRelatedCode with BuildTasks with Helpers with Depends with Runners with Bootstrap

package object building extends PolicyPackage {
  locally {
    import org.slf4j.{ LoggerFactory, Logger }, ch.qos.logback.classic
    LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME).asInstanceOf[classic.Logger] setLevel classic.Level.INFO
  }

  def chain[A](g: A => A)(f: A => A): A => A = f andThen g

  def doto[A](x: A)(f: A => Unit): A = { f(x) ; x }

  // All interesting implicits should be in one place.
  implicit def symToLocalProject(sym: scala.Symbol): LocalProject                 = LocalProject(sym.name)
  implicit def projectToProjectOps(p: Project): ProjectOps                        = new ProjectOps(p)
  implicit def optionToOptionOps[A](opt: Option[A]): ScalaOptionOps[A]            = new ScalaOptionOps(opt)
  implicit def inputTaskValueDiscarding[A](in: InputTaskOf[A]): InputTaskOf[Unit] = in map (_ => ())
  implicit def sequenceOps[A](xs: Seq[A]): SequenceOps[A]                         = new SequenceOps[A](xs)

  implicit class InitSettingOps[A](val key: SettingOf[A]) {
    def |> [B](f: A => B): SettingOf[B] = Def setting f(key.value)
    def task: TaskOf[A]                 = Def task key.value
  }

  implicit class SettingKeyOps[A](val key: SettingKey[A])(implicit state: State) {
    def is: A = Project extract state get key
  }
  implicit class TaskKeyOps[A](val key: TaskKey[A])(implicit state: State) {
    def | (alt: => A): A = is getOrElse alt
    def is: Option[A] = Project.evaluateTask(key, state) match {
      case None           => None
      case Some(Inc(inc)) => Incomplete.show(inc.tpe) ; None
      case Some(Value(v)) => Some(v)
    }
  }
}
