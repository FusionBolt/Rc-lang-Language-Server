"use strict";

import {
  Disposable,
  Command,
  RequestType,
  NotificationType,
  TextDocumentPositionParams,
} from "vscode-languageserver-protocol";

export interface RcTreeViews {
  disposables: Disposable[];
  reveal(params: RcTreeRevealResult): void;
}

export interface RcTreeViewNode {
  /** The ID of the view that this node is associated with. */
  viewId: string;
  /** The URI of this node, or undefined if root node of the view. */
  nodeUri?: string;
  /** The title to display for this node. */
  label: string;
  /** An optional command to trigger when the user clicks on this tree view node. */
  command?: Command;
  /** An optional SVG icon to display next to the label of this tree node. */
  icon?: string;
  /** An optional description of this tree node that is displayed when the user hovers over this node. */
  tooltip?: string;
  /**
   * Whether this tree node should be collapsed, expanded or if it has no children.
   *
   * - undefined: this node has no children.
   * - collapsed: this node has children and this node should be auto-expanded
   *   on the first load.
   * - collapsed: this node has children and the user should manually expand
   *   this node to see the children.
   */
  collapseState?: "collapsed" | "expanded";
}

export interface RcTreeViewChildrenParams {
  /** The ID of the view that is node is associated with. */
  viewId: string;
  /** The URI of the parent node. */
  nodeUri?: string;
}

export interface RcTreeViewChildrenResult {
  /** The child nodes of the requested parent node. */
  nodes: RcTreeViewNode[];
}

export namespace RcTreeViewChildren {
  export const type = new RequestType<
    RcTreeViewChildrenParams,
    RcTreeViewChildrenResult,
    void
  >("Rc/treeViewChildren");
}

export interface RcTreeViewDidChangeParams {
  nodes: RcTreeViewNode[];
}
export namespace RcTreeViewDidChange {
  export const type = new NotificationType<RcTreeViewDidChangeParams>(
    "Rc/treeViewDidChange"
  );
}

export interface RcTreeViewParentParams {
  viewId: string;
  nodeUri: string;
}

export interface RcTreeViewParentResult {
  uri?: string;
}

export namespace RcTreeViewParent {
  export const type = new RequestType<
    RcTreeViewParentParams,
    RcTreeViewParentResult,
    void
  >("Rc/treeViewParent");
}

export interface RcTreeViewVisibilityDidChangeParams {
  /** The ID of the view that this node is associated with. */
  viewId: string;
  /** True if the node is visible, false otherwise. */
  visible: boolean;
}

export namespace RcTreeViewVisibilityDidChange {
  export const type =
    new NotificationType<RcTreeViewVisibilityDidChangeParams>(
      "Rc/treeViewVisibilityDidChange"
    );
}

export interface RcTreeViewNodeCollapseDidChangeParams {
  /** The ID of the view that this node is associated with. */
  viewId: string;
  /** The URI of the node that was collapsed or expanded. */
  nodeUri: string;
  /** True if the node is collapsed, false if the node was expanded. */
  collapsed: boolean;
}

export namespace RcTreeViewNodeCollapseDidChange {
  export const type =
    new NotificationType<RcTreeViewNodeCollapseDidChangeParams>(
      "Rc/treeViewNodeCollapseDidChange"
    );
}

export interface RcTreeRevealResult {
  /** The ID of the view that this node is associated with. */
  viewId: string;
  /**
   * The list of URIs for the node to reveal and all of its ancestor parents.
   *
   * The node to reveal is at index 0, it's parent is at index 1 and so forth
   * up until the root node.
   */
  uriChain: string[];
}

export namespace RcTreeViewReveal {
  export const type = new RequestType<
    TextDocumentPositionParams,
    RcTreeRevealResult,
    void
  >("Rc/treeViewReveal");
}
