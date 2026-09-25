const { onRequest } = require("firebase-functions/https");
const { setGlobalOptions } = require("firebase-functions");
const { logger } = require("firebase-functions");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore } = require("firebase-admin/firestore");

initializeApp();

setGlobalOptions({
  region: "europe-west1",
  maxInstances: 10,
});

const db = getFirestore();

function escapeHtml(value) {
  return String(value ?? "")
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#039;");
}

function safeHttpUrl(value) {
  if (typeof value !== "string" || !value.trim()) return "";
  try {
    const url = new URL(value);
    return url.protocol === "https:" || url.protocol === "http:" ? url.href : "";
  } catch (_) {
    return "";
  }
}

function formatDate(value) {
  try {
    const date = typeof value?.toDate === "function" ? value.toDate() : new Date(value);
    if (Number.isNaN(date.getTime())) return "";
    return new Intl.DateTimeFormat("fr-FR", {
      dateStyle: "medium",
      timeStyle: "short",
      timeZone: "Africa/Niamey",
    }).format(date);
  } catch (_) {
    return "";
  }
}

function postIdFromRequest(req) {
  const path = String(req.path || "").split("/").filter(Boolean);
  if (path[0] === "post" && path[1]) {
    return decodeURIComponent(path[1]);
  }
  const queryId = typeof req.query.id === "string" ? req.query.id : "";
  return queryId.trim();
}

function pageShell({ title, description, imageUrl, body }) {
  const safeTitle = escapeHtml(title);
  const safeDescription = escapeHtml(description);
  const safeImage = safeHttpUrl(imageUrl);
  const ogImage = safeImage
    ? `<meta property="og:image" content="${escapeHtml(safeImage)}">`
    : "";

  return `<!doctype html>
<html lang="fr">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width,initial-scale=1">
  <meta name="theme-color" content="#0b0b0d">
  <meta name="description" content="${safeDescription}">
  <meta property="og:site_name" content="The Hub">
  <meta property="og:title" content="${safeTitle}">
  <meta property="og:description" content="${safeDescription}">
  <meta property="og:type" content="article">
  ${ogImage}
  <title>${safeTitle} · The Hub</title>
  <style>
    :root {
      color-scheme: dark;
      font-family: Inter,system-ui,-apple-system,BlinkMacSystemFont,"Segoe UI",sans-serif;
    }
    * { box-sizing: border-box; }
    body {
      margin: 0;
      min-height: 100vh;
      background:
        radial-gradient(circle at 50% -10%, rgba(255,255,255,.08), transparent 42%),
        #09090b;
      color: #fff;
    }
    header {
      width: min(100%, 760px);
      margin: 0 auto;
      padding: 24px 20px 8px;
      display: flex;
      align-items: center;
      gap: 10px;
      font-weight: 800;
      font-size: 22px;
    }
    .logo {
      width: 38px;
      height: 38px;
      border-radius: 12px;
      display: grid;
      place-items: center;
      background: #fff;
      color: #111;
      font-size: 19px;
    }
    main {
      width: min(92vw, 620px);
      margin: 26px auto 48px;
    }
    .card {
      overflow: hidden;
      border: 1px solid #29292f;
      border-radius: 24px;
      background: #151519;
      box-shadow: 0 24px 80px rgba(0,0,0,.38);
    }
    .author {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 20px 20px 14px;
    }
    .avatar {
      width: 46px;
      height: 46px;
      border-radius: 50%;
      object-fit: cover;
      background: #29292f;
    }
    .avatar-fallback {
      display: grid;
      place-items: center;
      font-weight: 800;
    }
    .username { font-weight: 750; }
    .date { margin-top: 3px; color: #85858f; font-size: 13px; }
    .content {
      padding: 6px 20px 20px;
      white-space: pre-wrap;
      overflow-wrap: anywhere;
      line-height: 1.55;
      font-size: 17px;
    }
    .media {
      width: 100%;
      max-height: 620px;
      display: block;
      object-fit: cover;
      background: #09090b;
    }
    .video { max-height: 620px; }
    .stats {
      display: flex;
      gap: 22px;
      padding: 15px 20px;
      color: #a4a4ad;
      border-top: 1px solid #29292f;
      font-size: 14px;
    }
    .actions {
      padding: 18px 20px 22px;
      display: grid;
      gap: 10px;
    }
    .open {
      display: block;
      text-align: center;
      padding: 14px 18px;
      border-radius: 15px;
      background: #fff;
      color: #111;
      text-decoration: none;
      font-weight: 800;
    }
    .install {
      display: block;
      text-align: center;
      padding: 12px 18px;
      border-radius: 15px;
      border: 1px solid #34343b;
      color: #ddd;
      text-decoration: none;
      font-weight: 650;
    }
    footer {
      text-align: center;
      color: #686871;
      font-size: 12px;
      margin-top: 18px;
    }
    .error {
      text-align: center;
      padding: 42px 26px;
    }
    .error h1 { margin: 0 0 10px; }
    .error p { color: #a4a4ad; line-height: 1.5; }
  </style>
</head>
<body>
  <header><span class="logo">H</span><span>The Hub</span></header>
  <main>${body}</main>
</body>
</html>`;
}

