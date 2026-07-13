"use client";

import {
  Configuration,
  DefaultApi,
  ResponseError,
  type CreateEventRequest,
  type CreateMediaIntent201Response,
  type CreateMediaIntentRequest,
  type Event,
  type EventWithSession,
  type Media,
  type PublishEvent200Response,
  type UpdateEventRequest,
  type WambeProductEventV1,
} from "@wambe/api-client";
import { getBrowserSupabase, isDemoMode } from "@/lib/auth/client";

type ErrorBody = {
  error?: {
    code?: string;
    message?: string;
    fieldErrors?: Record<string, string[]>;
    requestId?: string;
  };
};

export class WambeApiError extends Error {
  constructor(
    message: string,
    readonly code = "REQUEST_FAILED",
    readonly status = 0,
    readonly fieldErrors: Record<string, string[]> = {},
    readonly requestId?: string,
  ) {
    super(message);
  }
}

export interface HostApi {
  listEvents(): Promise<Event[]>;
  createEvent(request: CreateEventRequest, key: string): Promise<EventWithSession>;
  getEvent(eventId: string): Promise<Event>;
  updateEvent(
    eventId: string,
    version: number,
    request: UpdateEventRequest,
    key: string,
  ): Promise<Event>;
  publishEvent(
    eventId: string,
    version: number,
    key: string,
  ): Promise<PublishEvent200Response>;
  unpublishEvent(eventId: string, version: number, key: string): Promise<Event>;
  deleteEvent(eventId: string, key: string): Promise<void>;
  createMediaIntent(
    eventId: string,
    request: CreateMediaIntentRequest,
    key: string,
  ): Promise<CreateMediaIntent201Response>;
  completeMediaUpload(eventId: string, mediaId: string, key: string): Promise<Media>;
  listMedia(eventId: string): Promise<Media[]>;
  deleteMedia(eventId: string, mediaId: string, key: string): Promise<void>;
  recordMilestone(
    sessionId: string,
    productEvent: WambeProductEventV1,
    key: string,
  ): Promise<void>;
}

async function normalizeError(error: unknown): Promise<never> {
  if (error instanceof WambeApiError) {
    throw error;
  }
  if (error instanceof ResponseError) {
    let body: ErrorBody = {};
    try {
      body = (await error.response.clone().json()) as ErrorBody;
    } catch {
      // Keep the neutral fallback below.
    }
    throw new WambeApiError(
      body.error?.message ?? "Wambe could not complete that request.",
      body.error?.code,
      error.response.status,
      body.error?.fieldErrors,
      body.error?.requestId,
    );
  }
  throw new WambeApiError(
    error instanceof Error ? error.message : "Wambe could not complete that request.",
  );
}

