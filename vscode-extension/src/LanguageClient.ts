import {
    NotificationType,
    ExecuteCommandParams,
  } from "vscode-languageserver-protocol";
  
export namespace ExecuteClientCommand {
    export const type = new NotificationType<ExecuteCommandParams>(
        "Rc/executeClientCommand"
    );
}

export const ClientCommands = {
    GoTo: "rclang.goto"
} as const;

export type ClientCommands = typeof ClientCommands;