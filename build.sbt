import policy._, build._

// A fully-qualified reference to a setting or task looks like:
//
// {<build-uri>}<project-id>/config:inkey::key

initialCommands in console := """import policy.build._"""

seq(policyBuildSettings: _*)

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
  settings (
    unmanagedJars in (Compile, compile) := Nil
  )
)

lazy val root = ( project.sub in file(".")
    dependsOn ( asm, library, compiler, partest )
    aggregate ( asm, library, compiler, partest )
     settings (
      sourceDirectories :=  Nil,
          bootstrapInfo <<= bootstrapInfoOutput map printResult("bootstrap"),
        publishArtifact :=  false,
               getScala :=  scalaInstanceTask.evaluated,
           fork in Test :=  true,
                   test :=  Partest.runAllTests.value,
               testOnly :=  Partest.runTests.evaluated,
                    run :=  Runners.runInput(CompilerRunnerClass)("-usejavacp").evaluated,
                   repl :=  Runners.runInput(ReplRunnerClass, Props.unsuppressTraces)("-usejavacp").evaluated
    )
)
