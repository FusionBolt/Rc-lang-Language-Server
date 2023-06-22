// The module 'vscode' contains the VS Code extensibility API
// Import the module and reference it with the alias vscode in your code below
import * as vscode from 'vscode';
import { workspace, ExtensionContext, window } from 'vscode';
import {
	LanguageClient,
	LanguageClientOptions,
	ServerOptions,
} from 'vscode-languageclient/node';

let client: LanguageClient;

// This method is called when your extension is activated
// Your extension is activated the very first time the command is executed
export function activate(context: vscode.ExtensionContext) {

	// Use the console to output diagnostic information (console.log) and errors (console.error)
	// This line of code will only be executed once when your extension is activated
	console.log('Congratulations, your extension "rc-lang" is now active!');

	// If the extension is launched in debug mode then the debug server options are used
	// Otherwise the run options are used
	const serverOptions: ServerOptions = {
		run: { command: '/Users/homura/Library/Java/JavaVirtualMachines/openjdk-17.0.2/Contents/Home/bin/java', args: ['-Dfile.encoding=UTF-8', '-classpath', '/Users/homura/Code/Rc-lang-Language-Server/target/scala-3.1.0/classes:/Users/homura/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/com/google/code/findbugs/jsr305/3.0.2/jsr305-3.0.2.jar:/Users/homura/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/com/google/code/gson/gson/2.10.1/gson-2.10.1.jar:/Users/homura/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/com/google/errorprone/error_prone_annotations/2.3.4/error_prone_annotations-2.3.4.jar:/Users/homura/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/com/google/guava/failureaccess/1.0.1/failureaccess-1.0.1.jar:/Users/homura/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/com/google/guava/guava/30.1-jre/guava-30.1-jre.jar:/Users/homura/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/com/google/guava/listenablefuture/9999.0-empty-to-avoid-conflict-with-guava/listenablefuture-9999.0-empty-to-avoid-conflict-with-guava.jar:/Users/homura/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/com/google/j2objc/j2objc-annotations/1.3/j2objc-annotations-1.3.jar:/Users/homura/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/checkerframework/checker-qual/3.5.0/checker-qual-3.5.0.jar:/Users/homura/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/eclipse/lsp4j/org.eclipse.lsp4j.debug/0.20.1/org.eclipse.lsp4j.debug-0.20.1.jar:/Users/homura/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/eclipse/lsp4j/org.eclipse.lsp4j.generator/0.20.1/org.eclipse.lsp4j.generator-0.20.1.jar:/Users/homura/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/eclipse/lsp4j/org.eclipse.lsp4j.jsonrpc.debug/0.20.1/org.eclipse.lsp4j.jsonrpc.debug-0.20.1.jar:/Users/homura/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/eclipse/lsp4j/org.eclipse.lsp4j.jsonrpc/0.20.1/org.eclipse.lsp4j.jsonrpc-0.20.1.jar:/Users/homura/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/eclipse/lsp4j/org.eclipse.lsp4j/0.20.1/org.eclipse.lsp4j-0.20.1.jar:/Users/homura/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/eclipse/xtend/org.eclipse.xtend.lib.macro/2.28.0/org.eclipse.xtend.lib.macro-2.28.0.jar:/Users/homura/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/eclipse/xtend/org.eclipse.xtend.lib/2.28.0/org.eclipse.xtend.lib-2.28.0.jar:/Users/homura/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/eclipse/xtext/org.eclipse.xtext.xbase.lib/2.28.0/org.eclipse.xtext.xbase.lib-2.28.0.jar:/Users/homura/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/scala-lang/scala-library/2.13.6/scala-library-2.13.6.jar:/Users/homura/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/scala-lang/scala3-library_3/3.1.0/scala3-library_3-3.1.0.jar', 'main'] },
		debug: { command: '/Users/homura/Library/Java/JavaVirtualMachines/openjdk-17.0.2/Contents/Home/bin/java', args: ['-Dfile.encoding=UTF-8', '-classpath', '/Users/homura/Code/Rc-lang-Language-Server/target/scala-3.1.0/classes:/Users/homura/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/com/google/code/findbugs/jsr305/3.0.2/jsr305-3.0.2.jar:/Users/homura/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/com/google/code/gson/gson/2.10.1/gson-2.10.1.jar:/Users/homura/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/com/google/errorprone/error_prone_annotations/2.3.4/error_prone_annotations-2.3.4.jar:/Users/homura/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/com/google/guava/failureaccess/1.0.1/failureaccess-1.0.1.jar:/Users/homura/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/com/google/guava/guava/30.1-jre/guava-30.1-jre.jar:/Users/homura/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/com/google/guava/listenablefuture/9999.0-empty-to-avoid-conflict-with-guava/listenablefuture-9999.0-empty-to-avoid-conflict-with-guava.jar:/Users/homura/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/com/google/j2objc/j2objc-annotations/1.3/j2objc-annotations-1.3.jar:/Users/homura/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/checkerframework/checker-qual/3.5.0/checker-qual-3.5.0.jar:/Users/homura/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/eclipse/lsp4j/org.eclipse.lsp4j.debug/0.20.1/org.eclipse.lsp4j.debug-0.20.1.jar:/Users/homura/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/eclipse/lsp4j/org.eclipse.lsp4j.generator/0.20.1/org.eclipse.lsp4j.generator-0.20.1.jar:/Users/homura/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/eclipse/lsp4j/org.eclipse.lsp4j.jsonrpc.debug/0.20.1/org.eclipse.lsp4j.jsonrpc.debug-0.20.1.jar:/Users/homura/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/eclipse/lsp4j/org.eclipse.lsp4j.jsonrpc/0.20.1/org.eclipse.lsp4j.jsonrpc-0.20.1.jar:/Users/homura/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/eclipse/lsp4j/org.eclipse.lsp4j/0.20.1/org.eclipse.lsp4j-0.20.1.jar:/Users/homura/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/eclipse/xtend/org.eclipse.xtend.lib.macro/2.28.0/org.eclipse.xtend.lib.macro-2.28.0.jar:/Users/homura/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/eclipse/xtend/org.eclipse.xtend.lib/2.28.0/org.eclipse.xtend.lib-2.28.0.jar:/Users/homura/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/eclipse/xtext/org.eclipse.xtext.xbase.lib/2.28.0/org.eclipse.xtext.xbase.lib-2.28.0.jar:/Users/homura/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/scala-lang/scala-library/2.13.6/scala-library-2.13.6.jar:/Users/homura/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/scala-lang/scala3-library_3/3.1.0/scala3-library_3-3.1.0.jar', 'main']}
	};

	const clientOptions: LanguageClientOptions = {
		documentSelector: ['rclang'],
		synchronize: {
			fileEvents: workspace.createFileSystemWatcher('**/*.semanticdb')
		}
	};

	// // The command has been defined in the package.json file
	// // Now provide the implementation of the command with registerCommand
	// // The commandId parameter must match the command field in package.json
	// let disposable = vscode.commands.registerCommand('rc-lang.helloWorld', () => {
	// 	// The code you place here will be executed every time your command is executed
	// 	// Display a message box to the user
	// 	vscode.window.showInformationMessage('Hello World from Rc-lang!');

	// });

	client = new LanguageClient(
		'rclang',
		'rclang',
		serverOptions,
		clientOptions
	);
	client.start();
	// context.subscriptions.push(disposable);
}

// This method is called when your extension is deactivated
export function deactivate() {
	if(!client) {
		return undefined;
	}
	return client.stop();
}
