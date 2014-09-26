package scala.typelevel.annotation

import scala.annotation.meta._

@getter
final class implicitAmbiguous(msg: String) extends scala.annotation.StaticAnnotation {}