function realApi(): HostApi {
  let supabase: ReturnType<typeof getBrowserSupabase> = null;
  try {
    supabase = getBrowserSupabase();
  } catch {
    // Keep the API fail-closed; operations return a configuration error below.
  }
  const generated = new DefaultApi(
    new Configuration({
      basePath:
        process.env.NEXT_PUBLIC_WAMBE_API_URL ?? "http://localhost:8080/api/v1",
      accessToken: async () => {
        const session = await supabase?.auth.getSession();
        const token = session?.data.session?.access_token;
        if (!token) {
          throw new WambeApiError(
            supabase
              ? "Please sign in again."
              : "Authentication is not configured for this environment.",
            supabase ? "UNAUTHENTICATED" : "AUTH_CONFIGURATION_MISSING",
            supabase ? 401 : 503,
          );
        }
        return token;
      },
    }),
  );

  async function run<T>(operation: () => Promise<T>, allowRefresh = true): Promise<T> {
    try {
      return await operation();
    } catch (error) {
      if (
        allowRefresh &&
        error instanceof ResponseError &&
        error.response.status === 401 &&
        supabase
      ) {
        await supabase.auth.refreshSession();
        return run(operation, false);
      }
      return normalizeError(error);
    }
  }

  return {
    listEvents: () => run(async () => (await generated.listEvents({ limit: 50 })).items),
    createEvent: (request, key) =>
      run(() =>
        generated.createEvent({
          createEventRequest: request,
          idempotencyKey: key,
        }),
      ),
    getEvent: (eventId) => run(() => generated.getEvent({ eventId })),
    updateEvent: (eventId, version, request, key) =>
      run(() =>
        generated.updateEvent({
          eventId,
          ifMatch: String(version),
          idempotencyKey: key,
          updateEventRequest: request,
        }),
      ),
    publishEvent: (eventId, version, key) =>
      run(() =>
        generated.publishEvent({
          eventId,
          ifMatch: String(version),
          idempotencyKey: key,
        }),
      ),
    unpublishEvent: (eventId, version, key) =>
      run(() =>
        generated.unpublishEvent({
          eventId,
          ifMatch: String(version),
          idempotencyKey: key,
        }),
      ),
    deleteEvent: (eventId, key) =>
      run(() => generated.deleteEvent({ eventId, idempotencyKey: key })),
    createMediaIntent: (eventId, request, key) =>
      run(() =>
        generated.createMediaIntent({
          eventId,
          idempotencyKey: key,
          createMediaIntentRequest: request,
        }),
      ),
    completeMediaUpload: (eventId, mediaId, key) =>
      run(() =>
        generated.completeMediaUpload({
          eventId,
          mediaId,
          idempotencyKey: key,
        }),
      ),
    listMedia: (eventId) =>
      run(async () => (await generated.listMedia({ eventId })).items),
    deleteMedia: (eventId, mediaId, key) =>
      run(() =>
        generated.deleteMedia({ eventId, mediaId, idempotencyKey: key }),
      ),
    recordMilestone: (sessionId, productEvent, key) =>
      run(() =>
        generated.recordCreationMilestone({
          sessionId,
          idempotencyKey: key,
          wambeProductEventV1: productEvent,
        }),
      ),
  };
}

const DEMO_EVENTS_KEY = "wambe.demo.events";

type StoredEvent = Omit<
  Event,
  "startsAt" | "publishedAt" | "lastSavedAt" | "createdAt" | "updatedAt" | "media"
> & {
  startsAt?: string | null;
  publishedAt?: string | null;
  lastSavedAt?: string | null;
  createdAt: string;
  updatedAt: string;
  media: Array<
    Omit<Media, "createdAt" | "updatedAt"> & {
      createdAt: string;
      updatedAt: string;
    }
  >;
};

function hydrate(value: StoredEvent): Event {
  return {
    ...value,
    startsAt: value.startsAt ? new Date(value.startsAt) : undefined,
    publishedAt: value.publishedAt ? new Date(value.publishedAt) : undefined,
    lastSavedAt: value.lastSavedAt ? new Date(value.lastSavedAt) : undefined,
    createdAt: new Date(value.createdAt),
    updatedAt: new Date(value.updatedAt),
    media: value.media.map((item) => ({
      ...item,
      createdAt: new Date(item.createdAt),
      updatedAt: new Date(item.updatedAt),
    })),
  };
}

function readDemoEvents(): Event[] {
  if (typeof window === "undefined") return [];
  const raw = window.localStorage.getItem(DEMO_EVENTS_KEY);
  return raw ? (JSON.parse(raw) as StoredEvent[]).map(hydrate) : [];
}

function writeDemoEvents(events: Event[]) {
  window.localStorage.setItem(DEMO_EVENTS_KEY, JSON.stringify(events));
}

