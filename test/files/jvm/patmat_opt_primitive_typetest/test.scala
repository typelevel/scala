import scala.tools.partest._

object Test extends BytecodeTest {
  def show: Unit = {
    val classNode = loadClassNode("SameBytecode")
    sameBytecode(getMethod(classNode, "a"), getMethod(classNode, "b"))
  }
}
