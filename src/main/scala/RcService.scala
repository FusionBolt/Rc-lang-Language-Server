import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.jsonrpc.services.JsonNotification
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest

import java.util
import java.util.concurrent.CompletableFuture
import scala.meta.internal.tvp.{RcTreeViewChildrenResult, TreeViewChildrenParams, TreeViewNodeCollapseDidChangeParams, TreeViewNodeRevealResult, TreeViewParentParams, TreeViewParentResult, TreeViewVisibilityDidChangeParams}
/**
 * Interface which describes Rc specific LSP requests and notifications which are
 * implemented by Rc.
 */
trait RcService {
  @JsonRequest("Rc/treeViewChildren")
  def treeViewChildren(
                        params: TreeViewChildrenParams
                      ): CompletableFuture[RcTreeViewChildrenResult]

  @JsonRequest("Rc/treeViewParent")
  def treeViewParent(
                      params: TreeViewParentParams
                    ): CompletableFuture[TreeViewParentResult]

  @JsonRequest("Rc/treeViewReveal")
  def treeViewReveal(
                      params: TextDocumentPositionParams
                    ): CompletableFuture[TreeViewNodeRevealResult]

  @JsonNotification("Rc/treeViewVisibilityDidChange")
  def treeViewVisibilityDidChange(
                                   params: TreeViewVisibilityDidChangeParams
                                 ): CompletableFuture[Unit]

  @JsonNotification("Rc/treeViewNodeCollapseDidChange")
  def treeViewNodeCollapseDidChange(
                                     params: TreeViewNodeCollapseDidChangeParams
                                   ): CompletableFuture[Unit]
}
