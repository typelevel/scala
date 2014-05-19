import VersionKeys._

scalaModuleSettings

name                       := "scala-partest"

version                    := "1.0.1-SNAPSHOT"

scalaVersion               := "2.11.0"

scalaXmlVersion            := "1.0.1"

scalaCheckVersion          := "1.11.3"

// used as binary version when compiling against 2.12.0-SNAPSHOT
snapshotScalaBinaryVersion := "2.11"

// TODO remove this after https://github.com/scala/sbt-scala-modules/pull/7
//      is merged and this build refers to the new plugin.
scalaBinaryVersion := (
  if (scalaVersion.value.startsWith("2.12"))
    snapshotScalaBinaryVersion.value
  else
    scalaBinaryVersion.value
)

// TODO: enable "-Xfatal-warnings" for nightlies,
// off by default because we don't want to break scala/scala pr validation due to deprecation
// don't use for doc scope, scaladoc warnings are not to be reckoned with
scalacOptions in (Compile, compile) ++= Seq("-optimize", "-feature", "-deprecation", "-unchecked", "-Xlint")

// dependencies
// versions involved in integration builds / that change frequently should be keys, set above!
libraryDependencies += "org.apache.ant"                 % "ant"            % "1.8.4"

libraryDependencies += "com.googlecode.java-diff-utils" % "diffutils"      % "1.3.0"

libraryDependencies += "org.scala-sbt"                  % "test-interface" % "1.0"

// to run scalacheck tests, depend on scalacheck separately
libraryDependencies += "org.scalacheck"                %% "scalacheck"     % scalaCheckVersion.value % "provided"

// mark all scala dependencies as provided which means one has to explicitly provide them when depending on partest
// this allows for easy testing of modules (like scala-xml) that provide tested classes themselves and shouldn't
// pull in an older version of itself
libraryDependencies += "org.scala-lang.modules"        %% "scala-xml"      % scalaXmlVersion.value % "provided" intransitive()

libraryDependencies += "org.scala-lang"                 % "scalap"         % scalaVersion.value % "provided" intransitive()

libraryDependencies += "org.scala-lang"                 % "scala-reflect"  % scalaVersion.value % "provided" intransitive()

libraryDependencies += "org.scala-lang"                 % "scala-compiler" % scalaVersion.value % "provided" intransitive()
