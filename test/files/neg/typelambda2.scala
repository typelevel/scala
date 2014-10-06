object Test {

  def qux[Q[_]] = 999

  qux[[x[_]] => x[Double]]
  qux[[x, y] => (x => y)]
  qux[x => y]
}
