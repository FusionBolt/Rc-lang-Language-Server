import org.eclipse.lsp4j.services.LanguageClient
import org.eclipse.lsp4j.*
import rclang.ast.{ASTNode, RcModule}

import scala.meta.internal.tvp.*

class RcTreeViewProvider(var client: LanguageClient, private var ast: RcModule) extends TreeViewProvider {
  private var tree: ASTNodeTree = null
  def updateAST(newAst: RcModule): Unit = {
    ast = newAst
    tree = new TreeBuilder().build(newAst)
  }

  override def init(): Unit = {
  }

  override def reset(): Unit = {
  }

  override def children(params: TreeViewChildrenParams): RcTreeViewChildrenResult = {
    // command is goto line:character
    logMessage("nodeUri")
    logMessage(params.nodeUri)
    logMessage("viewId")
    logMessage(params.viewId)
    val rootLabel = "RcModule"
    val nodes = if(params.nodeUri == null) then
      val label = rootLabel
      Array(new TreeViewNode(label, label, label, collapseState = RcTreeItemCollapseState.collapsed))
    else
      val uri = if params.nodeUri == rootLabel then tree.root.label else params.nodeUri
      tree(uri).children.map(child => {
        val label = child.label
        new TreeViewNode(label, label, label, ServerCommands.GoTo.cmd)
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
