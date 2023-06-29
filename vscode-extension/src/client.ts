import { workspace } from "vscode";
import { LanguageClientOptions } from "vscode-languageclient";
import { LanguageClient } from "vscode-languageclient/node";

let client: LanguageClient;

export function getClient() {
    if(!client) {
        client = new LanguageClient(
            'rclang',
            'rclang',
            getServerOptions(),
            getClientOptions()
        );
    }
    return client
}

export function deactivate() {
    if(!client) {
		return undefined;
	}
	return client.stop();
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