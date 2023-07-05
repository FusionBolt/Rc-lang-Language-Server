import org.eclipse.lsp4j.services.LanguageClient
import org.eclipse.lsp4j.*
import rclang.ast.{ASTNode, RcModule}

import scala.meta.internal.tvp.*

class RcTreeViewProvider(var client: LanguageClient, var ast: RcModule) extends TreeViewProvider {
  override def init(): Unit = {
  }

  override def reset(): Unit = {
  }

  override def children(params: TreeViewChildrenParams): RcTreeViewChildrenResult = {
    // command is goto line:character
    logMessage(params.nodeUri)
    logMessage(params.viewId)
    val nodes = if(params.nodeUri == null) then
      Array(new TreeViewNode("module", "module", "module", collapseState = RcTreeItemCollapseState.collapsed))
    else
      ast.items.map(item => {
        val p = new ASTPrinter(false)
        p.visit(item)
        val s = p.result
        new TreeViewNode(s, s, s, ServerCommands.GoTo.cmd)
      }).toArray
    new RcTreeViewChildrenResult(nodes)
  }

  override def parent(params: TreeViewParentParams): TreeViewParentResult = {
    new TreeViewParentResult()
  }

  private def logMessage(message: String): Unit = {
    val msg = if(message == null) then "null" else message
    client.logMessage(new MessageParams(MessageType.Info, msg));
  }
}
