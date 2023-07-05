import org.eclipse.lsp4j.jsonrpc.services.JsonNotification
import org.eclipse.lsp4j.ExecuteCommandParams;

import scala.meta.internal.tvp.TreeViewClient
import org.eclipse.lsp4j.services.LanguageClient

trait RcLanguageClient extends LanguageClient with TreeViewClient {

  @JsonNotification("Rc/executeClientCommand")
  def rcExecuteClientCommand(params: ExecuteCommandParams): Unit
}
