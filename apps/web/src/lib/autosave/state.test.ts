import { describe, expect, it } from "vitest";
import { initialSaveState, saveReducer } from "./state";

describe("saveReducer", () => {
  it("moves a changed draft through saving to saved", () => {
    const dirty = saveReducer(initialSaveState, { type: "CHANGE" });
    const saving = saveReducer(dirty, { type: "SAVE_START" });
    const saved = saveReducer(saving, { type: "SAVE_SUCCESS" });

    expect(dirty).toEqual({ phase: "dirty", message: "Unsaved changes" });
    expect(saving.message).toBe("Saving…");
    expect(saved).toEqual({ phase: "saved", message: "Saved" });
  });

  it("keeps edits visibly unsaved while offline", () => {
    const offline = saveReducer(initialSaveState, { type: "OFFLINE" });
    const edited = saveReducer(offline, { type: "CHANGE" });
    const online = saveReducer(edited, { type: "ONLINE" });

    expect(edited.message).toBe("Offline · Not saved");
    expect(online.phase).toBe("dirty");
  });

  it("exposes a retryable failure", () => {
    const failed = saveReducer(
      { phase: "saving", message: "Saving…" },
      { type: "SAVE_FAILURE" },
    );
    expect(failed).toEqual({ phase: "failed", message: "Couldn't save" });
  });
});
