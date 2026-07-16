import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { dirname, isAbsolute, join, resolve } from "node:path";

const scriptDirectory = dirname(fileURLToPath(import.meta.url));
const root = dirname(scriptDirectory);
const checklistPath = join(root, "docs", "operations", "staging-release-checklist.md");
const v1EvidencePath = join(
  root,
  "docs",
  "sdlc",
  "stories",
  "US-003-secure-us-002-staging-release",
  "STAGING_EVIDENCE.example.json",
);
const v11EvidencePath = join(
  root,
  "docs",
  "sdlc",
  "stories",
  "US-004-complete-staging-release-controls",
  "contracts",
  "staging-evidence-v1.1.example.json",
);
const requiredFindingIds = [
  "QA-001",
  "SEC-011",
  "SEC-012",
  "SEC-013",
  "SEC-014",
];
const findingReviewerRoles = new Set(["QA", "Security"]);
const controlReviewerRoles = new Set([
  "Backend",
  "Frontend",
  "QA",
  "Security",
  "Operations",
  "Product Owner",
]);

const checklist = readFileSync(checklistPath, "utf8");
const checklistIds = [...checklist.matchAll(/^- \[[ xX]\] `([A-Z0-9][A-Z0-9._-]{1,63})`/gm)]
  .map((match) => match[1]);

function readJson(path) {
  return JSON.parse(readFileSync(path, "utf8"));
}

function requireArray(value, source) {
  if (!Array.isArray(value)) {
    throw new Error(`${source} must be an array.`);
  }
}

function requireUnique(ids, source, itemName) {
  const duplicates = ids.filter((id, index) => ids.indexOf(id) !== index);
  if (duplicates.length > 0) {
    throw new Error(
      `${source} has duplicate ${itemName}: ${[...new Set(duplicates)].join(", ")}`,
    );
  }
}

function requireExactIds(actualIds, expectedIds, source, itemName) {
  requireUnique(actualIds, source, itemName);

  const missing = expectedIds.filter((id) => !actualIds.includes(id));
  const unexpected = actualIds.filter((id) => !expectedIds.includes(id));
  if (actualIds.length !== expectedIds.length || missing.length > 0 || unexpected.length > 0) {
    throw new Error(
      [
        `${source} ${itemName} mismatch.`,
        `Missing: ${missing.join(", ") || "none"}.`,
        `Unexpected: ${unexpected.join(", ") || "none"}.`,
      ].join(" "),
    );
  }
}

function requireControlMapping(evidence, source) {
  requireArray(evidence.controls, `${source} controls`);
  requireExactIds(
    evidence.controls.map((control) => control.controlId),
    checklistIds,
    source,
    "control IDs",
  );
}

function requirePlaceholderCommit(evidence, source) {
  if (!/^0{40,64}$/.test(evidence.release?.commit ?? "")) {
    throw new Error(`${source} release commit must remain an all-zero placeholder.`);
  }
}

function assertV1Template(evidence) {
  const source = "US-003 staging evidence v1.0 example";
  requireControlMapping(evidence, source);
  if (!evidence.controls.every((control) => control.status === "NOT_RUN")) {
    throw new Error(`${source} must contain only NOT_RUN controls.`);
  }
  requirePlaceholderCommit(evidence, source);
}

function assertV11Identity(evidence, source) {
  if (
    evidence.schemaVersion !== "1.1"
    || evidence.storyId !== "US-004"
    || evidence.environment !== "staging"
    || evidence.release?.repository !== "yabdev/wambe"
  ) {
    throw new Error(`${source} does not have the required US-004 staging release identity.`);
  }
}

function assertV11FindingSet(evidence, source) {
  requireArray(evidence.findings, `${source} findings`);
  requireExactIds(
    evidence.findings.map((finding) => finding.findingId),
    requiredFindingIds,
    source,
    "finding IDs",
  );
}

function requireEvidenceMetadata(item, source) {
  if (
    typeof item.timestampUtc !== "string"
    || Number.isNaN(Date.parse(item.timestampUtc))
    || typeof item.reviewerRole !== "string"
    || item.reviewerRole.length === 0
    || typeof item.reviewerReference !== "string"
    || item.reviewerReference.length === 0
    || !Array.isArray(item.sourceReferences)
    || item.sourceReferences.length === 0
    || typeof item.outcome !== "string"
    || item.outcome.length === 0
  ) {
    throw new Error(`${source} is missing required review evidence metadata.`);
  }
}

