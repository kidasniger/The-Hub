const { onDocumentCreated } = require("firebase-functions/firestore");
const { setGlobalOptions } = require("firebase-functions");
const { logger } = require("firebase-functions");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore, FieldValue } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");

initializeApp();

setGlobalOptions({
  region: "europe-west1",
  maxInstances: 10,
});

const db = getFirestore();
const messaging = getMessaging();

const TYPES = new Set([
  "like",
  "comment",
  "follow",
  "message",
  "like_comment",
  "reply_comment",
]);

function buildNotificationContent(data) {
  const actor = data.actorUsername || "Quelqu'un";
  switch (data.type) {
    case "like":
      return { title: "Nouveau j'aime", body: actor + " a aimé votre publication." };
    case "comment":
      return { title: "Nouveau commentaire", body: actor + " a commenté votre publication." };
    case "follow":
      return { title: "Nouvel abonné", body: actor + " vous suit maintenant." };
    case "message":
      return { title: "Nouveau message", body: actor + " vous a envoyé un message." };
    case "like_comment":
      return { title: "J'aime sur votre commentaire", body: actor + " a aimé votre commentaire." };
    case "reply_comment":
      return { title: "Réponse à votre commentaire", body: actor + " a répondu à votre commentaire." };
    default:
      return { title: "The Hub", body: "Vous avez une nouvelle notification." };
  }
}

function buildDeepLink(data) {
  const type = data.type;
  if (type === "follow" && data.actorId) {
    return "thehub://profile/" + encodeURIComponent(data.actorId);
  }

  if (type === "message" && data.conversationId) {
    return "thehub://chat/" + encodeURIComponent(data.conversationId);
  }

  if (
    (type === "comment" ||
      type === "like_comment" ||
      type === "reply_comment") &&
    data.postId
  ) {
    return "thehub://comments/" + encodeURIComponent(data.postId);
  }

  if (data.postId) {
    return "thehub://post/" + encodeURIComponent(data.postId);
  }

  return "thehub://feed";
}

function sanitizeData(data) {
  const value = data == null ? "" : String(data);
  return value.length > 3500 ? value.slice(0, 3500) : value;
}

async function deleteInvalidToken(tokenRef, token) {
  try {
    await tokenRef.delete();
    logger.info("Removed invalid FCM token", { token });
  } catch (error) {
    logger.warn("Could not remove invalid FCM token", {
      token,
      error: error?.message || String(error),
    });
  }
}

exports.pushNotificationOnCreate = onDocumentCreated(
  "notifications/{notificationId}",
  async (event) => {
    const snapshot = event.data;
    if (!snapshot) return null;

    const notification = snapshot.data() || {};
    const recipientId = notification.recipientId;
    const actorId = notification.actorId;
    const type = notification.type;

    if (
      !recipientId ||
      !actorId ||
      actorId === recipientId ||
      !TYPES.has(type)
    ) {
      return null;
    }

    const tokenSnapshot = await db
      .collection("users")
      .doc(recipientId)
      .collection("fcmTokens")
      .get();

    if (tokenSnapshot.empty) {
      logger.info("No FCM tokens for notification recipient", { recipientId });
      return null;
    }

    const content = buildNotificationContent(notification);
    const deepLink = buildDeepLink(notification);
    const data = {
      notificationId: sanitizeData(snapshot.id),
      type: sanitizeData(type),
      title: sanitizeData(content.title),
      body: sanitizeData(content.body),
      actorId: sanitizeData(notification.actorId),
      actorUsername: sanitizeData(notification.actorUsername || ""),
      postId: sanitizeData(notification.postId || ""),
      commentId: sanitizeData(notification.commentId || ""),
      conversationId: sanitizeData(notification.conversationId || ""),
      deepLink,
    };

    const tokenDocs = tokenSnapshot.docs;
    const tokens = tokenDocs
      .map((doc) => doc.getString("token"))
      .filter((token) => typeof token === "string" && token.length > 0);

    if (tokens.length === 0) {
      return null;
    }

    let successCount = 0;
    let failureCount = 0;

    for (let offset = 0; offset < tokens.length; offset += 500) {
      const chunk = tokens.slice(offset, offset + 500);
      const message = {
        tokens: chunk,
        data,
        android: {
          priority: "high",
        },
      };

      const response = await messaging.sendEachForMulticast(message);
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
          const token = chunk[index];
          const tokenDoc = tokenDocs.find(
            (doc) => doc.getString("token") === token
          );
          if (tokenDoc) {
            await deleteInvalidToken(tokenDoc.ref, token);
          }
        }
      }
    }

    await snapshot.ref.set(
      {
        pushSentAt: FieldValue.serverTimestamp(),
        pushSuccessCount: successCount,
        pushFailureCount: failureCount,
      },
      { merge: true }
    );

    logger.info("Push notification dispatched", {
      notificationId: snapshot.id,
      recipientId,
      type,
      successCount,
      failureCount,
    });

    return null;
  }
);
