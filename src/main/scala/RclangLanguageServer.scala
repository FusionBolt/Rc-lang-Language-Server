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
    rootUri = params.getRootUri
    assert(rootUri != null)

    initRclang()
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

  override def documentSymbol(params: DocumentSymbolParams) = computeAsync { cancelToken =>
    client.logTrace(new LogTraceParams("document symbol"))
    val uri = params.getTextDocument.getUri
    client.logMessage(new MessageParams(MessageType.Info, "documentSymbol " + uri))
    val ast = driver(uri)
    val table = symbolTable(ast)
    val symbols = table.classTable.values.flatMap { klass =>
      val methods = klass.methods.values.map { localTable =>
        val method = localTable.astNode
        new DocumentSymbol(method.name.str, SymbolKind.Method,
          method.getPositionRange,
          method.getPositionRange)}
      val fields = klass.fields.values.map { field =>
        new DocumentSymbol(field.name.str, SymbolKind.Field,
          field.getPositionRange,
          field.getPositionRange)}
      val klassSym = new DocumentSymbol(klass.astNode.name.str, SymbolKind.Class,
        klass.astNode.getPositionRange,
        klass.astNode.getPositionRange)
      if(klass.astNode.name.str != rclang.Def.Kernel) {
        methods.concat(fields).concat(List(klassSym))
      }
      else {
        methods.concat(fields)
      }
    }
    symbols.map(JEither.forRight).toList.asJava
  }

  override def completion(position: CompletionParams): CompletableFuture[JEither[util.List[CompletionItem], CompletionList]] = computeAsync { cancelToken =>
    // todo: get current position and get context completion item
    val uri = position.getTextDocument.getUri
    val keywordItems = keywords.map { k =>
      new CompletionItem(k.toString.toLowerCase)
    }.toList
    val table = symbolTable(uri)
    val methodItems = table.classTable(rclang.Def.Kernel).methods.values.map { localTable =>
      val method = localTable.astNode
      new CompletionItem(method.name.str)
    }
    val items = keywordItems.concat(methodItems)
    JEither.forRight(new CompletionList(false, items.asJava))
  }



  override def documentHighlight(params: DocumentHighlightParams): CompletableFuture[util.List[_ <: DocumentHighlight]] = computeAsync { cancelToken =>
    val uri = params.getTextDocument.getUri
    val ast = driver(uri)
    val highlights = ast.items.flatMap {
      case m@Method(decl, body) => List(new DocumentHighlight(
        m.getPositionRange,
        DocumentHighlightKind.Text))
      case _ => Nil
    }
    //    highlights.asJava
    Nil.asJava
  }

  override def hover(params: HoverParams): CompletableFuture[Hover] = computeAsync { cancelToken =>
    val uri = params.getTextDocument.getUri
    val ast = driver(uri)
    val hover = ast.items.flatMap {
      case m@Method(decl, body) => List(new Hover(
        new MarkupContent("plaintext", m.toString),
        m.getPositionRange))
      case _ => Nil
    }
    hover.head
  }

  override def definition(params: DefinitionParams): CompletableFuture[JEither[util.List[_ <: Location], util.List[_ <: LocationLink]]] = computeAsync { cancelToken =>
    val uri = params.getTextDocument.getUri
    val ast = driver(uri)
    val locations = ast.items.flatMap {
      case m@Method(decl, body) => List(new Location(uri, m.getPositionRange))
      case _ => Nil
    }
    JEither.forLeft(locations.asJava)
  }

  override def rename(params: RenameParams): CompletableFuture[WorkspaceEdit] = computeAsync { cancelToken =>
    val uri = params.getTextDocument.getUri
    new WorkspaceEdit()
  }

  override def references(params: ReferenceParams): CompletableFuture[util.List[_ <: Location]] = computeAsync { cancelToken =>
    val uri = params.getTextDocument.getUri
    val ast = driver(uri)
    val locations = ast.items.flatMap {
      case m@Method(decl, body) => List(new Location(uri, m.getPositionRange))
      case _ => Nil
    }
    locations.asJava
  }
}