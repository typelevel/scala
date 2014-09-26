object Test {
  trait =!=[C, D]

  implicit def neq[E, F] : E =!= F = null

  @typelevel.annotation.implicitAmbiguous("Could not prove ${J} =!= ${J}")
  implicit def neqAmbig1[G, H, J] : J =!= J = null
  implicit def neqAmbig2[I] : I =!= I = null

  implicitly[Int =!= Int]
}
