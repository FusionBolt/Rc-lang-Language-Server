package scala.meta.internal.tvp

import javax.annotation.Nullable

import org.eclipse.lsp4j.jsonrpc.services.JsonNotification

trait TreeViewClient {
  @JsonNotification("Rc/treeViewDidChange")
  def RcTreeViewDidChange(params: TreeViewDidChangeParams): Unit
}

case class TreeViewChildrenParams(
                                   viewId: String,
                                   @Nullable nodeUri: String = null,
                                 )

case class TreeViewParentParams(
                                 viewId: String,
                                 @Nullable nodeUri: String = null,
                               )

case class TreeViewParentResult(
                                 @Nullable uri: String = null
                               )

case class TreeViewVisibilityDidChangeParams(
                                              viewId: String,
                                              visible: java.lang.Boolean,
                                            )

case class TreeViewNodeCollapseDidChangeParams(
                                                viewId: String,
                                                nodeUri: String,
                                                collapsed: java.lang.Boolean,
                                              )

case class RcTreeViewChildrenResult(
                                         nodes: Array[TreeViewNode]
                                       )

object RcTreeItemCollapseState {
  def collapsed: String = "collapsed"
  def expanded: String = "expanded"
  def none: String = null
}

case class TreeViewDidChangeParams(
                                    nodes: Array[TreeViewNode]
                                  )

case class TreeViewNodeRevealResult(
                                     viewId: String,
                                     uriChain: Array[String],
                                   )
