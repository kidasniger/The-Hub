const { cert, initializeApp } = require("firebase-admin/app");
const { getFirestore } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");
const fs = require("fs");

function parseSemver(value) {
  const normalized = String(value || "").replace(/^v/i, "").trim();
  const match = normalized.match(/^(\\d+)\\.(\\d+)\\.(\\d+)$/);
  if (!match) {
    throw new Error("Version invalide: " + value);
  }
  return match.slice(1).map(Number);
}

function compareVersions(a, b) {
  for (let index = 0; index < 3; index += 1) {
    if (a[index] !== b[index]) return a[index] - b[index];
  }
  return 0;
}

function ownerUidFromTokenDoc(doc) {
  const userDocument = doc.ref.parent.parent;
  return userDocument ? userDocument.id : "";
}

async function run() {
  const credentialsPath = process.env.GOOGLE_APPLICATION_CREDENTIALS;
  if (!credentialsPath) {
    throw new Error("GOOGLE_APPLICATION_CREDENTIALS est manquant.");
  }

  const releaseTag = process.env.RELEASE_TAG || "";
  const releaseName = process.env.RELEASE_NAME || "";
  const releaseUrl = process.env.RELEASE_URL || "";
  const releaseVersion = releaseTag.replace(/^v/i, "");

  const releaseSemver = parseSemver(releaseVersion);

  const credentials = JSON.parse(fs.readFileSync(credentialsPath, "utf8"));
  const app = initializeApp({
    credential: cert(credentials),
    projectId: "the-hub-f95f4",
  });

  const db = getFirestore(app);
  const messaging = getMessaging(app);

  const tokenSnapshot = await db.collectionGroup("fcmTokens").get();

  const candidates = [];
  const seenTokens = new Set();
  let skippedUpToDate = 0;
  let skippedMalformed = 0;

  for (const doc of tokenSnapshot.docs) {
    const token = doc.get("token");
    const platform = doc.get("platform");
    const userId = ownerUidFromTokenDoc(doc);
    const installedVersion = doc.get("appVersion");

    if (
      typeof token !== "string" ||
      !token.trim() ||
      platform !== "android" ||
      !userId
    ) {
      skippedMalformed += 1;
      continue;
    }

    if (seenTokens.has(token)) {
      continue;
    }
    seenTokens.add(token);

    if (typeof installedVersion === "string" && installedVersion.trim()) {
      try {
        if (compareVersions(parseSemver(installedVersion), releaseSemver) >= 0) {
          skippedUpToDate += 1;
          continue;
        }
      } catch (_) {
        // Unknown legacy version format: notify so the device can self-check.
      }
    }

    candidates.push({
      token,
      userId,
      doc,
    });
  }

  const title = "Nouvelle mise à jour disponible";
  const body = "The Hub " + releaseTag + " est disponible. Ouvrez pour voir les nouveautés.";
  const createdAtMs = String(Date.now());

  let successCount = 0;
  let failureCount = 0;
  let invalidTokenCount = 0;

  for (let offset = 0; offset < candidates.length; offset += 500) {
    const chunk = candidates.slice(offset, offset + 500);

    const messages = chunk.map((entry) => ({
      token: entry.token,
      data: {
        recipientId: entry.userId,
        type: "app_update",
        title,
        body,
        appVersion: releaseVersion,
        releaseTag,
        releaseName,
        releaseUrl,
        deepLink: "thehub://update",
        createdAtMs,
      },
      android: {
        priority: "high",
      },
    }));

    const response = await messaging.sendEach(messages);
    successCount += response.successCount;
    failureCount += response.failureCount;

    for (let index = 0; index < response.responses.length; index += 1) {
      const sendResponse = response.responses[index];
      if (sendResponse.success) continue;

      const errorCode = sendResponse.error?.code || "";
      if (
        errorCode === "messaging/registration-token-not-registered" ||
        errorCode === "messaging/invalid-registration-token"
      ) {
        invalidTokenCount += 1;
        try {
          await chunk[index].doc.ref.delete();
        } catch (error) {
          console.warn(
            "Impossible de supprimer le token invalide:",
            error?.message || String(error)
          );
        }
      }
    }
  }

  console.log(JSON.stringify({
    releaseVersion,
    totalTokens: tokenSnapshot.size,
    candidates: candidates.length,
    successCount,
    failureCount,
    invalidTokenCount,
    skippedUpToDate,
    skippedMalformed,
  }, null, 2));
}

run().catch((error) => {
  console.error(error);
  process.exit(1);
});
