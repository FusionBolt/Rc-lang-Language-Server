// The module 'vscode' contains the VS Code extensibility API
// Import the module and reference it with the alias vscode in your code below
import { Console } from 'console';
import { subscribe } from 'diagnostics_channel';
import { stat } from 'fs';
import * as vscode from 'vscode';
import { workspace, ExtensionContext, window } from 'vscode';
import {
	commands,
	DebugConfiguration,
	Disposable,
	WorkspaceFolder,
	DebugAdapterDescriptor,
	DebugConfigurationProviderTriggerKind,
	tasks,
	Task,
	ShellExecution,
  } from "vscode";

import {
	LanguageClient,
	LanguageClientOptions,
	ServerOptions,
} from 'vscode-languageclient/node';

let client: LanguageClient;

function initStatusBar() {
	let statusBarItem = vscode.window.createStatusBarItem(vscode.StatusBarAlignment.Right, 100);
	statusBarItem.text = "RclangStatusBar"
	statusBarItem.show()
	// context.subscriptions.push(statusBarItem)
	// context.subscriptions.push(vscode.window.onDidChangeActiveTextEditor())
}

function debuggerRegister(context: vscode.ExtensionContext) {
	function registerCommand(
		command: string,
		callback: (...args: any[]) => unknown
	  ) {
		console.log("register")
		context.subscriptions.push(commands.registerCommand(command, callback));
	  }

	var type = "rc-lang"
	vscode.debug.registerDebugConfigurationProvider(
		type, 
		new RclangMainConfigProvider(),
		DebugConfigurationProviderTriggerKind.Initial
		)
	vscode.debug.registerDebugAdapterDescriptorFactory(
		type,
		new RclangDebugServerFactory()
	)

	console.log("before register")
	registerCommand(
		"StartDebugSession",
		// ClientCommands.StartDebugSession,
		(param) => {
			start(false, param).then((wasStarted) => {
			if (!wasStarted) {
				window.showErrorMessage("Debug session not started");
			}
			});
		}
		);

		registerCommand(
		"StartRunSession",
		// ClientCommands.StartRunSession,
		(param) => {
			start(true, param).then((wasStarted) => {
			if (!wasStarted) {
				window.showErrorMessage("Run session not started");
			}
			});
		}
		);
}

function getServerOptions() {
	const debugOptions = [
        '-Xdebug',
        '-Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=8000,quiet=y'
    ];
    let args = ['-jar', '/Users/homura/Code/Rc-lang-Language-Server/target/scala-3.1.0/Rc-lang-Language-Server-assembly-0.1.0-SNAPSHOT.jar'];
    let cmd = '/Users/homura/Library/Java/JavaVirtualMachines/openjdk-17.0.2/Contents/Home/bin/java';
	
    // If the extension is launched in debug mode then the debug server options are used
    // Otherwise the run options are used
    const serverOptions = {
        run: { command: cmd, args: args },
        debug: { command: cmd, args: args }
    };
	return serverOptions
}

function getClientOptions() {
	const clientOptions: LanguageClientOptions = {
		documentSelector: ['rc-lang'],
		synchronize: {
			fileEvents: workspace.createFileSystemWatcher('**/*.semanticdb')
		},
		
	};
	return clientOptions
}

export function activate(context: vscode.ExtensionContext) {
	initStatusBar()
	debuggerRegister(context)
	client = new LanguageClient(
		'rclang',
		'rclang',
		getServerOptions(),
		getClientOptions()
	);
	client.start()
	console.log("client start")
}

export interface DebugDiscoveryParams {
	path: string;
	runType: RunType;
  }
  
  export enum RunType {
	Run = "run",
	RunOrTestFile = "runOrTestFile",
	TestFile = "testFile",
	TestTarget = "testTarget",
  }



class RclangMainConfigProvider implements vscode.DebugConfigurationProvider {
	async resolveDebugConfiguration(
		_folder: WorkspaceFolder | undefined,
		debugConfiguration: DebugConfiguration
	): Promise<DebugConfiguration | null> {
		console.log("Config")
		// await vscode.commands.executeCommand("rcc")

		const editor = vscode.window.activeTextEditor;
		// debugConfiguration.type is undefined if there are no configurations
		// we are running whatever is in the file
		if (debugConfiguration.type === undefined && editor) {
		const args: DebugDiscoveryParams = {
			path: editor.document.uri.toString(true),
			runType: RunType.RunOrTestFile,
		};
		debug(false, args)
		return debugConfiguration;
		} else {
			console.log("config false")
		return debugConfiguration;
		}

		return debugConfiguration
	}
}

export function debugServerFromUri(uri: string): vscode.DebugAdapterServer {
	const debugServer = vscode.Uri.parse(uri);
	const segments = debugServer.authority.split(":");
	const host = segments[0];
	const port = parseInt(segments[segments.length - 1]);
	return new vscode.DebugAdapterServer(port, host);
  }


export interface DebugSession {
	name: string;
	uri: string;
  }
  
  var DebugAdapterStart = "debug-adapter-start"

async function debug(
	noDebug: boolean,
	debugParams: DebugDiscoveryParams
  ): Promise<boolean> {

	console.log("enter debug, before DebugAdapterStart")
	// await commands.executeCommand("workbench.action.files.save");
	const response = await vscode.commands.executeCommand<DebugSession>(
	  DebugAdapterStart,
	  debugParams
	);
	console.log("after command DebugAdapterStart")
  
	if (response === undefined) {
	  return false;
	}
  
	const port = debugServerFromUri(response.uri).port;
  
	const configurationType = "rc-lang";

	const configuration: vscode.DebugConfiguration = {
	  type: configurationType,
	  name: response.name,
	  noDebug: noDebug,
	  request: "launch",
	  debugServer: port, // note: MUST be a number. vscode magic - automatically connects to the server
	};
	// commands.executeCommand("workbench.panel.repl.view.focus");
	return vscode.debug.startDebugging(undefined, configuration);
  }
  

class RclangDebugServerFactory implements vscode.DebugAdapterDescriptorFactory {
	async createDebugAdapterDescriptor(
		session: vscode.DebugSession
	): Promise<DebugAdapterDescriptor | null> {
	console.log("ServerFactory")
    if (
      session.configuration.mainClass !== undefined ||
      session.configuration.testClass !== undefined ||
      session.configuration.hostName !== undefined
    ) {
		console.log("ServerFactory DebugAdapterStart")
      const debugSession = await vscode.commands.executeCommand<DebugSession>(
        DebugAdapterStart,
        session.configuration
      );
	  console.log("after ServerFactory DebugAdapterStart")

      if (debugSession === undefined) {
        return null;
      } else {
        return debugServerFromUri(debugSession.uri);
      }
    }
    return null;
  }
}

export async function start(
	noDebug: boolean,
	debugParams: any,
  ): Promise<boolean> {
	console.log("start run")
	return debug(noDebug, debugParams)
	// if (noDebug && isExtendedRclangRunMain(debugParams)) {
	//   return runMain(debugParams);
	// } else {
	//   return debug(noDebug, debugParams);
	// }
	// return Promise.resolve(false);
  }

// This method is called when your extension is deactivated
export function deactivate() {
	if(!client) {
		return undefined;
	}
	return client.stop();
}
