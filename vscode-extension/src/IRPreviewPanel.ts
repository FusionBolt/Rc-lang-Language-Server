import { LanguageClient } from "vscode-languageclient/node";
import { IRPreviewPanelUpdate } from "./IRPreviewProtocol";
import * as vscode from 'vscode';

export function initCustomPreview(client: LanguageClient, context: vscode.ExtensionContext) {
    IRPreviewPanel.client = client
    context.subscriptions.push(
		vscode.commands.registerCommand('rc.IRPreview.start', (params) => {
            let uri = new URL('file://' + params["path"]).href
            console.log("start")
			IRPreviewPanel.createOrShow(context.extensionUri, uri);
		})
	);

	if (vscode.window.registerWebviewPanelSerializer) {
		// Make sure we register a serializer in activation event
		vscode.window.registerWebviewPanelSerializer(IRPreviewPanel.viewType, {
			async deserializeWebviewPanel(webviewPanel: vscode.WebviewPanel, state: any) {
				console.log(`Got state: ${state}`);
				// Reset the webview options so we use latest uri for `localResourceRoots`.
				webviewPanel.webview.options = getWebviewOptions(context.extensionUri);
				// IRPreviewPanel.revive(webviewPanel, context.extensionUri);
			}
		});
	}
}

class IRPreviewPanel {
    public static client: LanguageClient
	public _panel: vscode.WebviewPanel
	public static readonly viewType = 'IRPreview';
	private _disposables: vscode.Disposable[] = [];
    private _uri: String

	public static createOrShow(extensionUri: vscode.Uri, uri: String) {
		const column = vscode.window.activeTextEditor
			? vscode.window.activeTextEditor.viewColumn
			: undefined;

		// Otherwise, create a new panel.
		const panel = vscode.window.createWebviewPanel(
			IRPreviewPanel.viewType,
			'IR Preview',
			column || vscode.ViewColumn.One,
			getWebviewOptions(extensionUri),
		);

		new IRPreviewPanel(panel, uri);
	}

	// public static revive(panel: vscode.WebviewPanel, extensionUri: vscode.Uri) {
	// 	IRPreviewPanel.currentPanel = new IRPreviewPanel(panel, extensionUri);
	// }

	constructor(panel: vscode.WebviewPanel, uri: String) {
		this._panel = panel;
        this._uri = uri
		// Set the webview's initial html content
		this._update();

		// Listen for when the panel is disposed
		// This happens when the user closes the panel or when the panel is closed programmatically
		this._panel.onDidDispose(() => this.dispose(), null, this._disposables);

		// Update the content based on view changes
		this._panel.onDidChangeViewState(
			e => {
				if (this._panel.visible) {
					this._update();
				}
			},
			null,
			this._disposables
		);

		// Handle messages from the webview
		this._panel.webview.onDidReceiveMessage(
			message => {
				switch (message.command) {
					case 'alert':
						vscode.window.showErrorMessage(message.text);
						return;
				}
			},
			null,
			this._disposables
		);
	}

	public dispose() {
		// Clean up our resources
		this._panel.dispose();

		while (this._disposables.length) {
			const x = this._disposables.pop();
			if (x) {
				x.dispose();
			}
		}
	}

	private _update() {
        this._panel.title = "IRPreview"
        IRPreviewPanel.client.sendRequest(IRPreviewPanelUpdate.type, {
            documentUri: this._uri
        }).then((result) => {
            let str = result.irs!.map(lineIR =>lineIR.flatMap(ir => ir)).map(s => `<tr><td>${s}</td></tr>`).join("\n")
            this._panel.webview.html = this._getHtmlForWebview(str)
            return undefined
        })
	}

	private _getHtmlForWebview(str: String) {
        return `<!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <title>Cat Coding</title>
        </head>
        <body>
            <table border="1">
                ${str}
            </table>
        </body>
        </html>`
	}
}

function getWebviewOptions(extensionUri: vscode.Uri): vscode.WebviewOptions {
	return {
		// Enable javascript in the webview
		enableScripts: true,

		// And restrict the webview to only loading content from our extension's `media` directory.
		localResourceRoots: [vscode.Uri.joinPath(extensionUri, 'media')]
	};
}