import org.eclipse.lsp4j
import org.eclipse.lsp4j.jsonrpc.messages
import org.eclipse.lsp4j.jsonrpc.messages.Either3
import org.eclipse.lsp4j.{CallHierarchyIncomingCall, CallHierarchyIncomingCallsParams, CallHierarchyItem, CallHierarchyOutgoingCall, CallHierarchyOutgoingCallsParams, CallHierarchyPrepareParams, CodeAction, CodeActionParams, CodeLens, CodeLensParams, ColorInformation, ColorPresentation, ColorPresentationParams, CompletionItem, CompletionList, CompletionParams, CreateFilesParams, DeclarationParams, DefinitionParams, DeleteFilesParams, DidChangeConfigurationParams, DidChangeTextDocumentParams, DidChangeWatchedFilesParams, DidChangeWorkspaceFoldersParams, DidCloseTextDocumentParams, DidOpenTextDocumentParams, DidSaveTextDocumentParams, DocumentColorParams, DocumentDiagnosticParams, DocumentDiagnosticReport, DocumentFormattingParams, DocumentHighlight, DocumentHighlightParams, DocumentLink, DocumentLinkParams, DocumentOnTypeFormattingParams, DocumentRangeFormattingParams, DocumentSymbol, DocumentSymbolParams, ExecuteCommandParams, FoldingRange, FoldingRangeRequestParams, Hover, HoverParams, ImplementationParams, InlayHint, InlayHintParams, InlineValue, InlineValueParams, LinkedEditingRangeParams, LinkedEditingRanges, Location, LocationLink, Moniker, MonikerParams, PrepareRenameDefaultBehavior, PrepareRenameParams, PrepareRenameResult, ReferenceParams, RenameFilesParams, RenameParams, SelectionRange, SelectionRangeParams, SemanticTokens, SemanticTokensDelta, SemanticTokensDeltaParams, SemanticTokensParams, SemanticTokensRangeParams, SignatureHelp, SignatureHelpParams, SymbolInformation, TextDocumentPositionParams, TextEdit, TypeDefinitionParams, TypeHierarchyItem, TypeHierarchyPrepareParams, TypeHierarchySubtypesParams, TypeHierarchySupertypesParams, WillSaveTextDocumentParams, WorkspaceDiagnosticParams, WorkspaceDiagnosticReport, WorkspaceEdit, WorkspaceSymbol, WorkspaceSymbolParams}

import java.util
import java.util.concurrent.CompletableFuture
import scala.meta.internal.tvp.{RcTreeViewChildrenResult, TreeViewChildrenParams, TreeViewNodeCollapseDidChangeParams, TreeViewNodeRevealResult, TreeViewParentParams, TreeViewParentResult, TreeViewVisibilityDidChangeParams}

class DelegatingRcService(@volatile var underlying: RcLSPService) extends RcLSPService {
  override def treeViewChildren(params: TreeViewChildrenParams): CompletableFuture[RcTreeViewChildrenResult] = underlying.treeViewChildren(params)

  override def treeViewParent(params: TreeViewParentParams): CompletableFuture[TreeViewParentResult] = underlying.treeViewParent(params)

  override def treeViewReveal(params: TextDocumentPositionParams): CompletableFuture[TreeViewNodeRevealResult] = underlying.treeViewReveal(params)

  override def treeViewVisibilityDidChange(params: TreeViewVisibilityDidChangeParams): CompletableFuture[Unit] = underlying.treeViewVisibilityDidChange(params)

  override def treeViewNodeCollapseDidChange(params: TreeViewNodeCollapseDidChangeParams): CompletableFuture[Unit] = underlying.treeViewNodeCollapseDidChange(params)

  override def irPreviewPanelUpdate(params: IRPreviewPanelUpdateParams): CompletableFuture[IRPreviewPanelUpdateResult] = underlying.irPreviewPanelUpdate(params)

  override def didChangeConfiguration(params: DidChangeConfigurationParams): Unit = underlying.didChangeConfiguration(params)

  override def didChangeWatchedFiles(params: DidChangeWatchedFilesParams): Unit = underlying.didChangeWatchedFiles(params)

  override def didOpen(params: DidOpenTextDocumentParams): Unit = underlying.didOpen(params)

  override def didChange(params: DidChangeTextDocumentParams): Unit = underlying.didChange(params)

