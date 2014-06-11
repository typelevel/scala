import policy.building._

onLoad in Global ~=  (_ andThen ScopedShow.dump)

PolicyKeys.settingsDumpFile <<= topDir("settings.dump")

// See project/BuildSettings for all the details - here we retain a high level view.
lazy val root = (
  (project in file(".")).setup.noArtifacts
    dependsOn ( library, compilerProject ) aggregate ( library, compilerProject, compat )
)

lazy val library = project.setup addMima scalaLibrary

lazy val compilerProject = (
  Project(id = "compiler", base = file("compiler")).setup
    dependsOn library
    intransitiveTestDeps ( diffutils, testInterface )
)

lazy val repl = project.setup dependsOn compilerProject deps jline

lazy val compat = (
  project.setup.noArtifacts
    dependsOn compilerProject
    sbtDeps ( "interface", "compiler-interface" )
)

bintraySettings
