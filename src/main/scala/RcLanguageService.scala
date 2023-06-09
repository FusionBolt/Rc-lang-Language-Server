import org.eclipse.lsp4j.{CompletionOptions, DidChangeConfigurationParams, DidChangeTextDocumentParams, DidChangeWatchedFilesParams, DidCloseTextDocumentParams, DidOpenTextDocumentParams, DidSaveTextDocumentParams, InitializeParams, InitializeResult, ServerCapabilities}
import org.eclipse.lsp4j.jsonrpc.{CancelChecker, CompletableFutures}
import org.eclipse.lsp4j.services.{LanguageClient, LanguageClientAware, LanguageServer, TextDocumentService, WorkspaceService}
import org.eclipse.lsp4j
import lsp4j.*
import rclang.ast
import rclang.ast.Method

import lsp4j.jsonrpc.messages.{Either => JEither}
import scala.util.control.NonFatal
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


class RcLanguageService(var server: RcLanguageServer) extends RcLSPService {
  private var uri: String = ""
  private val treeViewProvider = new RcTreeViewProvider(client, null)
  private val irPreviewProvider = new RcIRPreviewProvider(client)
  // todo: save context for file had been opened
  private val rcContext = new RcContext()
  rcContext.client = this.client
  def client = server.client

  def logMessage(str: String) = server.logMessage(str)

  def computeAsync[R](fun: CancelChecker => R): CompletableFuture[R] = server.computeAsync(fun)

  def getServerCapabilities: ServerCapabilities = {
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

    //    c.setCodeActionProvider(true)
    c.setCodeActionProvider(new CodeActionOptions(
      List(
        CodeActionKind.QuickFix,
        CodeActionKind.Refactor,
      ).asJava));

    // SetMatches

    c.setCodeLensProvider(new CodeLensOptions(false))

    c.setDeclarationProvider(true)
    //    c.setDiagnosticProvider(new DiagnosticRegistrationOptions(false, true))
    c.setDocumentFormattingProvider(true)
    // CodeAction的id也可以加到这里
    c.setExecuteCommandProvider(new ExecuteCommandOptions(ServerCommands.allIds.toList.asJava))
    c.setImplementationProvider(true)
    c.setInlayHintProvider(true)

    c.setLinkedEditingRangeProvider(true)
    c.setReferencesProvider(true)
    c.setMonikerProvider(true)
    c.setInlineValueProvider(true)


    val semanticTokensOption = new SemanticTokensWithRegistrationOptions(new SemanticTokensLegend(), false)
    semanticTokensOption.setFull(true)
    semanticTokensOption.setRange(false)
    semanticTokensOption.setLegend(new SemanticTokensLegend(
      SemanticTokensProvider.TokenTypes.asJava,
      SemanticTokensProvider.TokenModifiers.asJava
    ))
    c.setSemanticTokensProvider(semanticTokensOption)

    //    c.setTextDocumentSync(true)
    c.setTypeDefinitionProvider(true)
    c.setTypeHierarchyProvider(true)
    //    c.setWorkspace(true)
    c.setWorkspaceSymbolProvider(true)

    // todo: Fuzzy symbol search
    // Do most of the initialization asynchronously so that we can return early
    // from this method and thus let the client know our capabilities.
    //    CompletableFuture.supplyAsync(() => drivers)
    c
  }

  override def didChangeConfiguration(params: DidChangeConfigurationParams): Unit = {}

  override def didChangeWatchedFiles(params: DidChangeWatchedFilesParams): Unit = {}

  override def didOpen(params: DidOpenTextDocumentParams): Unit = {
    this.uri = params.getTextDocument.getUri
    initForTextDocument(this.uri)
  }

  private def initForTextDocument(uri: String): Unit = {
    rcContext.init(uri)
    treeViewProvider.updateRoot(rcContext.tree)
    logMessage("didOpen")
  }

  override def didChange(params: DidChangeTextDocumentParams): Unit = {
    this.uri = params.getTextDocument.getUri
    initForTextDocument(this.uri)
  }

  override def didClose(params: DidCloseTextDocumentParams): Unit = {}

  override def didSave(params: DidSaveTextDocumentParams): Unit = {}

