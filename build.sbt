import policy._, build._

policyBuildSettings

lazy val asm = project.sub

lazy val library = project.sub.mima plus "forkjoin" settings (
   scalacOptions in Compile ++= strings("-sourcepath", (scalaSource in Compile).value),
           previousArtifact :=  Some(scalaModuleId("library").value),
         binaryIssueFilters ++= MimaPolicy.filters
)

lazy val partest = (
  project.sub plus ("partest-extras", "partest-javaagent") dependsOn compiler intransitiveDeps (
    "org.scala-lang.modules"         % "scala-xml_2.11"                % "1.0.1",
    "org.scala-lang.modules"         % "scala-parser-combinators_2.11" % "1.0.1",
    "org.scalacheck"                 % "scalacheck_2.11"               % "1.11.3",
    "org.scala-lang"                 % "scalap"                        % "2.11.0",
    "org.apache.ant"                 % "ant"                           % "1.9.4",
    "com.googlecode.java-diff-utils" % "diffutils"                     % "1.3.0",
    "org.scala-sbt"                  % "test-interface"                %  "1.0",
    "org.scala-lang"                 % "scala-actors"                  % "2.11.0"  % "test"
  )
  settings (
                           fork in Test := true,
                                   test := Partest.runAllTests.value,
                               testOnly := Partest.runTests.evaluated,
        javacOptions in (Test, compile) := wordSeq("-nowarn"),
       scalacOptions in (Test, compile) := wordSeq("-optimize -deprecation -unchecked -Xlint")
  )
)

lazy val compiler = (
  project.sub plus ("compiler", "reflect", "repl", "interactive", "scaladoc") dependsOn (asm, library) intransitiveDeps (
    "org.apache.ant"         % "ant"                           % "1.9.4",
    "org.scala-lang.modules" % "scala-xml_2.11"                % "1.0.1", // temporary - scaladoc
    "org.scala-lang.modules" % "scala-parser-combinators_2.11" % "1.0.1"  // temporary - scaladoc
  )
  settings (
       sourceGenerators in Test <+= Partest.explodeSbtSources,
            libraryDependencies ++= jline :: sbtModuleId("interface", "compiler-interface").value
  )
)

lazy val root = (
  project in file(".")
    dependsOn (asm, library, compiler, partest)
    aggregate (asm, library, compiler, partest)
     settings (
          publishArtifact := false,
                     test := (test in partest).value,
                 testOnly := (testOnly in partest).value,
              fork in run := true,
                      run := Runners.runInput(compilerRunnerClass)("-usejavacp").evaluated,
                     repl := Runners.runInput(replRunnerClass, Props.unsuppressTraces)("-usejavacp").evaluated
    )
)
