const LOCAL_HOSTS = new Set(["localhost", "127.0.0.1", "::1", "[::1]"]);

export class SiteOriginConfigurationError extends Error {
  readonly code = "WAMBE_SITE_ORIGIN_MISCONFIGURED";

  constructor() {
    super("NEXT_PUBLIC_SITE_URL must be configured as the exact site origin.");
    this.name = "SiteOriginConfigurationError";
  }
}

export function getClientSiteOrigin(): string {
  const configured = configuredSiteOrigin();
  if (configured) {
    if (configured !== window.location.origin) {
      throw new SiteOriginConfigurationError();
    }
    return configured;
  }
  return localDemoOrigin(window.location.origin);
}

export function getServerSiteOrigin(localRequestOrigin?: string): string {
  const configured = configuredSiteOrigin();
  if (configured) {
    return configured;
  }
  if (!localRequestOrigin) {
    throw new SiteOriginConfigurationError();
  }
  return localDemoOrigin(localRequestOrigin);
}

function configuredSiteOrigin(): string | undefined {
  const raw = process.env.NEXT_PUBLIC_SITE_URL;
  if (!raw) {
    if (process.env.NEXT_PUBLIC_DEMO_MODE === "true") {
      return undefined;
    }
    throw new SiteOriginConfigurationError();
  }

  const demoMode = process.env.NEXT_PUBLIC_DEMO_MODE === "true";
  const origin = exactOrigin(raw, demoMode);
  if (
    !demoMode &&
    !origin.startsWith("https://")
  ) {
    throw new SiteOriginConfigurationError();
  }
  if (demoMode && origin.startsWith("http://")) {
    const parsed = new URL(origin);
    if (!LOCAL_HOSTS.has(parsed.hostname)) {
      throw new SiteOriginConfigurationError();
    }
  }
  return origin;
}

function localDemoOrigin(raw: string): string {
  if (process.env.NEXT_PUBLIC_DEMO_MODE !== "true") {
    throw new SiteOriginConfigurationError();
  }
  const origin = exactOrigin(raw, true);
  const parsed = new URL(origin);
  if (!LOCAL_HOSTS.has(parsed.hostname)) {
    throw new SiteOriginConfigurationError();
  }
  return origin;
}

function exactOrigin(raw: string, allowNonDefaultPort = false): string {
  let parsed: URL;
  try {
    parsed = new URL(raw);
  } catch {
    throw new SiteOriginConfigurationError();
  }

  if (
    (parsed.protocol !== "https:" && parsed.protocol !== "http:") ||
    parsed.username ||
    parsed.password ||
    parsed.pathname !== "/" ||
    parsed.search ||
    parsed.hash ||
    (!allowNonDefaultPort && parsed.port)
  ) {
    throw new SiteOriginConfigurationError();
  }
  return parsed.origin;
}
