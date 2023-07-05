import { DocumentUri, LanguageClient } from "vscode-languageclient/node";
import { ClientCommands, ExecuteClientCommand } from "./LanguageClient";
import { ExtensionContext, Range, Uri, ViewColumn, window, workspace } from "vscode";

export interface WindowLocation {
    uri: DocumentUri;
    range: Range;
    otherWindow: boolean;
}

export function registerGoto(context: ExtensionContext, client: LanguageClient) {
    let dispose = client.onNotification(ExecuteClientCommand.type, 
		(params) => {
			switch (params.command) {
				case ClientCommands.GoTo: {
					const location = params.arguments?.[0] as WindowLocation
					const range = new Range(
						location.range.start.line,
						location.range.start.character,
						location.range.end.line,
						location.range.end.character
					  );
					let vs = ViewColumn.Active;
					if (location.otherWindow) {
					vs =
						window.visibleTextEditors
						.filter(
							(vte) =>
							window.activeTextEditor?.document.uri.scheme != "output" &&
							vte.viewColumn
						)
						.pop()?.viewColumn || ViewColumn.Beside;
					}
					const uri = Uri.parse(location.uri);
					// vscode will cache the virtual documents even after closing unless onDidChange is fired
					workspace.openTextDocument(uri).then((textDocument) =>
					window.showTextDocument(textDocument, {
						selection: range,
						viewColumn: vs,
					})
					);
				}
			}
		})
	context.subscriptions.push(dispose)
}