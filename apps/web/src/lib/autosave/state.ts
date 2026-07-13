export type SavePhase =
  | "idle"
  | "dirty"
  | "saving"
  | "saved"
  | "failed"
  | "offline";

export type SaveState = {
  phase: SavePhase;
  message: string;
};

export type SaveAction =
  | { type: "CHANGE" }
  | { type: "SAVE_START" }
  | { type: "SAVE_SUCCESS" }
  | { type: "SAVE_FAILURE" }
  | { type: "OFFLINE" }
  | { type: "ONLINE" };

export const initialSaveState: SaveState = {
  phase: "idle",
  message: "All changes saved",
};

export function saveReducer(state: SaveState, action: SaveAction): SaveState {
  switch (action.type) {
    case "CHANGE":
      return state.phase === "offline"
        ? { phase: "offline", message: "Offline · Not saved" }
        : { phase: "dirty", message: "Unsaved changes" };
    case "SAVE_START":
      return { phase: "saving", message: "Saving…" };
    case "SAVE_SUCCESS":
      return { phase: "saved", message: "Saved" };
    case "SAVE_FAILURE":
      return { phase: "failed", message: "Couldn't save" };
    case "OFFLINE":
      return { phase: "offline", message: "Offline · Not saved" };
    case "ONLINE":
      return state.phase === "offline"
        ? { phase: "dirty", message: "Back online · Saving soon" }
        : state;
  }
}
