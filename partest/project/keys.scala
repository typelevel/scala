object VersionKeys {
  import sbt.settingKey

  // To facilitate scripted build of all modules (while we're working on getting dbuild up and running)
  val scalaXmlVersion   = settingKey[String]("Version to use for the scala-xml dependency.")
  val scalaCheckVersion = settingKey[String]("Version to use for the scalacheck dependency.")
}
