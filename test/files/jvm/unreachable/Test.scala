import scala.tools.partest._
import org.objectweb.asm
import asm.tree.InsnList
import scala.collection.JavaConverters._

object Test extends BytecodeTest {
  def show: Unit = {
    val classNode = loadClassNode("Foo_1")
    // Foo_1 is full of unreachable code which if not elimintated
    // will result in NOPs as can be confirmed by adding -Ydisable-unreachable-prevention
    // to Foo_1.flags
    classNode.jmethods foreach { methodNode =>
      methodNode.jopcodes count (_ == asm.Opcodes.NOP) match {
        case 0 =>
        case n => println(s"Found $n NOP(s) in ${methodNode.name}")
      }
    }
  }
}
