import fs from "node:fs";
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from "@firebase/rules-unit-testing";
import {
  doc,
  getFirestore,
  setDoc,
  updateDoc,
  writeBatch,
} from "firebase/firestore";

const rules = fs.readFileSync(new URL("../firestore.rules", import.meta.url), "utf8");

let testEnv;

const alice = () => getFirestore(testEnv.authenticatedContext("alice"));
const bob = () => getFirestore(testEnv.authenticatedContext("bob"));

async function seed() {
  const db = getFirestore(testEnv.unauthenticatedContext());

  await setDoc(doc(db, "users/alice"), {
    uid: "alice",
    username: "alice",
    followersCount: 0,
    followingCount: 0,
  });
  await setDoc(doc(db, "users/bob"), {
    uid: "bob",
    username: "bob",
    followersCount: 0,
    followingCount: 0,
  });
  await setDoc(doc(db, "posts/post-1"), {
    authorId: "alice",
    authorUsername: "alice",
    text: "seed",
    createdAt: new Date(),
    likesCount: 0,
    commentsCount: 0,
    repostsCount: 0,
    isRepost: false,
  });
}

async function testDirectCounterTampering() {
  const attackerPost = doc(bob(), "posts/post-1");
  await assertFails(updateDoc(attackerPost, { likesCount: 999 }));
  await assertFails(updateDoc(attackerPost, { commentsCount: 999 }));
  await assertFails(updateDoc(attackerPost, { repostsCount: 999 }));
  await assertFails(updateDoc(doc(alice(), "posts/post-1"), { likesCount: 999 }));
  await assertFails(updateDoc(doc(alice(), "posts/post-1"), { commentsCount: 999 }));
  await assertFails(updateDoc(doc(alice(), "posts/post-1"), { repostsCount: 999 }));
  await assertFails(updateDoc(doc(alice(), "users/alice"), { followersCount: 999 }));
}

async function testLikeCounterMustMatchLikeMutation() {
  await assertFails(updateDoc(doc(bob(), "posts/post-1"), { likesCount: 1 }));

  const batch = writeBatch(bob());
  batch.set(doc(bob(), "posts/post-1/likes/bob"), { likedAt: new Date() });
  batch.update(doc(bob(), "posts/post-1"), { likesCount: 1 });
  await assertSucceeds(batch.commit());

  await assertFails(updateDoc(doc(bob(), "posts/post-1"), { likesCount: 2 }));
}

async function testCommentCounterMustMatchCommentMutation() {
  await assertFails(updateDoc(doc(bob(), "posts/post-1"), { commentsCount: 1 }));

  const batch = writeBatch(bob());
  batch.set(doc(bob(), "posts/post-1/comments/comment-1"), {
    authorId: "bob",
    authorUsername: "bob",
    text: "hello",
    createdAt: new Date(),
    likesCount: 0,
  });
  batch.set(doc(bob(), "posts/post-1/counterOps/bob"), {
    type: "comment_create",
    targetId: "comment-1",
  });
  batch.update(doc(bob(), "posts/post-1"), { commentsCount: 1 });
  await assertSucceeds(batch.commit());

  await assertFails(updateDoc(doc(bob(), "posts/post-1"), { commentsCount: 2 }));
}

async function testFollowerCounterMustMatchFollowMutation() {
  await assertFails(updateDoc(doc(bob(), "users/alice"), { followersCount: 1 }));

  const batch = writeBatch(bob());
  batch.set(doc(bob(), "users/alice/followers/bob"), {
    followedAt: new Date(),
    followerId: "bob",
    uid: "bob",
  });
  batch.set(doc(bob(), "users/bob/following/alice"), {
    followedAt: new Date(),
    followingId: "alice",
    uid: "alice",
  });
  batch.update(doc(bob(), "users/alice"), { followersCount: 1 });
  await assertSucceeds(batch.commit());

  await assertFails(updateDoc(doc(bob(), "users/alice"), { followersCount: 2 }));
}

async function testRepostCounterMustMatchRepostMutation() {
  await assertFails(updateDoc(doc(bob(), "posts/post-1"), { repostsCount: 1 }));

  const batch = writeBatch(bob());
  batch.set(doc(bob(), "posts/repost-1"), {
    authorId: "bob",
    authorUsername: "bob",
    text: "",
    createdAt: new Date(),
    likesCount: 0,
    commentsCount: 0,
    repostsCount: 0,
    isRepost: true,
    originalPostId: "post-1",
  });
  batch.set(doc(bob(), "posts/post-1/reposts/bob"), {
    postId: "post-1",
    reposterId: "bob",
    repostId: "repost-1",
    createdAt: new Date(),
  });
  batch.update(doc(bob(), "posts/post-1"), { repostsCount: 1 });
  await assertSucceeds(batch.commit());

  await assertFails(updateDoc(doc(bob(), "posts/post-1"), { repostsCount: 2 }));
}

async function testCommentDeleteCounterMustMatchDeletion() {
  const batch = writeBatch(bob());
  batch.delete(doc(bob(), "posts/post-1/comments/comment-1"));
  batch.set(doc(bob(), "posts/post-1/counterOps/bob"), {
    type: "comment_delete",
    targetId: "comment-1",
  });
  batch.update(doc(bob(), "posts/post-1"), { commentsCount: 0 });
  await assertSucceeds(batch.commit());

  await assertFails(updateDoc(doc(bob(), "posts/post-1"), { commentsCount: -1 }));
}

async function testUnfollowCounterMustMatchDeletion() {
  const batch = writeBatch(bob());
  batch.delete(doc(bob(), "users/alice/followers/bob"));
  batch.delete(doc(bob(), "users/bob/following/alice"));
  batch.update(doc(bob(), "users/alice"), { followersCount: 0 });
  await assertSucceeds(batch.commit());

  await assertFails(updateDoc(doc(bob(), "users/alice"), { followersCount: 1 }));
}

async function testRepostDeleteCounterMustMatchDeletion() {
  const batch = writeBatch(bob());
  batch.delete(doc(bob(), "posts/repost-1"));
  batch.delete(doc(bob(), "posts/post-1/reposts/bob"));
  batch.update(doc(bob(), "posts/post-1"), { repostsCount: 0 });
  await assertSucceeds(batch.commit());

  await assertFails(updateDoc(doc(bob(), "posts/post-1"), { repostsCount: 1 }));
}

try {
  testEnv = await initializeTestEnvironment({
    projectId: "demo-the-hub-security",
    firestore: { rules },
  });

  await testEnv.withSecurityRulesDisabled(seed);

  await testDirectCounterTampering();
  await testLikeCounterMustMatchLikeMutation();
  await testCommentCounterMustMatchCommentMutation();
  await testFollowerCounterMustMatchFollowMutation();
  await testRepostCounterMustMatchRepostMutation();
  await testCommentDeleteCounterMustMatchDeletion();
  await testUnfollowCounterMustMatchDeletion();
  await testRepostDeleteCounterMustMatchDeletion();

  console.log("Firestore counter security tests: PASS");
} finally {
  if (testEnv) {
    await testEnv.cleanup();
  }
}
