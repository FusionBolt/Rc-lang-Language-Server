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

// https://javadoc.io/doc/org.eclipse.lsp4j/org.eclipse.lsp4j/latest/index.html
// https://javadoc.io/static/org.eclipse.lsp4j/org.eclipse.lsp4j/0.21.0/org/eclipse/lsp4j/services/TextDocumentService.html
class RclangLanguageServer extends LanguageServer with WorkspaceService with TextDocumentService with LanguageClientAware {
  thisServer =>
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

//    val f = new FileOperationsServerCapabilities
//    val d = new WorkspaceServerCapabilities
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
    c.setSignatureHelpProvider(new SignatureHelpOptions(
      /* triggerCharacters = */ List("(").asJava))

    c.setCallHierarchyProvider(true)
    c.setCodeActionProvider(true)
    c.setCodeLensProvider(new CodeLensOptions(false))

    c.setDeclarationProvider(true)
//    c.setDiagnosticProvider(new DiagnosticRegistrationOptions(false, true))
    c.setDocumentFormattingProvider(true)
    c.setExecuteCommandProvider(new ExecuteCommandOptions(ServerCommands.allIds.toList.asJava))
    c.setImplementationProvider(true)
    c.setInlayHintProvider(true)
    c.setInlineValueProvider(true)
    c.setLinkedEditingRangeProvider(true)
    c.setMonikerProvider(true)
    c.setReferencesProvider(true)
    c.setSemanticTokensProvider(new SemanticTokensWithRegistrationOptions(new SemanticTokensLegend(), false))
//    c.setTextDocumentSync(true)
    c.setTypeDefinitionProvider(true)
    c.setTypeHierarchyProvider(true)
//    c.setWorkspace(true)
    c.setWorkspaceSymbolProvider(true)

    // todo: Fuzzy symbol search
    // Do most of the initialization asynchronously so that we can return early
    // from this method and thus let the client know our capabilities.
    //    CompletableFuture.supplyAsync(() => drivers)

