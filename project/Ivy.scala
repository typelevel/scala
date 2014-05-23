package policy
package building

import sbt._, Keys._

trait IvyHelpers {
  def newCache = new IvyCache(Some(Path.userHome / ".ivy2"))
  def retrieve(m: ModuleID, dest: File) = newCache.retrieveCachedJar(m, dest, None, NullLogger)
}