function demoApi(): HostApi {
  const updateStored = (event: Event) => {
    const events = readDemoEvents();
    const index = events.findIndex((item) => item.id === event.id);
    if (index >= 0) events[index] = event;
    else events.unshift(event);
    writeDemoEvents(events);
    return event;
  };

  const requireEvent = (eventId: string) => {
    const event = readDemoEvents().find((item) => item.id === eventId);
    if (!event) {
      throw new WambeApiError("This event is not available.", "RESOURCE_NOT_FOUND", 404);
    }
    return event;
  };

  return {
    listEvents: async () => readDemoEvents(),
    createEvent: async () => {
      const now = new Date();
      const event: Event = {
        id: crypto.randomUUID(),
        ownerId: "00000000-0000-0000-0000-000000000001",
        status: "draft",
        version: 1,
        timezone: "Africa/Lagos",
        shareEligible: false,
        createdAt: now,
        updatedAt: now,
        lastSavedAt: now,
        media: [],
      };
      updateStored(event);
      return { event, creationSessionId: crypto.randomUUID() };
    },
    getEvent: async (eventId) => requireEvent(eventId),
    updateEvent: async (eventId, version, request) => {
      const current = requireEvent(eventId);
      if (current.version !== version) {
        throw new WambeApiError(
          "This draft changed in another tab. Reload it before saving.",
          "EVENT_VERSION_CONFLICT",
          409,
        );
      }
      const now = new Date();
      return updateStored({
        ...current,
        ...request,
        version: current.version + 1,
        lastSavedAt: now,
        updatedAt: now,
      });
    },
    publishEvent: async (eventId, version) => {
      const current = requireEvent(eventId);
      const errors: Record<string, string[]> = {};
      if (!current.title) errors.title = ["Add an event title."];
      if (!current.eventType) errors.eventType = ["Choose an event type."];
      if (!current.startsAt || current.startsAt <= new Date())
        errors.startsAt = ["Choose a future date and time."];
      if (
        !current.venue?.confirmed ||
        current.venue.latitude == null ||
        current.venue.longitude == null
      )
        errors.venue = ["Confirm the venue pin."];
      if (!current.visibility) errors.visibility = ["Choose who can see this event."];
      if (Object.keys(errors).length) {
        throw new WambeApiError(
          "Review the highlighted details before publishing.",
          "PUBLICATION_VALIDATION_FAILED",
          422,
          errors,
        );
      }
      const title = current.title ?? "Wambe celebration";
      const slug =
        current.slug ??
        `${title.toLowerCase().replace(/[^a-z0-9]+/g, "-").replace(/^-|-$/g, "")}-${current.id.slice(0, 6)}`;
      const protectedMode =
        current.visibility === "invite_only" ||
        current.visibility === "hidden_location";
      const publishedAt = new Date();
      const event = updateStored({
        ...current,
        status: "published",
        version: version + 1,
        slug,
        canonicalUrl: `${window.location.origin}/e/${slug}`,
        shareEligible: !protectedMode,
        shareBlockedReason: protectedMode
          ? "Guest access setup is required before sharing."
          : undefined,
        publishedAt,
        updatedAt: publishedAt,
      });
      return {
        event,
        canonicalUrl: event.canonicalUrl!,
        shareEligible: event.shareEligible,
        shareBlockedReason: event.shareBlockedReason,
      };
    },
    unpublishEvent: async (eventId, version) => {
      const current = requireEvent(eventId);
      return updateStored({
        ...current,
        status: "unpublished",
        version: version + 1,
        shareEligible: false,
        updatedAt: new Date(),
      });
    },
    deleteEvent: async (eventId) => {
      writeDemoEvents(readDemoEvents().filter((event) => event.id !== eventId));
    },
    createMediaIntent: async (eventId, request) => {
      const current = requireEvent(eventId);
      const now = new Date();
      const media: Media = {
        id: crypto.randomUUID(),
        eventId,
        role: request.role,
        filename: request.filename,
        claimedMimeType: request.claimedMimeType,
        sizeBytes: request.sizeBytes,
        status: "quarantine",
        createdAt: now,
        updatedAt: now,
      };
      updateStored({ ...current, media: [...current.media, media] });
      return {
        media,
        uploadUrl: `demo://${media.id}`,
        expiresAt: new Date(Date.now() + 10 * 60_000),
      };
    },
    completeMediaUpload: async (eventId, mediaId) => {
      const current = requireEvent(eventId);
      let completed: Media | undefined;
      const media = current.media.map((item) => {
        if (item.id !== mediaId) return item;
        completed = { ...item, status: "active", updatedAt: new Date() };
        return completed;
      });
      updateStored({ ...current, media });
      if (!completed) throw new WambeApiError("Upload not found.", "RESOURCE_NOT_FOUND", 404);
      return completed;
    },
    listMedia: async (eventId) => requireEvent(eventId).media,
    deleteMedia: async (eventId, mediaId) => {
      const current = requireEvent(eventId);
      updateStored({
        ...current,
        media: current.media.filter((item) => item.id !== mediaId),
      });
    },
    recordMilestone: async () => undefined,
  };
}

let instance: HostApi | undefined;

export function getHostApi(): HostApi {
  instance ??= isDemoMode ? demoApi() : realApi();
  return instance;
}