function errorPage(title, message) {
  const body = `<section class="card error">
    <h1>${escapeHtml(title)}</h1>
    <p>${escapeHtml(message)}</p>
    <div class="actions">
      <a class="open" href="https://the-hub-f95f4.web.app/">Retour à The Hub</a>
      <a class="install" href="https://github.com/kidasniger/The-Hub/releases/latest">Installer / mettre à jour The Hub</a>
    </div>
  </section>
  <footer>Une publication The Hub partagée avec vous.</footer>`;
  return pageShell({
    title: `${title} · The Hub`,
    description: message,
    body,
  });
}

function renderPost(post, postId) {
  const username = post.authorUsername || "thehub_user";
  const text = post.text || "";
  const imageUrl = safeHttpUrl(post.imageUrl);
  const videoUrl = safeHttpUrl(post.videoUrl);
  const authorPhotoUrl = safeHttpUrl(post.authorPhotoUrl);
  const date = formatDate(post.createdAt);
  const appLink = "thehub://post/" + encodeURIComponent(postId);
  const description = text.replace(/\s+/g, " ").trim().slice(0, 180) || "Publication partagée sur The Hub";
  const initial = escapeHtml(username.trim().charAt(0).toUpperCase() || "T");

  const avatar = authorPhotoUrl
    ? `<img class="avatar" src="${escapeHtml(authorPhotoUrl)}" alt="">`
    : `<div class="avatar avatar-fallback">${initial}</div>`;

  let media = "";
  if (imageUrl) {
    media = `<img class="media" src="${escapeHtml(imageUrl)}" alt="Image de la publication" loading="eager">`;
  } else if (videoUrl) {
    media = `<video class="media video" src="${escapeHtml(videoUrl)}" controls playsinline preload="metadata"></video>`;
  }

  const body = `<article class="card">
    <div class="author">
      ${avatar}
      <div>
        <div class="username">${escapeHtml(username)}</div>
        <div class="date">${escapeHtml(date)}</div>
      </div>
    </div>
    ${text ? `<div class="content">${escapeHtml(text)}</div>` : ""}
    ${media}
    <div class="stats">
      <span>❤️ ${Number(post.likesCount) || 0} J’aime</span>
      <span>💬 ${Number(post.commentsCount) || 0} commentaires</span>
      <span>🔁 ${Number(post.repostsCount) || 0} republications</span>
    </div>
    <div class="actions">
      <a class="open" href="${appLink}">Ouvrir dans The Hub</a>
      <a class="install" href="https://github.com/kidasniger/The-Hub/releases/latest">Installer The Hub</a>
    </div>
  </article>
  <footer>Publication ${escapeHtml(postId)} · The Hub</footer>`;

  return pageShell({
    title: `${username} sur The Hub`,
    description,
    imageUrl,
    body,
  });
}

exports.postPreview = onRequest(async (req, res) => {
  res.set("Cache-Control", "public, max-age=60, s-maxage=300");
  res.set("X-Content-Type-Options", "nosniff");

  const postId = postIdFromRequest(req);
  if (!postId || postId.length > 256) {
    res.status(400).type("html").send(errorPage("Lien invalide", "Cette publication n'a pas pu être identifiée."));
    return;
  }

  try {
    const snapshot = await db.collection("posts").doc(postId).get();
    if (!snapshot.exists) {
      res.status(404).type("html").send(errorPage("Publication introuvable", "Cette publication n'existe plus ou le lien est incorrect."));
      return;
    }

    const post = snapshot.data() || {};
    if (post.isHidden === true || post.isDeletedByAdmin === true) {
      res.status(404).type("html").send(errorPage("Publication indisponible", "Cette publication n'est plus disponible publiquement."));
      return;
    }

    res.status(200).type("html").send(renderPost({ ...post, id: snapshot.id }, snapshot.id));
  } catch (error) {
    logger.error("Public post preview failed", {
      postId,
      error: error?.message || String(error),
    });
    res.status(500).type("html").send(errorPage("Aperçu indisponible", "Impossible de charger cette publication pour le moment."));
  }
});
