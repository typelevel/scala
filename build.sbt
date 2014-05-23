import policy._, build._

// There is an unmanaged asm-debug-all jar - should be able to use the standard
// asm-all artifact if/when the fixes against 5.0.2 enter the mainline.
seq(policyThisBuildSettings ++ policyGlobalSettings: _*)

lazy val root = (
  project.configured in file(".")
    dependsOn ( library, compiler, partest )
    aggregate ( library, compiler, partest, compat )
    settings  ( noArtifacts )
)

def partestDeps = Seq(jline, ant, scalaXml, scalaParsers, scalacheck, diffutils, testInterface)

lazy val library = project.configured also mimaDefaultSettings

lazy val compiler = project.configured dependsOn library intransitiveDeps ( /*asm,*/ jline, ant, scalaXml, scalaParsers )

lazy val partest = project.configured dependsOn compiler intransitiveDeps (partestDeps: _*)

lazy val compat = project.configured dependsOn compiler sbtDeps ( "interface", "compiler-interface" )
