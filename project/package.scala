package policy

import sbt._, Keys._, building._

sealed abstract class PolicyPackage extends Constants with PluginRelatedCode with BuildTasks with Helpers with Depends with Runners with Bootstrap

// Encapsulation of state transformations.
final class WState(val state: State) {
  implicit def implicitState: State = state
  val extracted = Project extract state

  def info(msg: => String) = state.log.info(msg)

  def apply[A](key: SettingKey[A]): A = extracted get key
  def apply[A](key: TaskKey[A]): A    = get[A](key) | sys.error(s"$key failed")

  def get[A](key: TaskKey[A]): Option[A] = Project.evaluateTask(key, state) match {
    case None           => None
    case Some(Inc(inc)) => Incomplete.show(inc.tpe) ; None
    case Some(Value(v)) => Some(v)
  }
  def map(f: State => State): WState     = new WState(f(state))
  def set(settings: SettingSeq): WState  = new WState(extracted.append(settings, state))
  def run(commands: Seq[String]): WState = new WState(commands ::: state)
}

object WState {
  def apply(f: WState => WState): StateMap = s => f(new WState(s)).state
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
  implicit def inputTaskValueDiscarding[A](in: InputTaskOf[A]): InputTaskOf[Unit] = in map (_ => ())
  implicit def sequenceOps[A](xs: Seq[A]): SequenceOps[A]                         = new SequenceOps[A](xs)

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
