import policy.building._

// See project/BuildSettings for all the details - here we retain a high level view.
lazy val root = (
  (project in file(".")).setup.noArtifacts
    dependsOn ( library, compiler, partest )
    aggregate ( library, compiler, partest, compat )
)

lazy val library = project.setup addMima scalaLibrary

lazy val compiler = project.setup dependsOn library intransitiveDeps ( ant, jline )

lazy val partest = project.setup dependsOn compiler deps ( jline, ant, diffutils) intransitiveDeps ( scalacheck, testInterface, scalaParsers, scalaXml )

lazy val compat = project.setup.noArtifacts dependsOn compiler sbtDeps ( "interface", "compiler-interface" )
