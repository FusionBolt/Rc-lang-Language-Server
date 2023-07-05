import scala.meta.internal.tvp.*

trait TreeViewProvider {
  def init(): Unit = ()

  def reset(): Unit = ()

  def children(
                params: TreeViewChildrenParams
              ): RcTreeViewChildrenResult = RcTreeViewChildrenResult(Array.empty)

  def parent(
              params: TreeViewParentParams
            ): TreeViewParentResult = TreeViewParentResult()
}
