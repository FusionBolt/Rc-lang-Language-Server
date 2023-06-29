import * as vscode from 'vscode';

export function initUX() {
    initStatusBar()
}

function initStatusBar() {
	let statusBarItem = vscode.window.createStatusBarItem(vscode.StatusBarAlignment.Right, 100);
	statusBarItem.text = "RclangStatusBar"
	statusBarItem.show()
}