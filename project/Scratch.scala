package policy
package building

// def allTaskAndSettingKeys = Def task (BuiltinCommands allTaskAndSettingKeys state.value sortBy (_.label))

// def settingsData: TaskOf[Settings[Scope]]                            = sbt.std.FullInstance.settingsData
// def settingsDataData: Def.Initialize[Task[Map[Scope, AttributeMap]]] = settingsData map (_.data)


// def renameProjects(from: String, to: String)(state: State): State = {
//   val (from, to) = twoWords.parsed
//   transformEveryKey(name)(_.replaceAllLiterally(from, to))(state)
// }
// val jarsOf = inputTaskKey[Seq[File]]("jars of module id")

// val parser0: ParserOf[(String,String)] = Def setting {
//   (state: State) => token(StringBasic <~ Space) ~ token(StringBasic)
// }
// def parser1[A]: ParserOf[A => A] = Def setting {
//   (state: State) =>
//     ( token("scala" <~ Space) ~ token(scalaVersion.value) ) |
//     ( token("sbt" <~ Space) ~ token(sbtVersion.value) ) |
//     ( token("commands" <~ Space) ~ token(state.remainingCommands.size.toString) )
// }
// s
// val s1 = s.update(name.key) {
//   case Some(name) => name.replaceAll("policy", "bootstrap")
//   case _          => "bootstrap"
// }
// s1.reload
// "publishLocal" :: s
// refs foreach println
// /** Sets the value associated with `key` in the custom attributes map by transforming the current value.*/
// def update[T](key: AttributeKey[T])(f: Option[T] => T): State

// final case class ArtifactAndFile(artifact: Artifact, file: File)
// object ArtifactAndFile {
//   implicit def newArtifactAndFile(pair: (Artifact, File)): ArtifactAndFile = ArtifactAndFile(pair._1, pair._2)
// }
// def moduleIdJarsTask = Def inputTask NotSpace.parsed

// def moduleIdJars(m: ModuleID) = Def task {
//   val configs       = (m.configurations getOrElse "") split "," map (_.trim) filterNot (_ == "")
//   val reports       = configs flatMap (update.value configuration _)
//   val moduleReports = reports flatMap (_.modules.toList)
//   val pairs:        = List[ArtifactAndFile](moduleReports flatMap (_.artifacts): _*)

//   pairs
// }



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


//   object scopee {
//     def showProj(x: ScopeAxis[Reference]): String = x match {
//       case Select(ProjectRef(_, id)) => id
//       case Select(x)                 => s"$x"
//       case x                         => s"$x"
//     }
//     def showConf(x: ScopeAxis[ConfigKey]): String = x match {
//       case Select(ConfigKey(x)) => s"$x"
//       case Select(x)            => s"$x"
//       case x                    => s"$x"
//     }
//     def showTask(x: ScopeAxis[AttributeKey[_]]): String = x match {
//       case Select(x) => s"$x"
//       case x         => s"$x"
//     }
//     def showScope(scope: Scope): String = scope match {
//       case Scope(p, c, t, x) => "%-15s  %-15s  %-15s".format(showProj(p), showConf(c), showTask(t))
//     }

//     def groupedKeys[A](f: ScopedKey[_] => A) = Def setting (buildStructure.value.settings map (_.key) groupBy f)

// //    byScope <<= groupedKeys(_.scope) map { x => (x mapValues (_.length) map (_.swap)).toList sortBy (_._2.project.toString) foreach { case (k, v) => println("%-4s  %s".format(k, showScope(v))) } ; x },

//     def thisProjectKeys = Def setting {

//       val Name = thisProject.value.id
//       val g    = groupedKeys(_.scope.project).value
//       val vs   = g collect { case (Select(ProjectRef(_, Name)), vs) => vs map (_.key.label) }

//       vs.flatten.toSeq
//     }



//       // groupedKeys(_.scope.project) map (_ collect { case (Select(ProjectRef(_, Name)), vs) => vs }) map (_.flatten.toSeq)
//   //   Def task (state.value.attributes(sessionSettings).original map (_.key) groupBy f)

//   }

//   val keysIn    = settingKey[Seq[String]]("keys scoped to current project")
//   val byProject = taskKey[Map[Reference, Seq[ScopedKey[_]]]]("keys by project")
//   val byConfig  = taskKey[Map[ConfigKey, Seq[ScopedKey[_]]]]("keys by project")
//   val byTask    = taskKey[Map[AttributeKey[_], Seq[ScopedKey[_]]]]("keys by project")
//   val byScope   = taskKey[Map[Scope, Seq[ScopedKey[_]]]]("keys by project")

//   // val keys          = Def task (state.value.attributes(sessionSettings).original map (_.key))
//   // val keysByProject = keys map (_ groupBy (_.scope.project))
//   // val keysByConfig  = keys map (_ groupBy (_.scope.config))
//   // val keysByTask    = keys map (_ groupBy (_.scope.task))
