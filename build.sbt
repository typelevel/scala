import policy._, build._
import Classpaths.publishConfig

initialCommands in console := """import policy.build._"""

policyBuildSettings

publishLocalConfiguration ~= (p => publishConfig(p.artifacts, p.ivyFile, p.checksums, p.resolverName, logging = UpdateLogging.Quiet, overwrite = false))

lazy val asm = project.sub

lazy val library = ( project.sub.mima
  addSourceDirs ( "forkjoin" )
       settings (
         scalacOptions in Compile ++= strings("-sourcepath", (scalaSource in Compile).value),
                 previousArtifact :=  Some(scalaModuleId("library")),
               binaryIssueFilters ++= MimaPolicy.filters
  )
)

lazy val compiler = ( project.sub
     addSourceDirs ( "compiler", "reflect", "repl", "scaladoc" )
         dependsOn ( asm, library )
  intransitiveDeps ( jline, ant, scalaXml, scalaParsers )
)

lazy val partest = ( project.sub
  dependsOn        ( compiler )
  intransitiveDeps ( jline, ant, scalaXml, scalaParsers, scalacheck, diffutils, testInterface )
  sbtTestDeps      ( "interface", "compiler-interface" )
  settings         ( sourceGenerators in Test <+= Partest.explodeSbtSources )
)

lazy val root = ( project.sub in file(".")
    dependsOn ( asm, library, compiler, partest )
    aggregate ( asm, library, compiler, partest )
     settings (
                  bootstrapInfo <<= bootstrapInfoOutput map printResult("bootstrap"),
                       getScala :=  scalaInstanceTask.evaluated,
                   fork in Test :=  true,
                           test :=  Partest.runAllTests.value,
                       testOnly :=  Partest.runTests.evaluated,
                            run :=  Runners.runInput(compilerRunnerClass)("-usejavacp").evaluated,
                           repl :=  Runners.runInput(replRunnerClass, Props.unsuppressTraces)("-usejavacp").evaluated
    )
)
