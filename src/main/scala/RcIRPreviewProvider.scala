import org.eclipse.lsp4j.services.LanguageClient
import org.eclipse.lsp4j.*
import rclang.ast.{ASTNode, RcModule}
import collection.JavaConverters.seqAsJavaListConverter

class RcIRPreviewProvider(var client: LanguageClient) extends IRPreviewProvider {
  override def onPanelupdate(params: IRPreviewPanelUpdateParams): IRPreviewPanelUpdateResult = {
    val ast = driver(params.documentUri)
    val (typedModule, table) = rclang.compiler.Driver.typeProc(ast)
    val mirMod = rclang.mir.MIRTranslator(table).proc(typedModule)
    IRPreviewPanelUpdateResult(mirMod.toString.split("\n").map(List(_).asJava).toList.asJava)
  }

  private def logMessage(message: String): Unit = {
    client.logMessage(new MessageParams(MessageType.Info, message));
  }
}
