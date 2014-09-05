resolvers ++= Seq(
  "paulp/maven" at "https://dl.bintray.com/paulp/maven",
  Resolver.url("paulp/sbt-plugins", url("https://dl.bintray.com/paulp/sbt-plugins"))(Resolver.ivyStylePatterns)
)

libraryDependencies += "org.improving" %% "psp-const" % "1.0.0"

addSbtPlugin("me.lessis" % "bintray-sbt" % "0.1.2")

addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "0.1.6")

addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.1.6")

addSbtPlugin("org.improving" % "psp-libsbt" % "0.3.1-M3")
