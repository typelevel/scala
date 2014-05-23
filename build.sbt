import policy._, build._

// There is an unmanaged asm-debug-all jar - should be able to use the standard
// asm-all artifact if/when the fixes against 5.0.2 enter the mainline.
buildLevelSettings

lazy val root = (
  project.setup.noArtifacts in file(".")
    dependsOn ( library, compiler, partest )
    aggregate ( library, compiler, partest, compat )
)

lazy val library = project.setup.addToolJars addMima scalaLibraryId

lazy val compiler = project.setup.addToolJars dependsOn library intransitiveDeps ( jline, ant )

lazy val partest = project.setup dependsOn compiler intransitiveDeps ( jline, ant, scalacheck, diffutils, testInterface )

lazy val compat = project.setup.noArtifacts dependsOn compiler sbtDeps ( "interface", "compiler-interface" )
