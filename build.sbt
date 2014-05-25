import policy.building._

// See project/BuildSettings for all the details - here we retain a high level view.
lazy val root = (
  (project in file(".")).setup.noArtifacts
    dependsOn ( library, compilerProject, partest )
    aggregate ( library, compilerProject, partest, compat )
    settings  ( commands += Command.command("bootstrap")(bootstrap) )
    settings  ( commands += Command.args("saveBootstrapVersion", "<version>")(saveBootstrapVersion) )
)

lazy val library = project.setup addMima scalaLibrary

lazy val compilerProject = Project(id = "compiler", base = file("compiler")).setup dependsOn library intransitiveDeps ( ant, jline )

lazy val partest = project.setup dependsOn compilerProject deps ( jline, ant, diffutils) intransitiveDeps ( scalacheck, testInterface, scalaParsers, scalaXml )

lazy val compat = project.setup.noArtifacts dependsOn compilerProject sbtDeps ( "interface", "compiler-interface" )