  override def documentSymbol(params: DocumentSymbolParams) = computeAsync { cancelToken =>
    val symbols = rcContext.table.classTable.values.flatMap { klass =>
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

  given RcContext = rcContext
  override def hover(params: HoverParams): CompletableFuture[Hover] = computeAsync { cancelToken =>
    logMessage(f"hover: ${params.getPosition}");
    val node = getPositionNode(rcContext.ast, params.getPosition)
    node.client = this.client
    val str = node.currentNode match
      case Some(ast) => astToStrInHover(ast.node)
      case None => "not found"
    new Hover(new MarkupContent(MarkupKind.PLAINTEXT, str))
  }

  // go to definition的时候reference也会一起触发
  override def definition(params: DefinitionParams): CompletableFuture[JEither[util.List[_ <: Location], util.List[_ <: LocationLink]]] = computeAsync { cancelToken =>
    logMessage("definition")
    hoverSymbolImplement(params.getPosition)
  }

  private def hoverSymbolImplement(position: Position): JEither[util.List[_ <: Location], util.List[_ <: LocationLink]] = {
    val node = getPositionNode(rcContext.ast, position)
    node.client = this.client
    val list = node.currentNode match
      case Some(ast) => definitionList(ast).map(node => new Location(uri, node.getPositionRange)).asJava
      case None => List().asJava
    JEither.forLeft(list)
  }

  override def references(params: ReferenceParams): CompletableFuture[util.List[_ <: Location]] = computeAsync { cancelToken =>
    logMessage("references")
    val uri = params.getTextDocument.getUri
    val ast = driver(uri)
    val locations = ast.items.flatMap {
      case m@Method(decl, body) => List(new Location(uri, m.getPositionRange))
      case _ => Nil
    }
    logMessage("reference")
    locations.asJava
  }

  override def typeDefinition(params: TypeDefinitionParams): CompletableFuture[JEither[util.List[_ <: Location], util.List[_ <: LocationLink]]] = computeAsync { cancelToken =>
    logMessage("typeDefinition")
    null
  }

  override def declaration(params: DeclarationParams): CompletableFuture[JEither[util.List[_ <: Location], util.List[_ <: LocationLink]]] = computeAsync { cancelToken =>
    logMessage("declaration")
    hoverSymbolImplement(params.getPosition)
  }

  override def implementation(params: ImplementationParams): CompletableFuture[JEither[util.List[_ <: Location], util.List[_ <: LocationLink]]] = computeAsync { cancelToken =>
    logMessage("implementation")
    hoverSymbolImplement(params.getPosition)
  }

  // 输入完新的名字以后会触发
  override def rename(params: RenameParams): CompletableFuture[WorkspaceEdit] = computeAsync { cancelToken =>
    logMessage("rename")
    val uri = params.getTextDocument.getUri
    new WorkspaceEdit()
  }

  override def irPreviewPanelUpdate(params: IRPreviewPanelUpdateParams): CompletableFuture[IRPreviewPanelUpdateResult] = computeAsync { cancelToken =>
    logMessage("irPreviewPanelUpdate")
    val result = irPreviewProvider.onPanelupdate(params)
    logMessage("update end")
    result
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

  override def executeCommand(params: ExecuteCommandParams): CompletableFuture[AnyRef] = computeAsync { cancelToken =>
    logMessage("executeCommand")
    logMessage(params.toString)
    params match
      case ServerCommands.RCC(args) => {
        logMessage("rcc")
        logMessage(args.mkString("\n"))
        args(0) match
          case Some(value) => {
            build(value.fsPath, client)
            client.showMessage(new MessageParams(MessageType.Info, "build success"))
          }
          case None => logMessage("error")
      }
      case ServerCommands.GoTo(args) => {
        logMessage("goto")
        args(0) match
          case Some(value) => {
            val position = GotoParams(value).position match
              case Some(value) => value
              case None => logMessage("None Position"); ???
            logMessage(position.getLine.toString)
            logMessage(position.getCharacter.toString)

            val cmd = WindowLocation(this.uri, new Range(position, position)).toExecuteCommandParams
            client.rcExecuteClientCommand(cmd)
          }
          case None => {
            logMessage("error")
          }
      }
      case ServerCommands.Run() => {
        logMessage("debugger")
      }
      case _ => {
        logMessage("unknown command")
      }
    null
  }

  // fix or refactor, e.g. alt + enter
  override def codeAction(params: CodeActionParams): CompletableFuture[util.List[JEither[Command, CodeAction]]] = computeAsync { cancelToken =>
    logMessage("codeAction")
    val fix = new CodeAction("CodeActionFix")
    fix.setKind(CodeActionKind.QuickFix)
    val refactor = new CodeAction("CodeActionRefactor")
    refactor.setKind(CodeActionKind.Refactor)
    List(fix, refactor).map(JEither.forRight).asJava
  }

  // A code lens represents a command that should be shown along with source text, like the number of references, a way to run tests, etc.
  override def codeLens(params: CodeLensParams): CompletableFuture[util.List[_ <: CodeLens]] = computeAsync { cancelToken =>
    logMessage("codeLens")
    val ast = driver(params.getTextDocument.getUri)
    val table = symbolTable(ast)
    table.kernel.methods.values.map(l => {
      val method = l.astNode
      new CodeLens(method.name.getPositionRange, new Command("Run", "rc.CodeLen"), null)
    }).toList.asJava
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

  override def inlayHint(params: InlayHintParams): CompletableFuture[util.List[InlayHint]] = computeAsync { cancelToken =>
    logMessage("inlayHint")
    List(
      new InlayHint(params.getRange.getStart, JEither.forLeft("HintStart")),
      new InlayHint(params.getRange.getEnd, JEither.forRight(List(
        new InlayHintLabelPart("HintEnd1"),
        new InlayHintLabelPart("HintEnd2"),
      ).asJava))
    ).asJava
  }

  override def documentHighlight(params: DocumentHighlightParams): CompletableFuture[util.List[_ <: DocumentHighlight]] = computeAsync { cancelToken =>
    // 单击到某个位置会触发，比如说点到一个单词的位置，那么单词高亮
    logMessage("highlight")
//    val uri = params.getTextDocument.getUri
//    val ast = driver(uri)
//    val list = getNode(ast, params.getPosition)
//    if (list.isEmpty) {
//      Nil.asJava
//    } else {
//      val x = list.minBy(_.priority)
//      List(new DocumentHighlight(x.getPositionRange, DocumentHighlightKind.Text)).asJava
//    }
    null
  }

  override def diagnostic(params: DocumentDiagnosticParams): CompletableFuture[DocumentDiagnosticReport] = computeAsync { cancelToken =>
    logMessage("diagnostic")
    val fullReport = new RelatedFullDocumentDiagnosticReport(
      List(
        new Diagnostic(new Range(new Position(1, 1), new Position(1, 5)), "diagnosticMessage")
      ).asJava)
    //    val unchanged = new RelatedUnchangedDocumentDiagnosticReport("ResultId")
    new DocumentDiagnosticReport(fullReport)
  }

  // return type maybe lost some information
  override def semanticTokensFull(params: SemanticTokensParams): CompletableFuture[SemanticTokens] = computeAsync { cancelToken =>
    logMessage("semanticTokensFull")
    new SemanticTokens(List(new Integer(3), new Integer(3), new Integer(3)).asJava)
  }

  override def treeViewChildren(
                                 params: TreeViewChildrenParams
                               ): CompletableFuture[RcTreeViewChildrenResult] = computeAsync { cancelToken =>
    logMessage("treeViewChildren")
    treeViewProvider.children(params)
  }

  override def treeViewParent(
                               params: TreeViewParentParams
                             ): CompletableFuture[TreeViewParentResult] = computeAsync { cancelToken =>
    logMessage("treeViewParent")
    treeViewProvider.parent(params)
  }

  override def treeViewVisibilityDidChange(
                                            params: TreeViewVisibilityDidChangeParams
                                          ): CompletableFuture[Unit] = computeAsync { cancelToken =>
    logMessage("treeViewVisibilityDidChange")
  }


  override def treeViewNodeCollapseDidChange(
                                              params: TreeViewNodeCollapseDidChangeParams
                                            ): CompletableFuture[Unit] = computeAsync { cancelToken =>
    logMessage("treeViewNodeCollapseDidChange")
  }

  override def treeViewReveal(
                               params: TextDocumentPositionParams
                             ): CompletableFuture[TreeViewNodeRevealResult] = computeAsync { cancelToken =>
    logMessage("treeViewReveal")
    null
  }

  // 不知如何触发
  // A document link is a range in a text document that links to an internal or external resource, like another text document or a web site.
  override def documentLink(params: DocumentLinkParams): CompletableFuture[util.List[DocumentLink]] = computeAsync { cancelToken =>
    logMessage("documentLink")
    null
  }


  // color decorators https://code.visualstudio.com/api/language-extensions/programmatic-language-features#show-color-decorators
  // capabilities.setColorProvider(true)
  //  override def colorPresentation(params: ColorPresentationParams): CompletableFuture[util.List[ColorPresentation]] = computeAsync { cancelToken =>
  //    logMessage("colorPresentation")
  //    List(new ColorPresentation("colorPresentation")).asJava
  //  }
  //  override def documentColor(params: DocumentColorParams): CompletableFuture[util.List[ColorInformation]] = computeAsync { cancelToken =>
  //    logMessage("documentColor")
  //    null
  //  }


  override def moniker(params: MonikerParams): CompletableFuture[util.List[Moniker]] = computeAsync { cancelToken =>
    logMessage("moniker")
    null
  }

  override def symbol(params: WorkspaceSymbolParams): CompletableFuture[JEither[util.List[_ <: SymbolInformation], util.List[_ <: WorkspaceSymbol]]] = computeAsync { cancelToken =>
    logMessage("Symbol")
    val uri = params.getQuery
    val ast = driver(uri)
    val symbols = ast.items.flatMap {
      case m@Method(decl, body) => List(new SymbolInformation(m.toString, SymbolKind.Method, new Location(uri, m.getPositionRange)))
      case _ => Nil
    }
    JEither.forLeft(Nil.asJava)
  }

  //  // used for debugger, like inlayHint End, should enable in settings
  //  override def inlineValue(params: InlineValueParams): CompletableFuture[util.List[InlineValue]] = computeAsync { cancelToken =>
  //    logMessage("inlineValue")
  //    List(
  //      new InlineValue(new InlineValueText(params.getRange, "inlineValue")),
  //      new InlineValue(new InlineValueVariableLookup(params.getRange, false, "VarName")),
  //      new InlineValue(new InlineValueEvaluatableExpression(params.getRange, "inlineValueExpr"))
  //    ).asJava
  //  }
}
