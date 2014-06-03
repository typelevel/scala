import policy.building._

// See project/BuildSettings for all the details - here we retain a high level view.
lazy val root = (
  (project in file(".")).setup.noArtifacts
    dependsOn ( library, compilerProject )
    aggregate ( library, compilerProject )
    deps ( slf4jApi, logback )
)

lazy val library = project.setup addMima scalaLibrary deps ( slf4jApi, logback )

lazy val compilerProject = (
  Project(id = "compiler", base = file("compiler")).setup
    dependsOn library
    deps ( slf4jApi, logback )
    intransitiveDeps ( ant, jline )
    intransitiveTestDeps ( diffutils, scalacheck, testInterface, scalaParsers, scalaXml )
)

lazy val compat = project.setup.noArtifacts dependsOn compilerProject sbtDeps ( "interface", "compiler-interface" )

bintraySettings
