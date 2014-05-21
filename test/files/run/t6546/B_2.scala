import scala.tools.partest._

object Test extends BytecodeTest {
  def show: Unit = {
    val node = loadClassNode("A_1")
    assert(node.innerClasses.isEmpty, node.innerClasses)
  }
}
