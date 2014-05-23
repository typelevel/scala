package policy
package building

import sbt._
import scala.language.dynamics

/** It's a sad sort of immutable, but we can throw an exception if
 *  someone calls setProperty or other mutators outside of the constructor.
 */
final class ImmutableProperties private (in: Map[String, String]) extends java.util.Properties {
  // Add everything in the map to these properties.
  for ((k, v) <- in) super.put(k, v)

  private def fail() = throw new RuntimeException("Cannot mutate immutable properties")

  override def setProperty(key: String, value: String)   = printResult(s"setProperty($key, $value)")(super.setProperty(key, value))
  override def put(key: Object, value: Object)           = printResult(s"put($key, $value)")(super.put(key, value))
  override def putAll(t: jMap[_ <: Object, _ <: Object]) = printResult(s"putAll($t)")(super.putAll(t))
  override def remove(key: Object)                       = printResult(s"remove($key)")(super.remove(key))
  override def clear(): Unit                             = printResult(s"clear()")(super.clear())

  def toSeq: Seq[(String, String)]                                  = in.toSeq
  def joined(sep: String): Seq[String]                              = for ((k, v) <- toSeq) yield s"$k$sep$v"
  def filterKeys(p: String => Boolean): ImmutableProperties         = filter(kv => p(kv._1))
  def filter(p: ((String, String)) => Boolean): ImmutableProperties = ImmutableProperties(toSeq filter p: _*)

  def commandLineArgs: Seq[String] = joined("=") map ("-D" + _)
}

final class DynamicProperties private (file: File) extends scala.Dynamic {
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

object ImmutableProperties {
  def apply(in: (String, String)*): ImmutableProperties = new ImmutableProperties(in.toMap)
}

object DynamicProperties {
  def apply(file: File): DynamicProperties = new DynamicProperties(file) load()
}
