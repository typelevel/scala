import policy.building._

// See project/BuildSettings for all the details - here we attempt to
// retain a high level view.
buildLevelSettings

bootstrapModuleId in ThisBuild := printResult("bootstrap")(chooseBootstrap)

//lazy val asm = RootProject(file("/mirror/forks/asm"))
//   ProjectRef(file("/mirror/forks/asm"), "asm")
//RootProject(uri("https://github.com/paulp/asm/tree/scala-fixes"))

lazy val root = (
  project.setup in file(".")
    dependsOn ( library, compiler, partest )
    aggregate ( library, compiler, partest, compat )
    settings  (
      commands += Command.command("publishBootstrapCompiler")(s => publishBootstrapScript ::: s),
      commands += Command.args("renameProjects", "<from> <to>") { case (state, Seq(from, to)) => transformEveryKey(name)(_.replaceAllLiterally(from, to))(state) },
      commands += Command.args("removeScalacOption", "<option>")((state, opt) => transformInEveryScope(scalacOptions, state, (xs: Seq[String]) => xs filterNot (_ == opt))),
      commands += Command.args("appendScalacOption", "<option>")(appendInEveryScope(scalacOptions, _, _))
    )
).noArtifacts

lazy val library = project.setup addMima scalaLibrary

lazy val compiler = project.setup dependsOn (library) deps ( jline, ant, "org.improving" % "asm" % "5.0.3-SNAPSHOT" )

lazy val partest = project.setup dependsOn compiler deps ( jline, ant, diffutils) intransitiveDeps ( scalacheck, testInterface, scalaParsers, scalaXml )

lazy val compat = project.setup dependsOn compiler sbtDeps ( "interface", "compiler-interface" ) noArtifacts
