import org.eclipse.lsp4j.{CompletionOptions, DidChangeConfigurationParams, DidChangeTextDocumentParams, DidChangeWatchedFilesParams, DidCloseTextDocumentParams, DidOpenTextDocumentParams, DidSaveTextDocumentParams, InitializeParams, InitializeResult, ServerCapabilities}
import org.eclipse.lsp4j.jsonrpc.CompletableFutures
import org.eclipse.lsp4j.services.{LanguageClient, LanguageClientAware, LanguageServer, TextDocumentService, WorkspaceService}
import org.eclipse.lsp4j
import lsp4j.*

import java.net.URI
import scala.concurrent.Future
import scala.concurrent.Promise
import java.util.concurrent.CompletableFuture
import concurrent.ExecutionContext.Implicits.global
import collection.JavaConverters.seqAsJavaListConverter

class RclangLanguageServer extends LanguageServer with WorkspaceService with TextDocumentService with LanguageClientAware { thisServer =>
  var client: LanguageClient = _
  var rootUri: String = _

  import lsp4j.jsonrpc.{CancelChecker, CompletableFutures}
  import lsp4j.jsonrpc.messages.{Either => JEither}
  import scala.util.control.NonFatal

  def computeAsync[R](fun: CancelChecker => R): CompletableFuture[R] =
    CompletableFutures.computeAsync({ (cancelToken: CancelChecker) =>
      // We do not support any concurrent use of the compiler currently.
      thisServer.synchronized {
        cancelToken.checkCanceled()
        try {
          fun(cancelToken)
        } catch {
          case NonFatal(ex) =>
            ex.printStackTrace
            throw ex
        }
      }
    })

  override def initialize(params: InitializeParams): CompletableFuture[InitializeResult] = computeAsync { cancelToken =>
    client.logTrace(new LogTraceParams("init"))
    rootUri = params.getRootUri
    assert(rootUri != null)

    val c = new ServerCapabilities
    c.setTextDocumentSync(TextDocumentSyncKind.Full)
    c.setDocumentHighlightProvider(true)
    c.setDocumentSymbolProvider(true)
    c.setDefinitionProvider(true)
    c.setRenameProvider(true)
    c.setHoverProvider(true)
    c.setWorkspaceSymbolProvider(true)
    c.setReferencesProvider(true)
    c.setCompletionProvider(new CompletionOptions(
      /* resolveProvider = */ false,
      /* triggerCharacters = */ List(".").asJava))

    // Do most of the initialization asynchronously so that we can return early
    // from this method and thus let the client know our capabilities.
//    CompletableFuture.supplyAsync(() => drivers)

    new InitializeResult(c)
  }

  override def shutdown(): CompletableFuture[AnyRef] = {
    CompletableFuture[AnyRef]()
  }

  override def exit(): Unit = {

  }

  override def getTextDocumentService: TextDocumentService = {
    this
  }

  override def getWorkspaceService: WorkspaceService = {
    this
  }

  override def connect(client: LanguageClient): Unit = {
    this.client = client
  }

  override def didChangeConfiguration(params: DidChangeConfigurationParams): Unit = {

  }

  override def didChangeWatchedFiles(params: DidChangeWatchedFilesParams): Unit = {

  }

  override def didOpen(params: DidOpenTextDocumentParams): Unit = {

  }

  override def didChange(params: DidChangeTextDocumentParams): Unit = {
  }


  override def didClose(params: DidCloseTextDocumentParams): Unit = {

  }

  override def didSave(params: DidSaveTextDocumentParams): Unit = {
  }
}