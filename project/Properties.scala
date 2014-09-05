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

  private def log[A](msg: String)(body: A): A = printResult(msg)(body)

  override def setProperty(key: String, value: String)   = log(s"setProperty($key, $value)")(super.setProperty(key, value))
  override def put(key: Object, value: Object)           = log(s"put($key, $value)")(super.put(key, value))
  override def putAll(t: jMap[_ <: Object, _ <: Object]) = log(s"putAll($t)")(super.putAll(t))
  override def remove(key: Object)                       = log(s"remove($key)")(super.remove(key))
  override def clear(): Unit                             = log(s"clear()")(super.clear())

  def toSeq: Seq[(String, String)]                                  = in.toSeq
  def joined(sep: String): Seq[String]                              = for ((k, v) <- toSeq) yield k + sep + v
  def filterKeys(p: String => Boolean): ImmutableProperties         = filter(kv => p(kv._1))
  def filter(p: ((String, String)) => Boolean): ImmutableProperties = ImmutableProperties(toSeq filter p: _*)

  def commandLineArgs: Seq[String] = joined("=") map ("-D" + _)
}

final class MutableProperties private (val file: File) {
  import scala.collection.JavaConverters._
  private[this] val props = new java.util.Properties
  private def objectToString(s: Object): String = s match {
    case null      => null
    case s: String => s
    case x         => "" + x
  }

  private def updateAndReturn(name: String, value: String): String  = try value finally set(name, value)
  private def set(key: String, value: String): String = objectToString(props.setProperty(key, value))

  def load(): this.type = { IO.load(props, file) ; this }
  def save(): this.type = { IO.write(props, null, file) ; this }

  def write(key: String, value: String): this.type = { set(key, value) ; save() }

  def ? (key: String): Option[String]                    = get(key)
  def apply(key: String): String                         = props getProperty key
  def update(key: String, value: String): Option[String] = Option(set(key, value))
  def get(key: String): Option[String]                   = Option(apply(key))
  def get(key: String, alt: => String): String           = get(key) | updateAndReturn(key, alt)
  def getInt(name: String, alt: => Int): Int             = get(name).fold(alt)(_.toInt)
  def toMap: Map[String, String]                         = toSeq.toMap
  def toSeq: Seq[(String, String)]                       = keys map (k => (k, apply(k)))
  def keys: Seq[String]                                  = props.keys.asScala.toSeq map objectToString

  def filename = file.getName
  override def toString = ( for ((k, v) <- toSeq) yield s"$k -> $v" ) mkString("MutableProperties(", ", ", ")")
}

object ImmutableProperties {
  def apply(in: (String, String)*): ImmutableProperties = new ImmutableProperties(in.toMap)
}

object MutableProperties {
  def apply(file: File): MutableProperties = new MutableProperties(file) load()
}
