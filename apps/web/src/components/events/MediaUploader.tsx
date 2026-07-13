"use client";

import { useRef, useState } from "react";
import type {
  CreateMediaIntentRequestClaimedMimeTypeEnum,
  Media,
  MediaRole,
} from "@wambe/api-client";
import { getHostApi } from "@/lib/api/host-api";
import styles from "./MediaUploader.module.css";

const ACCEPTED = [
  "image/jpeg",
  "image/png",
  "image/webp",
  "application/pdf",
] as const;
const MAX_BYTES = 10 * 1024 * 1024;

export function MediaUploader({
  eventId,
  media,
  onChange,
}: {
  eventId: string;
  media: Media[];
  onChange: (media: Media[]) => void;
}) {
  const [busyRole, setBusyRole] = useState<MediaRole>();
  const [error, setError] = useState<string>();
  const pollTimer = useRef<ReturnType<typeof setTimeout> | undefined>(undefined);

  async function poll() {
    const items = await getHostApi().listMedia(eventId);
    onChange(items);
    if (items.some((item) => item.status === "quarantine" || item.status === "scanning")) {
      pollTimer.current = setTimeout(poll, 2500);
    }
  }

  async function upload(file: File, role: MediaRole) {
    setError(undefined);
    if (!ACCEPTED.includes(file.type as (typeof ACCEPTED)[number])) {
      setError("Choose a JPG, PNG, WebP or PDF file.");
      return;
    }
    if (file.size > MAX_BYTES) {
      setError("That file is larger than 10 MB.");
      return;
    }
    setBusyRole(role);
    try {
      const api = getHostApi();
      const intent = await api.createMediaIntent(
        eventId,
        {
          role,
          filename: file.name,
          claimedMimeType: file.type as CreateMediaIntentRequestClaimedMimeTypeEnum,
          sizeBytes: file.size,
        },
        crypto.randomUUID(),
      );
      onChange(await api.listMedia(eventId));
      if (!intent.uploadUrl.startsWith("demo://")) {
        const response = await fetch(intent.uploadUrl, {
          method: "PUT",
          body: file,
          headers: { "Content-Type": file.type },
          signal: AbortSignal.timeout(120_000),
        });
        if (!response.ok) throw new Error("The file transfer did not finish.");
      }
      await api.completeMediaUpload(
        eventId,
        intent.media.id,
        crypto.randomUUID(),
      );
      onChange(await api.listMedia(eventId));
      await poll();
    } catch (caught) {
      setError(
        caught instanceof Error
          ? caught.message
          : "We couldn't upload that file. Your draft is safe.",
      );
    } finally {
      setBusyRole(undefined);
    }
  }

  async function remove(item: Media) {
    setError(undefined);
    try {
      const api = getHostApi();
      await api.deleteMedia(eventId, item.id, crypto.randomUUID());
      onChange(await api.listMedia(eventId));
    } catch (caught) {
      setError(
        caught instanceof Error
          ? caught.message
          : "We couldn't remove that file. Please try again.",
      );
    }
  }

  return (
    <div className={styles.wrapper}>
      <p className="muted">
        Add artwork now or skip for later. JPG, PNG, WebP or PDF · up to 10 MB.
      </p>
      <div className={styles.zones}>
        {(["invitation", "aso_ebi"] as MediaRole[]).map((role) => {
          const item = media.find((candidate) => candidate.role === role);
          return (
            <div className={styles.zone} key={role}>
              <div>
                <span className="eyebrow">
                  {role === "invitation" ? "Invitation" : "Aso-Ebi"}
                </span>
                <strong>
                  {item?.filename ??
                    (role === "invitation"
                      ? "Drop your invitation here"
                      : "Add your Aso-Ebi guide")}
                </strong>
                {item && (
                  <span className={styles.status} data-status={item.status}>
                    {item.status === "active"
                      ? "Ready"
                      : item.status === "rejected"
                        ? `Needs attention · ${item.rejectionCode ?? "scan failed"}`
                        : "Scanning safely…"}
                  </span>
                )}
              </div>
              {item ? (
                <button className="button secondary" onClick={() => remove(item)} type="button">
                  Remove
                </button>
              ) : (
                <label className="button secondary">
                  {busyRole === role ? "Uploading…" : "Choose file"}
                  <input
                    accept={ACCEPTED.join(",")}
                    className="sr-only"
                    disabled={Boolean(busyRole)}
                    onChange={(event) => {
                      const file = event.target.files?.[0];
                      if (file) void upload(file, role);
                    }}
                    type="file"
                  />
                </label>
              )}
            </div>
          );
        })}
      </div>
      {error && <div className="alert error" role="alert">{error}</div>}
      <div aria-live="polite" className="sr-only">
        {busyRole ? `Uploading ${busyRole}` : ""}
      </div>
    </div>
  );
}
