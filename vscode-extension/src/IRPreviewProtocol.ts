import {
    RequestType,
    ExecuteCommandParams,
  } from "vscode-languageserver-protocol";

export interface IRPreviewPanelUpdateParams {
    documentUri: String
}

// [lines][ir for lines]
export interface IRPreviewPanelUpdateResult { 
    irs?: String[][]
}

export namespace IRPreviewPanelUpdate {
    export const type = new RequestType<
        IRPreviewPanelUpdateParams,
        IRPreviewPanelUpdateResult,
        void
        >("Rc/irPreviewPanelUpdate");
}
