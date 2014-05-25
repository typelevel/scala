package policy
package building

import sbt._, Keys._

final class SequenceOps[A](val xs: Seq[A]) extends AnyVal {
  def preferring(p: A => Boolean): Seq[A] = xs sortWith ((x, y) => p(x) && !p(y))
}

final case class ForkConfig(mainClass: String, props: ImmutableProperties = ImmutableProperties(), programArgs: Seq[String] = Seq(), options: ForkOptions = stdForkOptions) {
  val forker      = new Fork("java", Some(mainClass))
  val fullOptions = if (props.isEmpty) options else copyOptions(props.commandLineArgs)

  def asInputTask = Def inputTask apply(spaceDelimited("<arg>").parsed: _*)

  private def copyOptions(opts: Seq[String]): ForkOptions = options.copy(runJVMOptions = options.runJVMOptions ++ opts)

  def mapProps(f: ImmutableProperties => ImmutableProperties): ForkConfig = copy(props = f(props))
  def mapOptions(f: ForkOptions => ForkOptions): ForkConfig               = copy(options = f(options))
  def addJvmOptions(opts: String*): ForkConfig                            = copy(options = copyOptions(opts))

  private def log(msg: String) = println(msg)
  def apply(args: String*): Int = {
    log(args.mkString(this + " ", " ", ""))
    forker(fullOptions, programArgs ++ args)
  }
  override def toString = s"""
    |ForkConfig(
    |  $mainClass
    |  $props
    |  $programArgs
    |  $options
    |)""".trim.stripMargin
}

final class ScalaOptionOps[A](val opt: Option[A]) extends AnyVal {
  @inline final def | (alt: => A): A = opt getOrElse alt
}

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
