import policy.building._

// See project/BuildSettings for all the details - here we retain a high level view.
lazy val root = (
  (project in file(".")).setup.noArtifacts
    dependsOn ( library, compilerProject )
    aggregate ( library, compilerProject )
)

lazy val library = project.setup addMima scalaLibrary deps ( slf4jApi, logback )

lazy val compilerProject = (
  Project(id = "compiler", base = file("compiler")).setup
    dependsOn library
    intransitiveDeps ( ant, jline )
    intransitiveTestDeps ( diffutils, scalacheck, testInterface )
)

lazy val compat = project.setup.noArtifacts dependsOn compilerProject sbtDeps ( "interface", "compiler-interface" )

bintraySettings
