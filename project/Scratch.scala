package policy
package building


// import sbt._, Keys._
// import Configurations.ScalaTool
// import Opts.resolver._
// import scala.sys.process.Process
// import Classpaths.{ packaged, publishConfig }


// import Configurations.ScalaTool
// import Opts.resolver._
// import scala.sys.process.Process
// import Classpaths.{ packaged, publishConfig }
// scalacOptions in Compile ++= strings("-sourcepath", (scalaSource in Compile).value),
//         previousArtifact :=  Some(scalaModuleId("library")),
//       binaryIssueFilters ++= MimaPolicy.filters


// A fully-qualified reference to a setting or task looks like:
//
// {<build-uri>}<project-id>/config:inkey::key

// (updateConfiguration in Compile) :=  new UpdateConfiguration(retrieveConfiguration.value, missingOk = true, ivyLoggingLevel.value),


// private type UpElem = (String, ModuleID, Artifact, File)

// final class UpdateReportOps(val report: UpdateReport) {
//   def all                                                        = report.allModules
//   def iterator                                                   = toSeq.iterator
//   def toSeq                                                      = report.toSeq
//   def map[A](f: UpElem => A): Seq[A]                             = toSeq map f
//   def withFilter(p: UpElem => Boolean): Seq[UpElem]              = toSeq filter p
//   def filter(p: UpElem => Boolean): Seq[UpElem]                  = this withFilter p
//   def find(p: UpElem => Boolean): Option[UpElem]                 = iterator find p
//   def collect[A](pf: PartialFunction[UpElem, A]): Seq[A]         = toSeq collect pf
//   def collectFirst[A](pf: PartialFunction[UpElem, A]): Option[A] = iterator collectFirst pf
// }

// final class ConfigurationReportOps(val optReport: Option[ConfigurationReport]) {
//   def name = optReport.fold("<error>")(_.configuration)
//   def all  = optReport.fold(Seq[ModuleID]())(_.allModules)
// }

// def onUpdate[A](f: UpdateReportOps => A)                               = Def task f(new UpdateReportOps(update.value))
// def onConfig[A](config: Configuration)(f: ConfigurationReportOps => A) = Def task f(new ConfigurationReportOps(update.value configuration config.name))
// def collectUpdate[A](pf: PartialFunction[UpElem, A])                   = onUpdate(_ collect pf)
// def collectUpdateOr[A](alt: => A)(pf: PartialFunction[UpElem, A])      = onUpdate(_ collectFirst pf getOrElse alt)

// // def mapReport[A](report: UpdateReport)(f: (String, ModuleID, Artifact, File) => A) = Def.task[Seq[A]](update.value.toSeq map f)

// def sbtLoader                                                   = appConfiguration map (_.provider.loader)
// def reportFiles(report: ConfigurationReport): Seq[File]         = report.modules flatMap (_.artifacts) map (_._2)
// def reportArtifacts(report: ConfigurationReport): Seq[Artifact] = report.modules flatMap (_.artifacts) map (_._1)
// def filesInConfig(config: Configuration)                        = Def task (update.value configuration config.name).fold(Seq[File]())(reportFiles)
// def artifactsInConfig(config: Configuration)                    = Def task (update.value configuration config.name).fold(Seq[Artifact]())(reportArtifacts)

// def isPolicyLibrary  = Def setting thisProject.value.id == "library"
// def isPolicyCompiler = Def setting thisProject.value.id == "compiler"

// implicit class StateOps(s: State) {
//   def extract: Extracted = Project extract s
//   def currentProject     = extract.currentProject
// }
