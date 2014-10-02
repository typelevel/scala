object Test {

  def qux[Q[_]] = 999

  qux[[x] => [y] => (x, y)]
}