  override def didClose(params: DidCloseTextDocumentParams): Unit = underlying.didClose(params)

  override def didSave(params: DidSaveTextDocumentParams): Unit = underlying.didSave(params)

  override def executeCommand(params: ExecuteCommandParams): CompletableFuture[AnyRef] = underlying.executeCommand(params)

  override def symbol(params: WorkspaceSymbolParams): CompletableFuture[messages.Either[util.List[_ <: SymbolInformation], util.List[_ <: WorkspaceSymbol]]] = underlying.symbol(params)

  override def resolveWorkspaceSymbol(workspaceSymbol: WorkspaceSymbol): CompletableFuture[WorkspaceSymbol] = underlying.resolveWorkspaceSymbol(workspaceSymbol)

  override def didChangeWorkspaceFolders(params: DidChangeWorkspaceFoldersParams): Unit = underlying.didChangeWorkspaceFolders(params)

  override def willCreateFiles(params: CreateFilesParams): CompletableFuture[WorkspaceEdit] = underlying.willCreateFiles(params)

  override def didCreateFiles(params: CreateFilesParams): Unit = underlying.didCreateFiles(params)

  override def willRenameFiles(params: RenameFilesParams): CompletableFuture[WorkspaceEdit] = underlying.willRenameFiles(params)

  override def didRenameFiles(params: RenameFilesParams): Unit = underlying.didRenameFiles(params)

  override def willDeleteFiles(params: DeleteFilesParams): CompletableFuture[WorkspaceEdit] = underlying.willDeleteFiles(params)

  override def didDeleteFiles(params: DeleteFilesParams): Unit = underlying.didDeleteFiles(params)

  override def diagnostic(params: WorkspaceDiagnosticParams): CompletableFuture[WorkspaceDiagnosticReport] = underlying.diagnostic(params)

  override def completion(position: CompletionParams): CompletableFuture[messages.Either[util.List[CompletionItem], CompletionList]] = underlying.completion(position)

  override def resolveCompletionItem(unresolved: CompletionItem): CompletableFuture[CompletionItem] = underlying.resolveCompletionItem(unresolved)

  override def hover(params: HoverParams): CompletableFuture[Hover] = underlying.hover(params)

  override def signatureHelp(params: SignatureHelpParams): CompletableFuture[SignatureHelp] = underlying.signatureHelp(params)

  override def declaration(params: DeclarationParams): CompletableFuture[messages.Either[util.List[_ <: Location], util.List[_ <: LocationLink]]] = underlying.declaration(params)

  override def definition(params: DefinitionParams): CompletableFuture[messages.Either[util.List[_ <: Location], util.List[_ <: LocationLink]]] = underlying.definition(params)

  override def typeDefinition(params: TypeDefinitionParams): CompletableFuture[messages.Either[util.List[_ <: Location], util.List[_ <: LocationLink]]] = underlying.typeDefinition(params)

  override def implementation(params: ImplementationParams): CompletableFuture[messages.Either[util.List[_ <: Location], util.List[_ <: LocationLink]]] = underlying.implementation(params)

  override def references(params: ReferenceParams): CompletableFuture[util.List[_ <: Location]] = underlying.references(params)

  override def documentHighlight(params: DocumentHighlightParams): CompletableFuture[util.List[_ <: DocumentHighlight]] = underlying.documentHighlight(params)

  override def documentSymbol(params: DocumentSymbolParams): CompletableFuture[util.List[messages.Either[SymbolInformation, DocumentSymbol]]] = underlying.documentSymbol(params)

  override def codeAction(params: CodeActionParams): CompletableFuture[util.List[messages.Either[lsp4j.Command, CodeAction]]] = underlying.codeAction(params)

  override def resolveCodeAction(unresolved: CodeAction): CompletableFuture[CodeAction] = underlying.resolveCodeAction(unresolved)

  override def codeLens(params: CodeLensParams): CompletableFuture[util.List[_ <: CodeLens]] = underlying.codeLens(params)

  override def resolveCodeLens(unresolved: CodeLens): CompletableFuture[CodeLens] = underlying.resolveCodeLens(unresolved)

  override def formatting(params: DocumentFormattingParams): CompletableFuture[util.List[_ <: TextEdit]] = underlying.formatting(params)

