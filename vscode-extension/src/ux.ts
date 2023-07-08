import * as vscode from 'vscode';
import { LanguageClient } from 'vscode-languageclient/node';
import { IRPreviewPanelUpdate} from './IRPreviewProtocol';
import { initCustomPreview } from './IRPreviewPanel';

export function initUX(client: LanguageClient, context: vscode.ExtensionContext) {
    initStatusBar()
    initCustomPreview(client, context)
}

function initStatusBar() {
	let statusBarItem = vscode.window.createStatusBarItem(vscode.StatusBarAlignment.Right, 100);
	statusBarItem.text = "Rc"
	statusBarItem.show()
}