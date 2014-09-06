object Test {
  val l = List("Identifiers", "with", "primes", "!")

  val l' = l map(_.length)

  val l'' = l zip l'

  val l''' = l''.reverse

  object a1 {
    def b(c: Char) = ???

    a1 b 'c'
  }

  object a2 {
    def b'(c: Int) = ???
    def b(c: Char) = ???

    a2 b' 23
    a2 b'23

    val i = 23
    a2 b'i

    val c' = 23
    a2 b'c' // parses as a2 b' c'

    a2 b 'c'
  }

  case object Foo'
  case class Bar'(i: Int)

  ((): Any) match {
    case foo': String => ???
    case Foo' => ???
    case Bar'(foo') => ???
  }

  val (x', y') = (13, 23)

  for (z' <- List(x', y')) x'*2

  type T' = Int
  def foo[U', F'[_]](ft: F'[T'], fu: F'[U']) = (ft, fu)
}