  override def rangeFormatting(params: DocumentRangeFormattingParams): CompletableFuture[util.List[_ <: TextEdit]] = underlying.rangeFormatting(params)

  override def onTypeFormatting(params: DocumentOnTypeFormattingParams): CompletableFuture[util.List[_ <: TextEdit]] = underlying.onTypeFormatting(params)

  override def rename(params: RenameParams): CompletableFuture[WorkspaceEdit] = underlying.rename(params)

  override def linkedEditingRange(params: LinkedEditingRangeParams): CompletableFuture[LinkedEditingRanges] = underlying.linkedEditingRange(params)

  override def willSave(params: WillSaveTextDocumentParams): Unit = underlying.willSave(params)

  override def willSaveWaitUntil(params: WillSaveTextDocumentParams): CompletableFuture[util.List[TextEdit]] = underlying.willSaveWaitUntil(params)

  override def documentLink(params: DocumentLinkParams): CompletableFuture[util.List[DocumentLink]] = underlying.documentLink(params)

  override def documentLinkResolve(params: DocumentLink): CompletableFuture[DocumentLink] = underlying.documentLinkResolve(params)

  override def documentColor(params: DocumentColorParams): CompletableFuture[util.List[ColorInformation]] = underlying.documentColor(params)

  override def colorPresentation(params: ColorPresentationParams): CompletableFuture[util.List[ColorPresentation]] = underlying.colorPresentation(params)

  override def foldingRange(params: FoldingRangeRequestParams): CompletableFuture[util.List[FoldingRange]] = underlying.foldingRange(params)

  override def prepareRename(params: PrepareRenameParams): CompletableFuture[Either3[lsp4j.Range, PrepareRenameResult, PrepareRenameDefaultBehavior]] = underlying.prepareRename(params)

  override def prepareTypeHierarchy(params: TypeHierarchyPrepareParams): CompletableFuture[util.List[TypeHierarchyItem]] = underlying.prepareTypeHierarchy(params)

  override def typeHierarchySupertypes(params: TypeHierarchySupertypesParams): CompletableFuture[util.List[TypeHierarchyItem]] = underlying.typeHierarchySupertypes(params)

  override def typeHierarchySubtypes(params: TypeHierarchySubtypesParams): CompletableFuture[util.List[TypeHierarchyItem]] = underlying.typeHierarchySubtypes(params)

  override def prepareCallHierarchy(params: CallHierarchyPrepareParams): CompletableFuture[util.List[CallHierarchyItem]] = underlying.prepareCallHierarchy(params)

  override def callHierarchyIncomingCalls(params: CallHierarchyIncomingCallsParams): CompletableFuture[util.List[CallHierarchyIncomingCall]] = underlying.callHierarchyIncomingCalls(params)

  override def callHierarchyOutgoingCalls(params: CallHierarchyOutgoingCallsParams): CompletableFuture[util.List[CallHierarchyOutgoingCall]] = underlying.callHierarchyOutgoingCalls(params)

  override def selectionRange(params: SelectionRangeParams): CompletableFuture[util.List[SelectionRange]] = underlying.selectionRange(params)

  override def semanticTokensFull(params: SemanticTokensParams): CompletableFuture[SemanticTokens] = underlying.semanticTokensFull(params)

  override def semanticTokensFullDelta(params: SemanticTokensDeltaParams): CompletableFuture[messages.Either[SemanticTokens, SemanticTokensDelta]] = underlying.semanticTokensFullDelta(params)

  override def semanticTokensRange(params: SemanticTokensRangeParams): CompletableFuture[SemanticTokens] = underlying.semanticTokensRange(params)

  override def moniker(params: MonikerParams): CompletableFuture[util.List[Moniker]] = underlying.moniker(params)

  override def inlayHint(params: InlayHintParams): CompletableFuture[util.List[InlayHint]] = underlying.inlayHint(params)

  override def resolveInlayHint(unresolved: InlayHint): CompletableFuture[InlayHint] = underlying.resolveInlayHint(unresolved)

  override def inlineValue(params: InlineValueParams): CompletableFuture[util.List[InlineValue]] = underlying.inlineValue(params)

  override def diagnostic(params: DocumentDiagnosticParams): CompletableFuture[DocumentDiagnosticReport] = underlying.diagnostic(params)
}
