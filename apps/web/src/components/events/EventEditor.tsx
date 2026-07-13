"use client";

import {
  useCallback,
  useEffect,
  useReducer,
  useRef,
  useState,
} from "react";
import { useRouter } from "next/navigation";
import type {
  Event,
  UpdateEventRequest,
  UpdateEventRequestEventTypeEnum,
  Venue,
  Visibility,
  WambeProductEventV1NameEnum,
  WambeProductEventV1Properties,
} from "@wambe/api-client";
import { getHostApi, WambeApiError } from "@/lib/api/host-api";
import {
  initialSaveState,
  saveReducer,
} from "@/lib/autosave/state";
import { MediaUploader } from "./MediaUploader";
import { VenuePicker } from "./VenuePicker";
import styles from "./EventEditor.module.css";

const STEPS = ["Basics", "Venue", "Invitation & style", "Privacy & publish"];
const EVENT_TYPES = [
  ["wedding", "Wedding", "Two families, one beautiful beginning"],
  ["birthday", "Birthday", "Another year worth gathering for"],
  ["naming_ceremony", "Naming ceremony", "Welcome a new name with joy"],
  ["anniversary", "Anniversary", "Honour the story so far"],
  ["graduation", "Graduation", "Celebrate the work and the future"],
  ["housewarming", "Housewarming", "Open the doors to your people"],
  ["other", "Other celebration", "Make the moment your own"],
] as const;
const VISIBILITIES = [
  ["public", "Public", "Anyone with the link can view. May be eligible for future discovery."],
  ["private_link", "Private link", "Only people you share the link with can open it."],
  ["invite_only", "Invite-only", "Only invited guests can access it. Guest access setup is required before sharing."],
  ["hidden_location", "Hidden location", "Guests see the venue only after access is approved. Guest access setup is required before sharing."],
] as const;

type Draft = {
  eventType: string;
  title: string;
  startsAt: string;
  venue: Venue;
  dressCodeNotes: string;
  visibility: Visibility | "";
};

const EMPTY_DRAFT: Draft = {
  eventType: "",
  title: "",
  startsAt: "",
  venue: { displayAddress: "", confirmed: false },
  dressCodeNotes: "",
  visibility: "",
};

function localDateTime(date?: Date | null) {
  if (!date) return "";
  const parts = new Intl.DateTimeFormat("sv-SE", {
    timeZone: "Africa/Lagos",
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    hourCycle: "h23",
  }).format(date);
  return parts.replace(" ", "T");
}

function fromLagosWallTime(value: string) {
  return new Date(`${value}:00+01:00`);
}

function fromEvent(event: Event): Draft {
  return {
    eventType: event.eventType ?? "",
    title: event.title ?? "",
    startsAt: localDateTime(event.startsAt),
    venue: event.venue ?? { displayAddress: "", confirmed: false },
    dressCodeNotes: event.dressCodeNotes ?? "",
    visibility: event.visibility ?? "",
  };
}

function toUpdate(draft: Draft): UpdateEventRequest {
  return {
    eventType: (draft.eventType || null) as UpdateEventRequestEventTypeEnum | null,
    title: draft.title.trim() || null,
    startsAt: draft.startsAt ? fromLagosWallTime(draft.startsAt) : null,
    timezone: "Africa/Lagos",
    venue: draft.venue.displayAddress ? draft.venue : null,
    dressCodeNotes: draft.dressCodeNotes.trim() || null,
    visibility: draft.visibility || null,
  };
}

function deviceClass(): "mobile" | "tablet" | "desktop" {
  if (window.innerWidth < 768) return "mobile";
  if (window.innerWidth < 1024) return "tablet";
  return "desktop";
}

function recordMilestone(
  eventId: string,
  sessionId: string | undefined,
  eligibility: "eligible_first_time" | "resumed_draft",
  name: WambeProductEventV1NameEnum,
  properties: WambeProductEventV1Properties,
) {
  if (!sessionId) return;
  void getHostApi()
    .recordMilestone(
      sessionId,
      {
        schemaVersion: "1.0",
        name,
        eventId,
        creationSessionId: sessionId,
        occurredAt: new Date(),
        eligibility,
        deviceClass: deviceClass(),
        networkQuality: "unknown",
        properties,
      },
      crypto.randomUUID(),
    )
    .catch(() => {
      // Product measurement is fail-open and never blocks event creation.
    });
}

