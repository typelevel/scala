resolvers += "bintray/paulp" at "https://dl.bintray.com/paulp/maven"

libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.1.2"

addSbtPlugin("me.lessis" % "bintray-sbt" % "0.1.1", sbtVersion = "0.13")

addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "0.1.6", sbtVersion = "0.13")

addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.1.6", sbtVersion = "0.13")
