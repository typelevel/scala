import policy._, build._

policyBuildSettings

lazy val asm = project.sub settings (
   javaSource in Compile := buildBase.value / "src" / "asm"
)

lazy val library = project.sub.mima settings (
                 scalaSource in Compile :=  buildBase.value / "src" / "library",
                  javaSource in Compile <<= scalaSource in Compile,
  unmanagedSourceDirectories in Compile +=  buildBase.value / "src" / "forkjoin",
               scalacOptions in Compile ++= strings("-sourcepath", (scalaSource in Compile).value),
                       previousArtifact :=  Some(scalaModuleId("library").value),
                     binaryIssueFilters ++= MimaPolicy.filters
)

lazy val partest = (
  project.sub dependsOn compiler intransitiveDeps (
    "org.scala-lang.modules"         % "scala-xml_2.11"                % "1.0.1",
    "org.scala-lang.modules"         % "scala-parser-combinators_2.11" % "1.0.1",
    "org.scalacheck"                 % "scalacheck_2.11"               % "1.11.3",
    "org.scala-lang"                 % "scalap"                        % "2.11.0",
    "org.apache.ant"                 % "ant"                           % "1.9.4",
    "com.googlecode.java-diff-utils" % "diffutils"                     % "1.3.0",
    "org.scala-sbt"                  % "test-interface"                %   "1.0"
  )
  settings (
    unmanagedSourceDirectories in Compile ++= (buildBase.value / "src" * "partest-*").get
  )
)

lazy val compiler = (
  project.sub dependsOn (asm, library) intransitiveDeps (
    "org.apache.ant" % "ant" % "1.9.4"
  )
  settings (
    unmanagedSourceDirectories in Compile ++= Seq(buildBase.value / "src" / "reflect", buildBase.value / "src" / "compiler"),
                 sourceGenerators in Test <+= Partest.explodeSbtSources,
                      libraryDependencies ++= jline :: sbtModuleId("interface", "compiler-interface").value
  )
)

lazy val root = (
  project in file(".")
    dependsOn (asm, library, compiler, partest)
    aggregate (asm, library, compiler, partest)
     settings (
              publishArtifact :=  false,
                  fork in run :=  true,
                 fork in Test :=  true,
          javaOptions in Test ++= Partest.testJavaOptions.value,
        scalacOptions in Test :=  Nil,
        unmanagedBase in Test :=  baseDirectory.value / "test" / "lib",
                         test :=  Partest.runAllTests.value,
                     testOnly :=  Partest.runTests.evaluated,
                          run :=  Runners.runInput(compilerRunnerClass)("-usejavacp").evaluated,
                         repl :=  Runners.runInput(replRunnerClass, Props.unsuppressTraces)("-usejavacp").evaluated
    )
)
