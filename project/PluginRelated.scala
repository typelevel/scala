package policy
package building

/** I know what a PITA it is when a project has a bunch of sbt plugins
 *  and you want to do anything the least bit interesting with it, or a
 *  little time goes by. The best I can do right now is attempt to
 *  isolate the plugin code enough to make it easy to expunge.
 */
trait PluginRelatedCode {
  def mimaDefaultSettings = com.typesafe.tools.mima.plugin.MimaPlugin.mimaDefaultSettings
  def previousArtifact    = com.typesafe.tools.mima.plugin.MimaKeys.previousArtifact
  def binaryIssueFilters  = com.typesafe.tools.mima.plugin.MimaKeys.binaryIssueFilters
}
