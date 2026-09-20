// tools/push-worker/send-push-notifications.js
//
// Lancé périodiquement par GitHub Actions (pas de Cloud Function, pas de plan Blaze requis).
// Cherche les notifications Firestore avec pushSent == false, envoie un push FCM au
// destinataire si un token est connu, puis marque la notification comme traitée.

const { initializeApp, cert } = require('firebase-admin/app');
const { getFirestore, FieldValue } = require('firebase-admin/firestore');
const { getMessaging } = require('firebase-admin/messaging');
const fs = require('fs');

const serviceAccount = JSON.parse(
  fs.readFileSync(process.env.GOOGLE_APPLICATION_CREDENTIALS, 'utf8')
);

const app = initializeApp({
  credential: cert(serviceAccount),
});

const db = getFirestore(app);
const messaging = getMessaging(app);

function notificationText(n) {
  switch (n.type) {
    case 'like':
      return `${n.actorUsername || 'Quelqu\'un'} a aimé votre publication`;
    case 'comment':
      return `${n.actorUsername || 'Quelqu\'un'} a commenté : ${n.commentText || ''}`;
    case 'follow':
      return `${n.actorUsername || 'Quelqu\'un'} a commencé à vous suivre`;
    case 'message':
      return `${n.actorUsername || 'Quelqu\'un'} vous a envoyé un message`;
    default:
      return 'Nouvelle notification';
  }
}

async function run() {
  const snapshot = await db
    .collection('notifications')
    .where('pushSent', '==', false)
    .limit(200)
    .get();

  if (snapshot.empty) return 0;

  console.log(`${snapshot.size} notification(s) à traiter.`);

  for (const doc of snapshot.docs) {
    const notif = doc.data();

    if (!notif.recipientId) {
      await doc.ref.update({ pushSent: true, pushSkippedReason: 'no_recipientId' });
      continue;
    }

    const tokensSnap = await db
      .collection('users')
      .doc(notif.recipientId)
      .collection('fcmTokens')
      .get();

    if (tokensSnap.empty) {
      const ageMs = notif.createdAt
        ? Date.now() - notif.createdAt.toDate().getTime()
        : 0;
      const RETRY_TIMEOUT_MS = 24 * 60 * 60 * 1000;

      if (ageMs > RETRY_TIMEOUT_MS) {
        console.log(`Aucun token FCM pour ${notif.recipientId} depuis plus de 24h — abandon.`);
        await doc.ref.update({ pushSent: true, pushSkippedReason: 'no_token_timeout' });
      } else {
        console.log(`Aucun token FCM pour ${notif.recipientId} pour l'instant — nouvelle tentative plus tard.`);
      }
      continue;
    }

    let sentCount = 0;
    let lastError = null;

    for (const tokenDoc of tokensSnap.docs) {
      const token = tokenDoc.data().token;
      if (!token) continue;

      try {
        await messaging.send({
          token,
          notification: {
            title: 'The Hub',
            body: notificationText(notif),
          },
          data: {
            type: notif.type || '',
            postId: notif.postId || '',
            notificationId: doc.id,
          },
          android: { priority: 'high' },
        });
        sentCount += 1;
      } catch (err) {
        lastError = err;
        console.error(`Échec d'envoi vers un appareil de ${notif.recipientId} :`, err.message);
        const isInvalidToken =
          err.code === 'messaging/invalid-registration-token' ||
          err.code === 'messaging/registration-token-not-registered';

        if (isInvalidToken) {
          await tokenDoc.ref.delete();
        }
      }
    }

    await doc.ref.update({
      pushSent: true,
      pushSentAt: FieldValue.serverTimestamp(),
      pushSentDeviceCount: sentCount,
      ...(sentCount === 0 && lastError ? { pushSkippedReason: 'send_error' } : {}),
    });

    if (sentCount > 0) {
      console.log(`Push envoyé pour la notification ${doc.id} vers ${sentCount} appareil(s).`);
    }
  }

  return snapshot.size;
}

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

const POLL_INTERVAL_MS = 10 * 1000;
const MAX_RUNTIME_MS = 4 * 60 * 60 * 1000 + 50 * 60 * 1000;

async function watchLoop() {
  const start = Date.now();
  let checks = 0;

  while (Date.now() - start < MAX_RUNTIME_MS) {
    try {
      const count = await run();
      checks += 1;
      if (count > 0) console.log(`[check #${checks}] ${count} notification(s) envoyée(s).`);
    } catch (err) {
      console.error('Erreur pendant une vérification :', err);
    }
    await sleep(POLL_INTERVAL_MS);
  }

  console.log(`Fin de la boucle après ${checks} vérifications (~${Math.round((Date.now() - start) / 1000)}s).`);
}

watchLoop()
  .then(() => process.exit(0))
  .catch((err) => {
    console.error('Erreur du script :', err);
    process.exit(1);
  });
