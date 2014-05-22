import policy._, build._

seq(policyThisBuildSettings ++ policyGlobalSettings: _*)

lazy val root = ( project.sub in file(".")
      dependsOn ( library, compiler, partest )
      aggregate ( library, compiler, partest, compat )
      settings  ( getScala := scalaInstanceTask.evaluated )
)

lazy val library = ( project.sub.mima
     addSourceDirs ( "forkjoin" )
          settings (
         scalacOptions in Compile ++= strings("-sourcepath", (scalaSource in Compile).value),
                 previousArtifact :=  Some(scalaModuleId("library")),
               binaryIssueFilters ++= MimaPolicy.filters
  )
)

lazy val compiler = ( project.sub
      addSourceDirs ( "compiler", "reflect", "repl" )
          dependsOn ( library )
   intransitiveDeps ( /*asm,*/ jline, ant, scalaXml, scalaParsers )
)

lazy val compat = ( project.sub
        dependsOn ( compiler )
          sbtDeps ( "interface", "compiler-interface" )
         settings ( noArtifacts )
)

lazy val partest = ( project.sub
  dependsOn        ( compiler )
  intransitiveDeps ( jline, ant, scalaXml, scalaParsers, scalacheck, diffutils, testInterface )
  settings         (
       unmanagedBase :=  baseDirectory.value / "testlib",
                test :=  Partest.runAllTests.value,
            testOnly :=  Partest.runTests.evaluated
  )
  settings         ( noArtifacts )
)
