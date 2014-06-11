package policy

import sbt._, Keys._, building._

sealed abstract class PolicyPackage extends Constants with PluginRelatedCode with BuildTasks with Helpers with Depends with Runners with Bootstrap

// Encapsulation of state transformations.
final class WState(val state: State) {
  implicit def implicitState: State = state
  val extracted = Project extract state
  import extracted._

  def settings     = structure.settings
  def relation     = Project.relation(structure, true)
  def scopedKeys   = relation._1s.toSeq
  def attrKeys     = scopedKeys map (_.key)
  def projectScope = Load projectScope currentRef

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
  def look(f: WState => Unit): StateMap = { s => f(new WState(s)) ; s }
  def apply(f: WState => WState): StateMap = s => f(new WState(s)).state
}

package object building extends PolicyPackage {
  locally {
    import org.slf4j.{ LoggerFactory, Logger }, ch.qos.logback.classic
    LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME).asInstanceOf[classic.Logger] setLevel classic.Level.INFO
  }

  class ScopedShow[A](ws: WState, set: Setting[A]) {
    val ThisBuildUri = ws(baseDirectory in ThisBuild).toURI
    val skey = set.key
    val key   = skey.scopedKey
    def attrKey = key.key
    val scope = key.scope
    import scope._
    def label   = attrKey.label
    def axes    = List(project, config, task, extra)
    def globals = axes filter (_ == Global) size
    def thises  = axes filter (_ == This) size

    def hasPrintableValue = (
         !(label startsWith "pgp")
      && !(label == "taskTemporaryDirectory")
      && (getValue exists (x => any_s(x) != "-"))
    )
    def getValue: Option[A] = ws(Keys.settingsData).get(scope, attrKey)
    def value: A = getValue getOrElse sys.error("Failed")
    def value_s: String = getValue.fold("<error>")(any_s)

    private def any_s(x: Any): String = if (isUnprintable(x)) "-" else x match {
      case Int.MaxValue => "Int.MaxValue"
      case f: File      => f.getPath
      case s: String    => "\"" + s + "\""
      case c: Char      => "'" + c + "'"
      case xs: Array[_] => xs.map(any_s).mkString("Array(", ", ", ")")
      case Some(x)      => any_s(x)
      case xs: Seq[_]   => xs map any_s mkString ("Seq(", ", ", ")")
      case x: Product   => x.productIterator map any_s mkString (x.productPrefix + "(", ", ", ")")
      case x            => if (useToString(x)) "" + x else "<%s>".format(x.getClass.getName) // "<??? " + x.getClass.getName + ">: " + x
    }
    private def useToString(x: Any): Boolean = x match {
      case _: CrossVersion               => true
      case _: xsbti.compile.CompileOrder => true
      case _: Boolean                    => true
      case _: Int                        => true
      case _: Artifact                   => true
      case _: Enumeration#Value          => true
      case _: ModuleID                   => true
      case _: Resolver                   => true
      case _: URL | _: URI               => true
      case _                             => false
    }
    private def isUnprintable(x: Any): Boolean = x match {
      case ""                       => true
      case _: None.type             => true
      case _: Unit                  => true
      case _: Task[_]               => true
      case _: InputTask[_]          => true
      case _: Function0[_]          => true
      case _: Function1[_, _]       => true
      case _: Function2[_, _, _]    => true
      case _: Function3[_, _, _, _] => true
      case _: java.io.FileFilter    => true
      case x: Traversable[_]        => x.isEmpty
      case _                        => ("" + x) contains "$anon"
    }
    private def axis_s[A](s: ScopeAxis[A]): String = s match {
      case Global                          => "*"
      case This                            => "."
      case Select(BuildRef(ThisBuildUri))  => "{.}"
      case Select(ThisBuild)               => "{.}"
      case Select(ProjectRef(build, name)) => s"$name"
      case Select(BuildRef(build))         => "" + build
      case Select(ConfigKey(k))            => "" + k
      case Select(x)                       => "" + x
      case x                               => "??? " + x
    }

    def project_s = axis_s(project)
    def config_s  = axis_s(config)
    def task_s    = axis_s(task)
    def extra_s   = axis_s(extra)

    def axes_s = "%-12s %-12s %-15s".format(project_s, config_s, task_s)
    def score = (label, scope.toString)

    override def toString = "%-28s %s %s".format(label, axes_s, value_s)
  }

  def onLoadHook: StateMap = WState look { ws =>
    val shows = ws.settings map (k => new ScopedShow(ws, k)) filter (_.hasPrintableValue) sortBy (_.score)
    IO.write(file("settings.dump"), shows.map(_.toString).distinct.mkString("\n") + "\n")
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
