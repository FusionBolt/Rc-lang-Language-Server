package scala.meta.internal.tvp

import java.{util => ju}
import javax.annotation.Nullable

case class TreeViewNode(
                         viewId: String,
                         @Nullable nodeUri: String,
                         label: String,
                         @Nullable command: String = null,
                         @Nullable icon: String = null,
                         @Nullable tooltip: String = null,
                         // One of "collapsed", "expanded" or "none"
                         @Nullable collapseState: String = null,
                       ) {
  def isDirectory: Boolean = label.endsWith("/")
//  def isCollapsed: Boolean =
//    collapseState == MetalsTreeItemCollapseState.collapsed
//  def isExpanded: Boolean =
//    collapseState == MetalsTreeItemCollapseState.expanded
//  def isNoCollapse: Boolean = collapseState == MetalsTreeItemCollapseState.none
}

object TreeViewNode {
  def fromCommand(
                   command: String,
                   icon: String = TreeViewNode.command,
                 ): TreeViewNode = null
//    TreeViewNode(
//      viewId = "commands",
//      nodeUri = s"metals://command/${command.id}",
//      label = command.title,
//      command = String(
//        command.title,
//        "metals." + command.id,
//        command.description,
//      ),
//      tooltip = command.description,
//      icon = icon,
//    )
  def command: String = "debug-start"
  def empty(viewId: String): TreeViewNode = TreeViewNode(viewId, null, viewId)
}
