import org.eclipse.lsp4j.{CompletionOptions, DidChangeConfigurationParams, DidChangeTextDocumentParams, DidChangeWatchedFilesParams, DidCloseTextDocumentParams, DidOpenTextDocumentParams, DidSaveTextDocumentParams, InitializeParams, InitializeResult, ServerCapabilities}
import org.eclipse.lsp4j.jsonrpc.CompletableFutures
import org.eclipse.lsp4j.services.{LanguageClient, LanguageClientAware, LanguageServer, TextDocumentService, WorkspaceService}
import org.eclipse.lsp4j
import lsp4j.*
import rclang.ast
import rclang.ast.Method

import java.net.URI
import java.util
import scala.concurrent.Future
import scala.concurrent.Promise
import java.util.concurrent.CompletableFuture
import scala.compat.java8.FutureConverters
import concurrent.ExecutionContext.Implicits.global
import collection.JavaConverters.seqAsJavaListConverter
import scala.util.{Failure, Success}
import scala.meta.internal.tvp.*

// https://javadoc.io/doc/org.eclipse.lsp4j/org.eclipse.lsp4j/latest/index.html
// https://javadoc.io/static/org.eclipse.lsp4j/org.eclipse.lsp4j/0.21.0/org/eclipse/lsp4j/services/TextDocumentService.html

class RcLanguageServer extends RcLSPServer with LanguageClientAware {
  thisServer =>

  var client: RcLanguageClient = _
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

//  private val rcService = new RcLanguageService(this)
  private val rcService = new DelegatingRcService(null)

  override def initialize(params: InitializeParams): CompletableFuture[InitializeResult] = computeAsync { cancelToken =>
    rootUri = params.getRootUri
    assert(rootUri != null)
    initRclang()
    val service = new RcLanguageService(this)
    rcService.underlying = service
    val c = service.getServerCapabilities
    val serverInfo = new ServerInfo("rc-lang", "0.0.1")
    new InitializeResult(c, serverInfo)
  }

  override def initialized(params: InitializedParams): CompletableFuture[Unit] = computeAsync { cancelToken =>
    logMessage("initialized")
  }

  override def shutdown(): CompletableFuture[Object] = {
    CompletableFuture.completedFuture(new Object)
  }

  override def exit(): Unit = {
    System.exit(0)
  }

  override def getRcService: RcLSPService = rcService

  override def connect(client: LanguageClient): Unit = {
    this.client = client.asInstanceOf[RcLanguageClient]
  }

  def logMessage(message: String): Unit = {
    client.logMessage(new MessageParams(MessageType.Info, message));
  }
}