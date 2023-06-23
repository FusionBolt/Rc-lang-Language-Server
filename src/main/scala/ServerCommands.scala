import org.eclipse.lsp4j.ExecuteCommandParams
import org.eclipse.lsp4j
import collection.JavaConverters.seqAsJavaListConverter

case class Command(cmd: String, desc: String) {
  def unapply(params: ExecuteCommandParams): Boolean = {
    true
  }

  def toExecuteCommandParams(): ExecuteCommandParams = {
    new ExecuteCommandParams(
      cmd,
      List[Object]().asJava,
    )
  }
}

object ServerCommands {
  val RCC = Command("rcc", "Request Client Connection")

  val all = List(RCC)

  val allIds = all.map(_.cmd).toSet
}