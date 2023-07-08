import org.eclipse.lsp4j.*
import java.util.concurrent.CompletableFuture
import org.eclipse.lsp4j.jsonrpc.services.{JsonDelegate, JsonNotification, JsonRequest}
import org.eclipse.lsp4j.services.{LanguageServer, TextDocumentService, WorkspaceService}

trait RcLSPServer {
  @JsonRequest("initialize")
  def initialize(
                  params: InitializeParams
                ): CompletableFuture[InitializeResult]

  @JsonNotification("initialized")
  def initialized(params: InitializedParams): CompletableFuture[Unit]

  @JsonRequest("shutdown")
  def shutdown(): CompletableFuture[Object]

  @JsonNotification("exit")
  def exit(): Unit

  @JsonNotification("$/setTrace")
  def setTrace(params: SetTraceParams): Unit = {
    throw new UnsupportedOperationException
  }

  @JsonDelegate
  def getRcService: RcLSPService
}

trait RcLSPService extends TextDocumentService with WorkspaceService with RcService