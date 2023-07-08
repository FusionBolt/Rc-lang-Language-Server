import * as vscode from 'vscode';
import * as rcDebugger from "./debugger"
import * as rcux from "./ux"
import * as rcClient from "./client"
import * as rcTreeView from "./treeview"
import { ClientCommands, ExecuteClientCommand } from './LanguageClient';
import { Position, Range, Uri, ViewColumn, window, workspace } from 'vscode';
import { DocumentUri } from 'vscode-languageclient';
import { registerGoto } from './goto';

export function activate(context: vscode.ExtensionContext) {
	
	rcDebugger.debuggerRegister(context)
	let client = rcClient.getClient()
	rcux.initUX(client, context)
	let treeViews = rcTreeView.startTreeView(client, context, ["rclangASTViews"])
	context.subscriptions.concat(treeViews.disposables)
	registerGoto(context, client)
	client.start()
	console.log("client start")
}

export function deactivate() {
	rcClient.deactivate();
}