export function EventEditor({
  eventId,
  create = false,
}: {
  eventId?: string;
  create?: boolean;
}) {
  const router = useRouter();
  const [event, setEvent] = useState<Event>();
  const [draft, setDraft] = useState<Draft>(EMPTY_DRAFT);
  const [step, setStep] = useState(0);
  const [loading, setLoading] = useState(true);
  const [publishing, setPublishing] = useState(false);
  const [creationSessionId, setCreationSessionId] = useState<string>();
  const [eligibility, setEligibility] = useState<
    "eligible_first_time" | "resumed_draft"
  >("resumed_draft");
  const [errors, setErrors] = useState<Record<string, string[]>>({});
  const [saveState, dispatchSave] = useReducer(saveReducer, initialSaveState);
  const started = useRef(false);
  const editGeneration = useRef(0);
  const persistedGeneration = useRef(0);
  const dirtyFields = useRef<Set<keyof Draft>>(new Set());
  const eventRef = useRef<Event | undefined>(undefined);
  const draftRef = useRef<Draft>(EMPTY_DRAFT);
  const saveInFlight = useRef<Promise<Event | null> | null>(null);
  const publishIdempotencyKey = useRef<string | undefined>(undefined);
  const heading = useRef<HTMLHeadingElement>(null);
  const lastSave = useRef<
    | {
        key: string;
        request: UpdateEventRequest;
        version: number;
      }
    | undefined
  >(undefined);

  useEffect(() => {
    if (started.current) return;
    started.current = true;
    const api = getHostApi();
    if (create) {
      const activeDraft = sessionStorage.getItem("wambe.new.active-draft");
      if (activeDraft) {
        const active = JSON.parse(activeDraft) as { eventId: string };
        router.replace(`/events/${active.eventId}/edit`);
        return;
      }
      const clientKey =
        sessionStorage.getItem("wambe.new.client-key") ?? crypto.randomUUID();
      const idempotencyKey =
        sessionStorage.getItem("wambe.new.idempotency-key") ?? crypto.randomUUID();
      sessionStorage.setItem("wambe.new.client-key", clientKey);
      sessionStorage.setItem("wambe.new.idempotency-key", idempotencyKey);
      api
        .createEvent(
          {
            clientCreationKey: clientKey,
            eligibility: "eligible_first_time",
            deviceClass: deviceClass(),
            networkQuality:
              "connection" in navigator &&
              (navigator as Navigator & { connection?: { effectiveType?: string } })
                .connection?.effectiveType === "2g"
                ? "poor"
                : "unknown",
          },
          idempotencyKey,
        )
        .then((created) => {
          const initialDraft = fromEvent(created.event);
          eventRef.current = created.event;
          draftRef.current = initialDraft;
          setEvent(created.event);
          setDraft(initialDraft);
          setCreationSessionId(created.creationSessionId);
          setEligibility("eligible_first_time");
          sessionStorage.setItem(
            "wambe.new.active-draft",
            JSON.stringify({ eventId: created.event.id }),
          );
          sessionStorage.setItem(
            `wambe.session.${created.event.id}`,
            created.creationSessionId,
          );
          recordMilestone(
            created.event.id,
            created.creationSessionId,
            "eligible_first_time",
            "event_creation_started",
            { step: "basics" },
          );
          router.replace(`/events/${created.event.id}/edit`);
        })
        .catch(() => setErrors({ form: ["We couldn't open a new draft. Try again."] }))
        .finally(() => setLoading(false));
    } else if (eventId) {
      api
        .getEvent(eventId)
        .then((loaded) => {
          const loadedDraft = fromEvent(loaded);
          eventRef.current = loaded;
          draftRef.current = loadedDraft;
          setEvent(loaded);
          setDraft(loadedDraft);
          setCreationSessionId(
            sessionStorage.getItem(`wambe.session.${eventId}`) ?? undefined,
          );
          const activeDraft = sessionStorage.getItem("wambe.new.active-draft");
          if (
            activeDraft &&
            (JSON.parse(activeDraft) as { eventId: string }).eventId === eventId
          ) {
            sessionStorage.removeItem("wambe.new.active-draft");
            sessionStorage.removeItem("wambe.new.client-key");
            sessionStorage.removeItem("wambe.new.idempotency-key");
          }
        })
        .catch(() => setErrors({ form: ["This event is not available."] }))
        .finally(() => setLoading(false));
    }
  }, [create, eventId, router]);

  useEffect(() => {
    const offline = () => dispatchSave({ type: "OFFLINE" });
    const online = () => dispatchSave({ type: "ONLINE" });
    window.addEventListener("offline", offline);
    window.addEventListener("online", online);
    return () => {
      window.removeEventListener("offline", offline);
      window.removeEventListener("online", online);
    };
  }, []);

  useEffect(() => {
    const protect = (browserEvent: BeforeUnloadEvent) => {
      if (["dirty", "saving", "failed", "offline"].includes(saveState.phase)) {
        browserEvent.preventDefault();
      }
    };
    window.addEventListener("beforeunload", protect);
    return () => window.removeEventListener("beforeunload", protect);
  }, [saveState.phase]);

  const saveNow = useCallback((): Promise<Event | null> => {
    const currentEvent = eventRef.current;
    if (!currentEvent || saveState.phase === "offline" || !navigator.onLine) {
      return Promise.resolve(null);
    }
    if (saveInFlight.current) return saveInFlight.current;
    if (
      persistedGeneration.current >= editGeneration.current &&
      (saveState.phase === "idle" || saveState.phase === "saved")
    ) {
      return Promise.resolve(currentEvent);
    }

    const generation = editGeneration.current;
    dispatchSave({ type: "SAVE_START" });
    const operation = lastSave.current ?? {
      key: crypto.randomUUID(),
      request: toUpdate(draftRef.current),
      version: currentEvent.version,
    };
    lastSave.current = operation;

    const task = (async () => {
      try {
        const saved = await getHostApi().updateEvent(
          currentEvent.id,
          operation.version,
          operation.request,
          operation.key,
        );
        eventRef.current = saved;
        setEvent(saved);
        lastSave.current = undefined;
        persistedGeneration.current = generation;
        if (generation === editGeneration.current) {
          const savedDraft = fromEvent(saved);
          draftRef.current = savedDraft;
          dirtyFields.current.clear();
          setDraft(savedDraft);
          dispatchSave({ type: "SAVE_SUCCESS" });
        } else {
          dispatchSave({ type: "CHANGE" });
        }
        recordMilestone(
          saved.id,
          creationSessionId,
          eligibility,
          "draft_save_succeeded",
          { step: (["basics", "venue", "style", "privacy"] as const)[step] },
        );
        return saved;
      } catch (caught) {
        recordMilestone(
          currentEvent.id,
          creationSessionId,
          eligibility,
          "draft_save_failed",
          { step: (["basics", "venue", "style", "privacy"] as const)[step] },
        );
        if (
          caught instanceof WambeApiError &&
          caught.code === "EVENT_VERSION_CONFLICT"
        ) {
          try {
            const latest = await getHostApi().getEvent(currentEvent.id);
            const serverDraft = fromEvent(latest);
            const localDraft = draftRef.current;
            const reconciled = { ...serverDraft };
            dirtyFields.current.forEach((field) => {
              Object.assign(reconciled, { [field]: localDraft[field] });
            });
            eventRef.current = latest;
            draftRef.current = reconciled;
            setEvent(latest);
            setDraft(reconciled);
            lastSave.current = undefined;
            dispatchSave({ type: "CHANGE" });
            setErrors({
              form: [
                "We refreshed a newer server version. Your local edits are still here; choose Retry to save them.",
              ],
            });
          } catch {
            dispatchSave({ type: "SAVE_FAILURE" });
            setErrors({ form: [caught.message] });
          }
        } else {
          dispatchSave({ type: "SAVE_FAILURE" });
        }
        return null;
      } finally {
        saveInFlight.current = null;
      }
    })();
    saveInFlight.current = task;
    return task;
  }, [creationSessionId, eligibility, saveState.phase, step]);

  async function flushLatest(): Promise<Event | null> {
    let saved = await saveNow();
    if (!saved) return null;
    if (persistedGeneration.current < editGeneration.current) {
      saved = await saveNow();
    }
    return saved;
  }

  useEffect(() => {
    if (saveState.phase !== "dirty" || !event) return;
    const timer = setTimeout(() => void saveNow(), 700);
    return () => clearTimeout(timer);
  }, [event, saveNow, saveState.phase]);

  function change(patch: Partial<Draft>) {
    editGeneration.current += 1;
    (Object.keys(patch) as Array<keyof Draft>).forEach((field) =>
      dirtyFields.current.add(field),
    );
    lastSave.current = undefined;
    publishIdempotencyKey.current = undefined;
    setDraft((current) => {
      const next = { ...current, ...patch };
      draftRef.current = next;
      return next;
    });
    dispatchSave({ type: "CHANGE" });
  }

  async function go(next: number) {
    const saved = await flushLatest();
    if (!saved) return;
    setStep(next);
    setTimeout(() => heading.current?.focus(), 0);
  }

  async function publish() {
    const clientErrors: Record<string, string[]> = {};
    if (!draft.eventType) clientErrors.eventType = ["Choose an event type."];
    if (!draft.title.trim()) clientErrors.title = ["Add an event title."];
    if (!draft.startsAt || fromLagosWallTime(draft.startsAt) <= new Date())
      clientErrors.startsAt = ["Choose a future date and time."];
    if (!draft.venue.confirmed) clientErrors.venue = ["Confirm the venue pin."];
    if (!draft.visibility) clientErrors.visibility = ["Choose who can see this event."];
    if (event?.media.some((item) => item.status !== "active"))
      clientErrors.media = ["Wait for media safety checks or remove the file."];
    if (Object.keys(clientErrors).length) {
      setErrors(clientErrors);
      setStep(clientErrors.visibility ? 3 : clientErrors.venue ? 1 : clientErrors.media ? 2 : 0);
      setTimeout(() => document.getElementById("publish-errors")?.focus(), 0);
      return;
    }
    setPublishing(true);
    setErrors({});
    const saved = await flushLatest();
    if (!saved) {
      setPublishing(false);
      return;
    }
    try {
      publishIdempotencyKey.current ??= crypto.randomUUID();
      await getHostApi().publishEvent(
        saved.id,
        saved.version,
        publishIdempotencyKey.current,
      );
      publishIdempotencyKey.current = undefined;
      recordMilestone(
        saved.id,
        creationSessionId,
        eligibility,
        "event_publish_succeeded",
        {
          step: "publish",
          visibility: draft.visibility || undefined,
          hadMediaUpload: saved.media.length > 0,
          lifecycleAction:
            saved.status === "unpublished" ? "republished" : "published",
        },
      );
      router.push(`/events/${saved.id}/published`);
    } catch (caught) {
      const apiError =
        caught instanceof WambeApiError
          ? caught
          : new WambeApiError("Publishing paused. Your draft is safe.");
      setErrors(
        Object.keys(apiError.fieldErrors).length
          ? apiError.fieldErrors
          : { form: [apiError.message] },
      );
      recordMilestone(
        saved.id,
        creationSessionId,
        eligibility,
        "event_publish_failed",
        { step: "publish", outcomeCode: apiError.code },
      );
      setPublishing(false);
      setTimeout(() => document.getElementById("publish-errors")?.focus(), 0);
    }
  }

  if (loading) {
    return (
      <div className={styles.loading} aria-busy="true">
        <span className="spinner" aria-hidden="true" />
        <p>Opening your Wambe…</p>
      </div>
    );
  }

  if (!event) {
    return (
      <div className="alert error" role="alert">
        {errors.form?.[0] ?? "This event is not available."}
      </div>
    );
  }

  const protectedMode =
    draft.visibility === "invite_only" || draft.visibility === "hidden_location";

  return (
    <div className={styles.editor}>
      <header className={styles.editorHeader}>
        <button className="button ghost" onClick={() => router.push("/events")} type="button">
          ← Exit
        </button>
        <div className={styles.saveStatus} data-phase={saveState.phase} aria-live="polite">
          <span aria-hidden="true">●</span> {saveState.message}
          {saveState.phase === "failed" && (
            <button onClick={() => void saveNow()} type="button">Retry</button>
          )}
        </div>
      </header>

      {saveState.phase === "offline" && (
        <div className="alert" role="status">
          You’re offline. Keep going—these changes will save when you reconnect.
        </div>
      )}

      <div className={styles.workspace}>
        <section className={`card ${styles.formCard}`}>
          <div className={styles.stepContext}>
            <div>
              <span>Step {step + 1} of {STEPS.length}</span>
              <strong>{STEPS[step]}</strong>
            </div>
            <div
              aria-label={`Step ${step + 1} of ${STEPS.length}`}
              className={styles.progress}
              role="progressbar"
              aria-valuemax={STEPS.length}
              aria-valuemin={1}
              aria-valuenow={step + 1}
            >
              <span style={{ width: `${((step + 1) / STEPS.length) * 100}%` }} />
            </div>
          </div>

          {Object.keys(errors).length > 0 && (
            <div
              className="alert error"
              id="publish-errors"
              role="alert"
              tabIndex={-1}
            >
              <strong>Review these details</strong>
              <ul>
                {Object.entries(errors).flatMap(([field, messages]) =>
                  messages.map((message) => <li key={`${field}-${message}`}>{message}</li>),
                )}
              </ul>
            </div>
          )}

          <div className={styles.stepBody}>
            <h1 ref={heading} tabIndex={-1}>
              {step === 0 && "Tell us about the celebration."}
              {step === 1 && "Where will everyone gather?"}
              {step === 2 && "Add the invitation and style."}
              {step === 3 && "Who can see this event?"}
            </h1>

            {step === 0 && (
              <div className={styles.fields}>
                <fieldset className={styles.choiceFieldset}>
                  <legend>What are you celebrating?</legend>
                  <div className={styles.choiceGrid}>
                    {EVENT_TYPES.map(([value, label, description]) => (
                      <label className={styles.choice} data-selected={draft.eventType === value} key={value}>
                        <input
                          checked={draft.eventType === value}
                          name="event-type"
                          onChange={() => change({ eventType: value })}
                          type="radio"
                          value={value}
                        />
                        <strong>{label}</strong>
                        <span>{description}</span>
                      </label>
                    ))}
                  </div>
                  {errors.eventType && <span className="field-error">{errors.eventType[0]}</span>}
                </fieldset>
                <div className="field">
                  <label htmlFor="event-title">Event title</label>
                  <input
                    aria-invalid={Boolean(errors.title)}
                    id="event-title"
                    maxLength={120}
                    onChange={(input) => change({ title: input.target.value })}
                    placeholder="Ada & Tunde's Wedding"
                    value={draft.title}
                  />
                  {errors.title && <span className="field-error">{errors.title[0]}</span>}
                </div>
                <div className="field">
                  <label htmlFor="starts-at">Date and time</label>
                  <input
                    aria-invalid={Boolean(errors.startsAt)}
                    id="starts-at"
                    min={localDateTime(new Date())}
                    onChange={(input) => change({ startsAt: input.target.value })}
                    type="datetime-local"
                    value={draft.startsAt}
                  />
                  <span className="field-help">Shown in Africa/Lagos time.</span>
                  {errors.startsAt && <span className="field-error">{errors.startsAt[0]}</span>}
                </div>
              </div>
            )}

            {step === 1 && (
              <VenuePicker
                error={errors.venue?.[0]}
                onChange={(venue) => change({ venue })}
                value={draft.venue}
              />
            )}

            {step === 2 && (
              <div className={styles.fields}>
                <MediaUploader
                  eventId={event.id}
                  media={event.media}
                  onChange={(media) => {
                    if (eventRef.current) {
                      eventRef.current = { ...eventRef.current, media };
                    }
                    setEvent((current) => current ? { ...current, media } : current);
                  }}
                />
                <div className="field">
                  <label htmlFor="dress-code">Dress code or Aso-Ebi notes <span className="muted">(optional)</span></label>
                  <textarea
                    id="dress-code"
                    maxLength={1000}
                    onChange={(input) => change({ dressCodeNotes: input.target.value })}
                    placeholder="Coral and gold. Traditional attire warmly encouraged."
                    value={draft.dressCodeNotes}
                  />
                </div>
              </div>
            )}

            {step === 3 && (
              <div className={styles.fields}>
                <fieldset className={styles.choiceFieldset}>
                  <legend className="sr-only">Choose who can see this event</legend>
                  <div className={styles.visibilityList}>
                    {VISIBILITIES.map(([value, label, description]) => (
                      <label className={styles.visibility} data-selected={draft.visibility === value} key={value}>
                        <input
                          checked={draft.visibility === value}
                          name="visibility"
                          onChange={() => change({ visibility: value as Visibility })}
                          type="radio"
                          value={value}
                        />
                        <span>
                          <strong>{label}</strong>
                          <small>{description}</small>
                        </span>
                      </label>
                    ))}
                  </div>
                  {errors.visibility && <span className="field-error">{errors.visibility[0]}</span>}
                </fieldset>
                {protectedMode && (
                  <div className="alert">
                    <strong>Guest access setup comes next.</strong>
                    <p>You can publish this setting now, but sharing stays off until guest access enforcement is available.</p>
                  </div>
                )}
                <div className={styles.review}>
                  <p className="eyebrow">Ready for a final look</p>
                  <h2>{draft.title || "Your celebration"}</h2>
                  <dl>
                    <div><dt>When</dt><dd>{draft.startsAt ? fromLagosWallTime(draft.startsAt).toLocaleString("en-NG", { dateStyle: "long", timeStyle: "short", timeZone: "Africa/Lagos" }) : "Not added"}</dd></div>
                    <div><dt>Where</dt><dd>{draft.venue.displayAddress || "Not added"}</dd></div>
                    <div><dt>Privacy</dt><dd>{draft.visibility ? draft.visibility.replaceAll("_", " ") : "Not chosen"}</dd></div>
                  </dl>
                </div>
              </div>
            )}
          </div>

          <footer className={styles.actions}>
            {step > 0 ? (
              <button className="button secondary" onClick={() => void go(step - 1)} type="button">Back</button>
            ) : (
              <span />
            )}
            {step < STEPS.length - 1 ? (
              <button className="button" onClick={() => void go(step + 1)} type="button">
                Continue
              </button>
            ) : (
              <button className="button" disabled={publishing} onClick={() => void publish()} type="button">
                {publishing && <span className="spinner" aria-hidden="true" />}
                {publishing ? "Publishing your Wambe…" : "Publish Wambe"}
              </button>
            )}
          </footer>
        </section>

        <aside className={styles.preview} aria-label="Event preview">
          <div className={styles.previewArt}>
            <span className="eyebrow">{draft.eventType?.replaceAll("_", " ") || "Your celebration"}</span>
            <h2>{draft.title || "A beautiful day is coming"}</h2>
            <p>{draft.startsAt ? fromLagosWallTime(draft.startsAt).toLocaleDateString("en-NG", { day: "numeric", month: "long", year: "numeric", timeZone: "Africa/Lagos" }) : "Choose a date"}</p>
            <div className={styles.previewMonogram} aria-hidden="true">W</div>
          </div>
          <div className={styles.previewDetails}>
            <span>Live preview</span>
            <p>{draft.venue.displayAddress || "Your venue will appear here"}</p>
          </div>
        </aside>
      </div>
    </div>
  );
}
