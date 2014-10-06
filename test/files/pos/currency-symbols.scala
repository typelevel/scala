object A {

  val £ : Int = 0
  var ¥ : Int = 0
  def ¢ : Int = 0
  val ££ : Int = 0
  val x_£ : Int = 0

  type £ = Int
  type ¥[T] = List[T]
  type T[£] = List[£]

  type €[A, B] = Either[A, B]

  type Foo = Int € String
}
