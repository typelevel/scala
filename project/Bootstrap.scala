package policy
package building

import sbt._, Keys._

trait Bootstrap {
  self: PolicyPackage =>

  def bootstrapCommands = Seq(
    Command.command("publishLocalBootstrap")(publishLocalBootstrap),
    Command.command("publishBootstrap")(publishBootstrap),
    Command.args("saveBootstrapVersion", "<version>")((state, args) => saveBootstrapVersion(args)(state))
  )

  // Creates a fresh version number, publishes bootstrap jars with that number to the local repository.
  // Records the version in project/local.properties where it has precedence over build.properties.
  // Reboots sbt under the new jars.
  private val publishLocalBootstrap: StateMap = commonBootstrap(isLocal = true, Nil)
  private val publishBootstrap: StateMap      = commonBootstrap(isLocal = false, Seq("publish"))

  private def saveBootstrapVersion(args: Seq[String]): StateMap = WState { ws =>
    val (props, newModule) = args.toList match {
      case Nil                  => localProps -> ws(bootstrapModuleId in ThisBuild)
      case "local" :: v :: Nil  => localProps -> (PolicyOrg % "bootstrap-compiler" % v)
      case "remote" :: v :: Nil => buildProps -> (PolicyOrg % "bootstrap-compiler" % v)
      case _                    => return _.fail
    }
    updateBootstrapModule(props, newModule)
    ws.info(s"Updating $BootstrapModuleProperty to $newModule in " + props.filename)
    ws
  }

  private def updateBootstrapModule(props: MutableProperties, newModule: ModuleID): Unit = {
    val m = newModule.toString
    props.write(BootstrapModuleProperty, m)
    sys.props(BootstrapModuleProperty) = m
  }

  private def bootstrapSettings(newVersion: String) = Seq(
        name in 'library := "bootstrap-library",
       name in 'compiler := "bootstrap-compiler",
     version in 'library := newVersion,
    version in 'compiler := newVersion
  )

  private def commonBootstrap(isLocal: Boolean, commands: Seq[String]): StateMap = WState { ws =>
    val newVersion = ws(version) match {
      case v if isLocal => dash(v takeWhile (_ != '-'), runSlurp("bin/unique-version"))
      case v            => v
    }
    val saveCommand = "saveBootstrapVersion %s %s".format( if (isLocal) "local" else "remote" , newVersion )
    val newCommands = "publishLocal" +: commands :+ saveCommand :+ "reboot full"

    ws set bootstrapSettings(newVersion) run newCommands
  }
}
