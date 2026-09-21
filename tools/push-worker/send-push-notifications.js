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

function notificationContent(n) {
  const actor = n.actorDisplayName || n.actorUsername || 'Quelqu\'un';
  switch (n.type) {
    case 'ADMIN_BROADCAST':
    case 'ADMIN_ANNOUNCEMENT':
      return {
        title: n.title || 'The Hub',
        body: n.commentText || 'Une nouvelle annonce est disponible.',
      };
    case 'like':
      return { title: 'Nouveau j\'aime', body: actor + ' a aimé votre publication.' };
    case 'comment':
      return { title: 'Nouveau commentaire', body: n.commentText ? actor + ' a commenté : ' + n.commentText : actor + ' a commenté votre publication.' };
    case 'follow':
      return { title: 'Nouvel abonné', body: actor + ' a commencé à vous suivre.' };
    case 'message':
      return { title: 'Nouveau message', body: actor + ' vous a envoyé un message.' };
    case 'like_comment':
      return { title: 'J\'aime sur votre commentaire', body: actor + ' a aimé votre commentaire.' };
    case 'reply_comment':
      return { title: 'Réponse à votre commentaire', body: actor + ' a répondu à votre commentaire.' };
    default:
      return { title: 'The Hub', body: 'Vous avez une nouvelle notification.' };
  }
}

function buildDeepLink(n) {
  if (n.type === 'follow' && n.actorId) {
    return 'thehub://profile/' + encodeURIComponent(n.actorId);
  }
  if (n.type === 'message' && n.conversationId) {
    return 'thehub://chat/' + encodeURIComponent(n.conversationId);
  }
  if (
    (n.type === 'comment' || n.type === 'like_comment' || n.type === 'reply_comment') &&
    n.postId
  ) {
    return 'thehub://comments/' + encodeURIComponent(n.postId);
  }
  if (n.postId) {
    return 'thehub://post/' + encodeURIComponent(n.postId);
  }
  return 'thehub://feed';
}

function sanitizeData(value) {
  const text = value == null ? '' : String(value);
  return text.length > 3500 ? text.slice(0, 3500) : text;
}

async function getAnnouncementRecipients(segment) {
  const usersSnapshot = await db.collection('users').limit(5000).get();
  const users = usersSnapshot.docs;

  if (segment === 'admins') {
    const admins = await db.collection('admins').where('active', '==', true).get();
    const ids = new Set(admins.docs.map((doc) => doc.id));
    return users.filter((doc) => ids.has(doc.id));
  }

  if (segment === 'active_7d') {
    const cutoff = Date.now() - 7 * 24 * 60 * 60 * 1000;
    const presence = await db.collectionGroup('presence').limit(10000).get();
    const activeIds = new Set(
      presence.docs
        .filter((doc) => {
          const lastSeen = doc.get('lastSeen');
          return lastSeen && lastSeen.toDate().getTime() >= cutoff;
        })
        .map((doc) => doc.ref.parent.parent.id)
    );
    return users.filter((doc) => activeIds.has(doc.id));
  }

  return users;
}

async function processScheduledAnnouncements() {
  const now = Date.now();
  const scheduled = await db
    .collection('announcements')
    .where('status', '==', 'scheduled')
    .where('active', '==', true)
    .limit(50)
    .get();

  for (const announcementDoc of scheduled.docs) {
    const announcement = announcementDoc.data();
    const publishAt = announcement.publishAt?.toDate?.().getTime?.() ?? 0;
    if (!publishAt || publishAt > now) continue;

    const recipients = await getAnnouncementRecipients(announcement.segment || 'all');
    for (const chunk of Array.from({ length: Math.ceil(recipients.length / 400) }, (_, i) =>
      recipients.slice(i * 400, (i + 1) * 400)
    )) {
      const batch = db.batch();
      for (const userDoc of chunk) {
        const notificationId = `announcement-${announcementDoc.id}-${userDoc.id}`;
        batch.set(db.collection('notifications').doc(notificationId), {
          recipientId: userDoc.id,
          actorId: announcement.createdBy || 'thehub',
          actorUsername: 'thehub',
          actorDisplayName: 'The Hub',
          type: 'ADMIN_ANNOUNCEMENT',
          title: announcement.title || 'The Hub',
          commentText: announcement.body || '',
          announcementId: announcementDoc.id,
          batchId: announcementDoc.id,
          createdAt: FieldValue.serverTimestamp(),
          isRead: false,
          pushSent: false,
        }, { merge: true });
      }
      if (chunk.length > 0) await batch.commit();
    }

    await announcementDoc.ref.update({
      status: 'published',
      publishedAt: FieldValue.serverTimestamp(),
      recipientCount: recipients.length,
    });
    console.log(`Annonce ${announcementDoc.id} publiée vers ${recipients.length} destinataire(s).`);
  }

  const expired = await db
    .collection('announcements')
    .where('status', '==', 'published')
    .where('active', '==', true)
    .limit(50)
    .get();

  for (const announcementDoc of expired.docs) {
    const expiresAt = announcementDoc.get('expiresAt')?.toDate?.().getTime?.() ?? 0;
    if (expiresAt && expiresAt <= now) {
      await announcementDoc.ref.update({
        active: false,
        status: 'expired',
        expiredAt: FieldValue.serverTimestamp(),
      });
      console.log(`Annonce ${announcementDoc.id} expirée.`);
    }
  }
}

async function run() {
  await processScheduledAnnouncements();
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
        let actorDisplayName = notif.actorDisplayName || '';
        if (!actorDisplayName && notif.actorId) {
          try {
            const actorSnap = await db.collection('users').doc(notif.actorId).get();
            actorDisplayName = actorSnap.get('displayName') || actorSnap.get('name') || '';
          } catch (_) {}
        }

        const notificationForPush = {
          ...notif,
          actorDisplayName,
        };
        const content = notificationContent(notificationForPush);
        const deepLink = buildDeepLink(notif);
        await messaging.send({
          token,
          data: {
            notificationId: sanitizeData(doc.id),
            recipientId: sanitizeData(notif.recipientId || ''),
            createdAtMs: sanitizeData(
              notif.createdAt && typeof notif.createdAt.toMillis === 'function'
                ? String(notif.createdAt.toMillis())
                : ''
            ),
            type: sanitizeData(notif.type || ''),
            title: sanitizeData(content.title),
            body: sanitizeData(content.body),
            actorId: sanitizeData(notif.actorId || ''),
            actorUsername: sanitizeData(notif.actorUsername || ''),
            actorDisplayName: sanitizeData(actorDisplayName),
            postId: sanitizeData(notif.postId || ''),
            commentId: sanitizeData(notif.commentId || ''),
            conversationId: sanitizeData(notif.conversationId || ''),
            messageId: sanitizeData(notif.messageId || ''),
            deepLink: sanitizeData(deepLink),
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