function assertFinalReadiness(evidence) {
  const source = "US-004 final staging evidence";
  assertV11Identity(evidence, source);
  assertV11FindingSet(evidence, source);
  requireControlMapping(evidence, source);

  if (!/^[a-f0-9]{40,64}$/.test(evidence.release.commit) || /^0+$/.test(evidence.release.commit)) {
    throw new Error(`${source} must identify a non-placeholder lowercase hexadecimal commit.`);
  }

  for (const finding of evidence.findings) {
    if (finding.status !== "CLOSED") {
      throw new Error(`${source} finding ${finding.findingId} is not CLOSED.`);
    }
    requireEvidenceMetadata(finding, `${source} finding ${finding.findingId}`);
    if (!findingReviewerRoles.has(finding.reviewerRole)) {
      throw new Error(
        `${source} finding ${finding.findingId} has invalid reviewer role ${finding.reviewerRole}.`,
      );
    }
  }

  let passCount = 0;
  let waivedCount = 0;
  for (const control of evidence.controls) {
    if (control.status === "PASS") {
      passCount += 1;
    } else if (control.status === "WAIVED") {
      waivedCount += 1;
      if (
        typeof control.decisionReference !== "string"
        || control.decisionReference.length === 0
      ) {
        throw new Error(
          `${source} waived control ${control.controlId} has no Product Owner decision reference.`,
        );
      }
    } else {
      throw new Error(
        `${source} control ${control.controlId} has non-final status ${control.status}.`,
      );
    }
    requireEvidenceMetadata(control, `${source} control ${control.controlId}`);
    if (!controlReviewerRoles.has(control.reviewerRole)) {
      throw new Error(
        `${source} control ${control.controlId} has invalid reviewer role ${control.reviewerRole}.`,
      );
    }
  }

  return {
    closedFindings: evidence.findings.length,
    passCount,
    waivedCount,
  };
}

function assertV11Template(evidence) {
  const source = "US-004 staging evidence v1.1 example";
  assertV11Identity(evidence, source);
  assertV11FindingSet(evidence, source);
  requireControlMapping(evidence, source);

  if (!evidence.findings.every((finding) => finding.status === "OPEN")) {
    throw new Error(`${source} must contain only OPEN findings.`);
  }
  if (!evidence.controls.every((control) => control.status === "NOT_RUN")) {
    throw new Error(`${source} must contain only NOT_RUN controls.`);
  }
  requirePlaceholderCommit(evidence, source);

  try {
    assertFinalReadiness(evidence);
  } catch {
    return;
  }
  throw new Error(`${source} must be rejected as final readiness evidence.`);
}

function syntheticFinalEvidence(template) {
  const timestampUtc = "2026-07-16T00:00:00Z";
  const evidence = structuredClone(template);
  evidence.release.commit = "1".repeat(40);
  evidence.findings = evidence.findings.map((finding) => ({
    ...finding,
    status: "CLOSED",
    timestampUtc,
    reviewerRole: "Security",
    reviewerReference: "synthetic-policy-self-test",
    sourceReferences: ["synthetic://finding-evidence"],
    outcome: "Synthetic policy self-test only.",
  }));
  evidence.controls = evidence.controls.map((control) => ({
    ...control,
    status: "PASS",
    timestampUtc,
    reviewerRole: "Operations",
    reviewerReference: "synthetic-policy-self-test",
    sourceReferences: ["synthetic://control-evidence"],
    outcome: "Synthetic policy self-test only.",
  }));
  return evidence;
}

function finalEvidenceArgument() {
  const finalIndex = process.argv.indexOf("--final");
  if (finalIndex === -1) {
    return null;
  }
  const value = process.argv[finalIndex + 1];
  if (!value) {
    throw new Error("--final requires a path to a US-004 staging evidence JSON file.");
  }
  return isAbsolute(value) ? value : resolve(root, value);
}

requireUnique(checklistIds, "Staging checklist", "control IDs");
if (checklistIds.length !== 42) {
  throw new Error(`Staging checklist must contain exactly 42 controls, found ${checklistIds.length}.`);
}

const v1Evidence = readJson(v1EvidencePath);
const v11Evidence = readJson(v11EvidencePath);
assertV1Template(v1Evidence);
assertV11Template(v11Evidence);
assertFinalReadiness(syntheticFinalEvidence(v11Evidence));

const finalPath = finalEvidenceArgument();
if (finalPath) {
  const summary = assertFinalReadiness(readJson(finalPath));
  console.log(
    `US-004 final evidence is ready: ${summary.closedFindings}/5 findings closed, `
      + `${summary.passCount} controls passed, ${summary.waivedCount} controls waived.`,
  );
} else {
  console.log(
    `Staging evidence templates map ${checklistIds.length} unique controls; `
      + "the US-004 example is valid as a template and rejected as final evidence.",
  );
}
