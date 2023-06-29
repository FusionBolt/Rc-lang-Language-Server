import {
    commands,
    DebugConfiguration,
    WorkspaceFolder,
    DebugAdapterDescriptor,
    DebugConfigurationProviderTriggerKind,
    window,
} from "vscode";
import * as vscode from 'vscode';

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

export function debuggerRegister(context: vscode.ExtensionContext) {
    function registerCommand(
        command: string,
        callback: (...args: any[]) => unknown
    ) {
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