    new InitializeResult(c)
  }

  override def shutdown(): CompletableFuture[Object] = {
    CompletableFuture.completedFuture(new Object)
  }

  override def exit(): Unit = {
    System.exit(0)
  }

  override def getTextDocumentService: TextDocumentService = this

  override def getWorkspaceService: WorkspaceService = this

  override def connect(client: LanguageClient): Unit = {
    this.client = client
  }

  override def didChangeConfiguration(params: DidChangeConfigurationParams): Unit = {}

  override def didChangeWatchedFiles(params: DidChangeWatchedFilesParams): Unit = {}

  override def didOpen(params: DidOpenTextDocumentParams): Unit = {}

  override def didChange(params: DidChangeTextDocumentParams): Unit = {}

  override def didClose(params: DidCloseTextDocumentParams): Unit = {}

  override def didSave(params: DidSaveTextDocumentParams): Unit = {}


  override def documentSymbol(params: DocumentSymbolParams) = computeAsync { cancelToken =>
    val uri = params.getTextDocument.getUri
    val ast = driver(uri)
    logMessage("documentSymbol " + uri)
    //    logMessage("ast:" + ast.toString)
    logMessage("ast:\n" + astToStr(ast))
    val table = symbolTable(ast)
    val symbols = table.classTable.values.flatMap { klass =>
      val methods = klass.methods.values.map { localTable =>
        val method = localTable.astNode
        new DocumentSymbol(method.name.str, SymbolKind.Method,
          method.getPositionRange,
          method.getPositionRange)
      }
      val fields = klass.fields.values.map { field =>
        new DocumentSymbol(field.name.str, SymbolKind.Field,
          field.getPositionRange,
          field.getPositionRange)
      }
      val klassSym = new DocumentSymbol(klass.astNode.name.str, SymbolKind.Class,
        klass.astNode.getPositionRange,
        klass.astNode.getPositionRange)
      if (klass.astNode.name.str != rclang.Def.Kernel) {
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
    // 单击到某个位置会触发
    logMessage("highlight")
    val uri = params.getTextDocument.getUri
    val ast = driver(uri)
    val list = getNode(ast, params.getPosition)
    if(list.isEmpty) {
      Nil.asJava
    } else {
      val x = list.minBy(_.priority)
      List(new DocumentHighlight(x.getPositionRange, DocumentHighlightKind.Text)).asJava
    }
  }

  override def hover(params: HoverParams): CompletableFuture[Hover] = computeAsync { cancelToken =>
    val uri = params.getTextDocument.getUri
    val ast = driver(uri)

    logMessage(f"hover: ${params.getPosition}");
    // get Position
    // get ast node on position
    // show content about ast node
    val list = getNode(ast, params.getPosition)
    logMessage(list.map(_.toString).mkString("\n\n"))
    if list.isEmpty then {
      new Hover(new MarkupContent("plaintext", "not found"))
    }
    else {
      // sort and get the minimal one
      val x = list.maxBy(_.priority)
      val str = x match
        case klass: rclang.ast.Class => s"class ${klass.name.str}"
        case method: rclang.ast.Method => s"method ${method.name.str}"
        case _ => s"${x.getClass.getSimpleName}: ${x.toString}"
      val content = new MarkupContent("plaintext", str)
      new Hover(content)
    }
  }

  private def logMessage(message: String): Unit = {
    client.logMessage(new MessageParams(MessageType.Info, message));
  }

  // go to definition的时候reference也会一起触发
  override def definition(params: DefinitionParams): CompletableFuture[JEither[util.List[_ <: Location], util.List[_ <: LocationLink]]] = computeAsync { cancelToken =>
    val uri = params.getTextDocument.getUri
    val ast = driver(uri)
    val locations = ast.items.flatMap {
      case m@Method(decl, body) => List(new Location(uri, m.getPositionRange))
      case _ => Nil
    }
    logMessage("definition")
    JEither.forLeft(locations.asJava)
  }

  override def references(params: ReferenceParams): CompletableFuture[util.List[_ <: Location]] = computeAsync { cancelToken =>
    val uri = params.getTextDocument.getUri
    val ast = driver(uri)
    val locations = ast.items.flatMap {
      case m@Method(decl, body) => List(new Location(uri, m.getPositionRange))
      case _ => Nil
    }
    logMessage("reference")
    locations.asJava
  }

  // 输入完新的名字以后会触发
  override def rename(params: RenameParams): CompletableFuture[WorkspaceEdit] = computeAsync { cancelToken =>
    val uri = params.getTextDocument.getUri
    logMessage("rename")
    new WorkspaceEdit()
  }

  override def signatureHelp(params: SignatureHelpParams): CompletableFuture[SignatureHelp] = computeAsync { cancelToken =>
    val uri = params.getTextDocument.getUri
    val ast = driver(uri)
    val signatureHelp = ast.items.flatMap {
      case m@Method(decl, body) => List(new SignatureInformation(m.toString))
      case _ => Nil
    }
    logMessage("signatureHelp")
    new SignatureHelp(signatureHelp.asJava, 1, 1)
  }

  override def symbol(params: WorkspaceSymbolParams): CompletableFuture[JEither[util.List[_ <: SymbolInformation], util.List[_ <: WorkspaceSymbol]]] = computeAsync { cancelToken =>
//    val uri = params.getQuery
//    val ast = driver(uri)
//    val symbols = ast.items.flatMap {
//      case m@Method(decl, body) => List(new SymbolInformation(m.toString, SymbolKind.Method, m.getPositionRange))
//      case _ => Nil
//    }
    JEither.forLeft(Nil.asJava)
  }

  override def executeCommand(params: ExecuteCommandParams): CompletableFuture[AnyRef] = computeAsync { cancelToken =>
    logMessage("executeCommand")
    params match
      case ServerCommands.RCC() => logMessage("rcc")
      case _ => logMessage("unknown command")
    null
  }

  // fix or refactor, e.g. alt + enter
  override def codeAction(params: CodeActionParams): CompletableFuture[util.List[JEither[Command, CodeAction]]] = computeAsync { cancelToken =>
    logMessage("colorPresentation")
    null
  }

  // A code lens represents a command that should be shown along with source text, like the number of references, a way to run tests, etc.
  // 是不是类似于在上面显示一个[Run]一样的东西
  override def codeLens(params: CodeLensParams): CompletableFuture[util.List[_ <: CodeLens]] = computeAsync { cancelToken =>
    logMessage("prepareCallHierarchy")
    null
  }

  // A document link is a range in a text document that links to an internal or external resource, like another text document or a web site.
  override def documentLink(params: DocumentLinkParams): CompletableFuture[util.List[DocumentLink]] = computeAsync { cancelToken =>
    logMessage("callHierarchyIncomingCalls")
    null
  }

  override def documentColor(params: DocumentColorParams): CompletableFuture[util.List[ColorInformation]] = computeAsync { cancelToken =>
    logMessage("documentColor")
    Nil.asJava
  }

  override def colorPresentation(params: ColorPresentationParams): CompletableFuture[util.List[ColorPresentation]] = computeAsync { cancelToken =>
    logMessage("colorPresentation")
    null
  }

  override def prepareCallHierarchy(params: CallHierarchyPrepareParams): CompletableFuture[util.List[CallHierarchyItem]] = computeAsync { cancelToken =>
    logMessage("prepareCallHierarchy")
    null
  }

  override def callHierarchyIncomingCalls(params: CallHierarchyIncomingCallsParams): CompletableFuture[util.List[CallHierarchyIncomingCall]] = computeAsync { cancelToken =>
    logMessage("callHierarchyIncomingCalls")
    null
  }

  override def callHierarchyOutgoingCalls(params: CallHierarchyOutgoingCallsParams): CompletableFuture[util.List[CallHierarchyOutgoingCall]] = computeAsync { cancelToken =>
    logMessage("callHierarchyOutgoingCalls")
    null
  }

  override def semanticTokensFull(params: SemanticTokensParams): CompletableFuture[SemanticTokens] = computeAsync { cancelToken =>
    logMessage("semanticTokensFull")
    null
  }

  override def semanticTokensFullDelta(params: SemanticTokensDeltaParams): CompletableFuture[JEither[SemanticTokens, SemanticTokensDelta]] = computeAsync { cancelToken =>
    logMessage("semanticTokensFullDelta")
    null
  }

  override def semanticTokensRange(params: SemanticTokensRangeParams): CompletableFuture[SemanticTokens] = computeAsync { cancelToken =>
    logMessage("semanticTokensRange")
    null
  }

  override def moniker(params: MonikerParams): CompletableFuture[util.List[Moniker]] = computeAsync { cancelToken =>
    logMessage("moniker")
    null
  }

  override def inlayHint(params: InlayHintParams): CompletableFuture[util.List[InlayHint]] = computeAsync { cancelToken =>
    logMessage("inlayHint")
    null
  }

  override def inlineValue(params: InlineValueParams): CompletableFuture[util.List[InlineValue]] = computeAsync { cancelToken =>
    logMessage("inlineValue")
    null
  }

  override def diagnostic(params: DocumentDiagnosticParams): CompletableFuture[DocumentDiagnosticReport] = computeAsync { cancelToken =>
    logMessage("diagnostic")
    null
  }
}