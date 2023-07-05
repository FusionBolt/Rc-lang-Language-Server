import { CancellationToken, Disposable, Event, EventEmitter, ExtensionContext, OutputChannel, ProviderResult, TreeDataProvider, TreeItem, TreeItemCollapsibleState, TreeView, window } from "vscode";
import { LanguageClient } from "vscode-languageclient/node";
import { RcTreeViewChildren, RcTreeViewDidChange, RcTreeViewNode, RcTreeViewNodeCollapseDidChange, RcTreeViewParent, RcTreeViewVisibilityDidChange, RcTreeViews } from "./TreeViewProtocol";

export function startTreeView(client: LanguageClient, context: ExtensionContext, viewIds: string[]): RcTreeViews {
    const providers: Map<string, RcTreeDataProvider> = new Map()
    const allViews: Map<string, TreeView<string>> = new Map();

    const disposables = viewIds.map((viewId) => {
        const provider = new RcTreeDataProvider(client, viewId, providers, context);
        providers.set(viewId, provider);
        const view = window.createTreeView(viewId, {
            treeDataProvider: provider,
            showCollapseAll: true,
        })
        allViews.set(viewId, view)
            // Notify the server about view visibility changes
        const onDidChangeVisibility = view.onDidChangeVisibility((e) => {
            client.sendNotification(RcTreeViewVisibilityDidChange.type, {
            viewId: viewId,
            visible: e.visible,
            });
        });
        const onDidChangeExpandNode = view.onDidExpandElement((e) => {
            // expandedNode(viewId).add(e.element);
            client.sendNotification(RcTreeViewNodeCollapseDidChange.type, {
            viewId: viewId,
            nodeUri: e.element,
            collapsed: false,
            });
        });
        const onDidChangeCollapseNode = view.onDidCollapseElement((e) => {
            // expandedNode(viewId).delete(e.element);
            client.sendNotification(RcTreeViewNodeCollapseDidChange.type, {
            viewId: viewId,
            nodeUri: e.element,
            collapsed: true,
            });
        });
        return [
            view,
            onDidChangeVisibility,
            onDidChangeExpandNode,
            onDidChangeCollapseNode,
        ];
    })
    const treeViewDidChangeDispoasble = client.onNotification(
        RcTreeViewDidChange.type, 
        (params) => {
            console.log("treeViewDidChangeDispoasble")
        }
    );

    context.subscriptions.push(treeViewDidChangeDispoasble);
    return {
        disposables: ([] as Disposable[]).concat(...disposables),
        reveal(params) {
            console.log("reveal")
        },
    }
}

// NOTE(olafur): Copy-pasted from Stack Overflow, would be nice to move it elsewhere.
function notEmpty<TValue>(value: TValue | null | undefined): value is TValue {
    return value !== null && value !== undefined;
  }

class RcTreeDataProvider implements TreeDataProvider<string> {
    didChange = new EventEmitter<string | undefined>();
    onDidChangeTreeData = this.didChange.event;
    items: Map<string, RcTreeViewNode> = new Map();
    constructor(
        readonly client: LanguageClient,
        readonly viewId: string,
        readonly views: Map<string, RcTreeDataProvider>,
        readonly context: ExtensionContext
      ) {}

    getTreeItem(element: string): TreeItem | Thenable<TreeItem> {
        console.log("getTreeItem")
        console.log(element)
        return new TreeItem(element, TreeItemCollapsibleState.Collapsed)
    }
    getChildren(uri?: string | undefined): ProviderResult<string[] | undefined> {
        return this.client.sendRequest(RcTreeViewChildren.type, {
            viewId: this.viewId,
            nodeUri: uri
        }).then((result) => {
            result.nodes.forEach((n) => {
                if(n.nodeUri) {
                    this.items.set(n.nodeUri, n)
                }
            })
            return result.nodes.map((n) => n.nodeUri).filter(notEmpty)
        })
    }
    getParent?(uri: string): ProviderResult<string> {
        return "parent"
        return this.client.sendRequest(RcTreeViewParent.type, {
          viewId: this.viewId,
          nodeUri: uri,
        })
        .then((result) => {
          if (result.uri) {
            const item = this.items.get(result.uri);
            if (item) {
              item.collapseState;
            }
          }
          return result.uri;
        });
    }
    resolveTreeItem?(item: TreeItem, element: string, token: CancellationToken): ProviderResult<TreeItem> {
        throw new Error("Method not implemented.");
    }

}