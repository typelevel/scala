object Test {
  trait =!=[C, D]

  @typelevel.annotation.implicitAmbiguous("Could not prove ${A} =!= ${B}")
  implicit def neqAmbig1[A] : A =!= A = null
}
