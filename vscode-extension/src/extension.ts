import * as vscode from 'vscode';
import * as rcDebugger from "./debugger"
import * as rcux from "./ux"
import * as rcClient from "./client"
import * as rcTreeView from "./treeview"

export function activate(context: vscode.ExtensionContext) {
	rcux.initUX()
	
	rcDebugger.debuggerRegister(context)
	let client = rcClient.getClient()
	let treeViews = rcTreeView.startTreeView(client, context, ["rclangASTViews"])
	context.subscriptions.concat(treeViews.disposables)
	client.start()
	console.log("client start")
}

export function deactivate() {
	rcClient.deactivate();
}
