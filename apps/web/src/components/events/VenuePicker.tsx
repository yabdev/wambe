"use client";

import { useEffect, useState } from "react";
import {
  APIProvider,
  AdvancedMarker,
  Map,
  useMap,
  useMapsLibrary,
  type MapMouseEvent,
} from "@vis.gl/react-google-maps";
import type { Venue } from "@wambe/api-client";
import { isDemoMode } from "@/lib/auth/client";
import styles from "./VenuePicker.module.css";

const LAGOS = { lat: 6.5244, lng: 3.3792 };

function MapCamera({ position }: { position: { lat: number; lng: number } }) {
  const map = useMap();
  useEffect(() => {
    map?.panTo(position);
  }, [map, position]);
  return null;
}

function FindAddressButton({
  address,
  onFound,
}: {
  address: string;
  onFound: (venue: Venue) => void;
}) {
  const geocoding = useMapsLibrary("geocoding");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string>();

  async function findAddress() {
    if (!geocoding || !address.trim()) return;
    setBusy(true);
    setError(undefined);
    try {
      const response = await new geocoding.Geocoder().geocode({ address });
      const result = response.results[0];
      if (!result) {
        setError("We couldn't find that address. Add more detail and try again.");
        return;
      }
      onFound({
        displayAddress: result.formatted_address,
        placeId: result.place_id,
        latitude: result.geometry.location.lat(),
        longitude: result.geometry.location.lng(),
        confirmed: false,
      });
    } catch {
      setError("Map search is unavailable right now. Please try again.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className={styles.findAddress}>
      <button
        className="button secondary"
        disabled={!geocoding || !address.trim() || busy}
        onClick={() => void findAddress()}
        type="button"
      >
        {busy ? "Finding venue…" : "Find address on map"}
      </button>
      {error && <span className="field-error" role="alert">{error}</span>}
    </div>
  );
}

export function VenuePicker({
  value,
  onChange,
  error,
}: {
  value: Venue;
  onChange: (venue: Venue) => void;
  error?: string;
}) {
  const apiKey = process.env.NEXT_PUBLIC_GOOGLE_MAPS_API_KEY;
  const pinPosition =
    value.latitude != null && value.longitude != null
      ? { lat: value.latitude, lng: value.longitude }
      : null;
  const mapPosition = pinPosition ?? LAGOS;

  function choosePosition(event: MapMouseEvent) {
    if (!event.detail.latLng) return;
    onChange({
      ...value,
      latitude: event.detail.latLng.lat,
      longitude: event.detail.latLng.lng,
      confirmed: false,
    });
  }

  return (
    <div className={styles.wrapper}>
      <div className="field">
        <label htmlFor="venue-address">Venue address</label>
        <input
          aria-describedby="venue-help venue-error"
          aria-invalid={Boolean(error)}
          id="venue-address"
          onChange={(event) =>
            onChange({
              ...value,
              displayAddress: event.target.value,
              placeId: null,
              latitude: null,
              longitude: null,
              confirmed: false,
            })
          }
          placeholder="Start typing an address"
          value={value.displayAddress}
        />
        <span className="field-help" id="venue-help">
          Add a clear address, then place and confirm the map pin.
        </span>
        {error && <span className="field-error" id="venue-error">{error}</span>}
      </div>
      {apiKey ? (
        <APIProvider apiKey={apiKey}>
          <FindAddressButton address={value.displayAddress} onFound={onChange} />
          <div className={styles.map} aria-label="Venue pin map">
            <Map
              defaultCenter={mapPosition}
              defaultZoom={13}
              disableDefaultUI
              mapId="wambe-venue-map"
              onClick={choosePosition}
            >
              {pinPosition && (
                <>
                  <MapCamera position={pinPosition} />
                  <AdvancedMarker
                    draggable
                    onDragEnd={(event) => {
                      const lat = event.latLng?.lat();
                      const lng = event.latLng?.lng();
                      if (lat != null && lng != null) {
                        onChange({
                          ...value,
                          latitude: lat,
                          longitude: lng,
                          confirmed: false,
                        });
                      }
                    }}
                    position={pinPosition}
                  />
                </>
              )}
            </Map>
          </div>
        </APIProvider>
      ) : (
        <>
          <div className={styles.map} aria-label="Venue map preview">
          <div className={styles.mapFallback}>
            <span className={styles.pin} aria-hidden="true">●</span>
            <strong>Map preview</strong>
            <span>
              {isDemoMode
                ? "Demo mode can place a sample pin for browser testing."
                : "Google Maps appears when its browser key is configured."}
            </span>
          </div>
          </div>
          {isDemoMode && (
            <button
              className="button secondary"
              disabled={!value.displayAddress.trim()}
              onClick={() =>
                onChange({
                  ...value,
                  latitude: LAGOS.lat,
                  longitude: LAGOS.lng,
                  confirmed: false,
                })
              }
              type="button"
            >
              Place sample pin
            </button>
          )}
        </>
      )}
      {pinPosition && (
        <fieldset className={styles.pinControls}>
          <legend>Fine-tune the pin</legend>
          <p className="field-help">Move the pin if the map marker needs a small adjustment.</p>
          <div aria-label="Pin movement controls">
            {[
              ["North", 0.0003, 0],
              ["West", 0, -0.0003],
              ["East", 0, 0.0003],
              ["South", -0.0003, 0],
            ].map(([label, latitudeChange, longitudeChange]) => (
              <button
                aria-label={`Move pin ${label.toString().toLowerCase()}`}
                className={styles.nudge}
                key={label}
                onClick={() =>
                  onChange({
                    ...value,
                    latitude: pinPosition.lat + Number(latitudeChange),
                    longitude: pinPosition.lng + Number(longitudeChange),
                    confirmed: false,
                  })
                }
                type="button"
              >
                {label}
              </button>
            ))}
          </div>
        </fieldset>
      )}
      <div className={styles.confirmRow}>
        <div>
          <strong>
            {value.displayAddress || "Choose the celebration venue"}
          </strong>
          <span aria-live="polite" className="muted">
            {value.confirmed
              ? "Pin confirmed"
              : pinPosition
                ? "Pin placed — confirm to continue"
                : "Find the address or tap the map to place the pin"}
          </span>
        </div>
        <button
          className={value.confirmed ? "button secondary" : "button"}
          disabled={
            !value.displayAddress.trim() ||
            value.latitude == null ||
            value.longitude == null ||
            !Number.isFinite(value.latitude) ||
            !Number.isFinite(value.longitude)
          }
          onClick={() =>
            onChange({
              ...value,
              confirmed: true,
            })
          }
          type="button"
        >
          {value.confirmed ? "✓ Pin confirmed" : "Confirm this pin"}
        </button>
      </div>
    </div>
  );
}
