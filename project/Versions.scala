package policy
package build

import sbt._, Keys._

trait Bootstrap {
  def bootstrapProperty   = sys.props.getOrElse("policy.bootstrap", "scala")
  def buildProperty       = sys.props.getOrElse("policy.build", "1.0.0-SNAPSHOT")
  def isBuildingWithScala = bootstrapProperty == "scala"
  def bootVersion         = Def setting ( if (isBuildingWithScala) scalaVersion.value else bootstrapProperty )
  def buildVersion        = buildProperty

  def bootstrapCompiler = Def setting (
    if (isBuildingWithScala || thisProject.value.id == "compiler")
      "org.scala-lang" % "scala-compiler" % scalaVersion.value % "scala-tool"
    else
      organization.value % "policy-compiler" % bootVersion.value % "scala-tool"
  )

  def bootstrapInstance = Def.task[ScalaInstance] {
    val toolReport = update.value.configuration("scala-tool").get
    val j1 :: j2 :: js = toolReport.modules.toList flatMap (_.artifacts map (_._2))
    ScalaInstance(bootVersion.value, j1, j2, js: _*)(state.value.classLoaderCache.apply)
  }

  def policyBuildSettings = globally(
        organization := "org.improving",
        scalaVersion := "2.11.0",
             version := buildVersion,
          crossPaths := false
  )
}

