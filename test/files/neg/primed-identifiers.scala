object a1 {
  def b(c: Char) = ???

  a1 b'c' // parses as a1 b' c'
}

object a2 {
   def b'(c: Int) = ???
   def b(c: Char) = ???

  val c' = 23
  a2 b'c' // parses as a2 b' c'
}
