import policy.building._

def policyGlobalSettings = Seq(
    organization in ThisBuild :=  PolicyOrg,
             onLoad in Global ~=  chain(ScopedShow.dump),
  PolicyKeys.settingsDumpFile <<= fromBase("settings.dump")
)

policyGlobalSettings

// See project/BuildSettings for all the details - here we retain a high level view.
lazy val root = (
  project.rootSetup
    dependsOn ( library, compilerProject )
    aggregate ( library, compilerProject, compat )
)

lazy val library = project.setup addMima scalaLibrary

lazy val compilerProject = (
  Project(id = "compiler", base = file("compiler")).setup
    dependsOn library
    deps jline
    intransitiveTestDeps ( diffutils, testInterface )
)

// sbt compiler-interface depends on repl classes.
// lazy val repl = project.setup dependsOn compilerProject deps jline

lazy val compat = (
  project.setup.noArtifacts
    dependsOn ( compilerProject )
    sbtDeps ( "interface", "compiler-interface" )
)

bintraySettings
