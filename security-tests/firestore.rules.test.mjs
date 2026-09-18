import fs from "node:fs";
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from "@firebase/rules-unit-testing";
import {
  doc,
  getDoc,
  serverTimestamp,
  setDoc,
  updateDoc,
  writeBatch,
} from "firebase/firestore";

const rules = fs.readFileSync(new URL("../firestore.rules", import.meta.url), "utf8");

let testEnv;
let aliceDb;
let bobDb;
let charlieDb;

function alice() {
  return aliceDb;
}

function bob() {
  return bobDb;
}

async function seed(ctx) {
  const db = ctx.firestore();

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
  await setDoc(doc(db, "conversations/alice_bob"), {
    participantIds: ["alice", "bob"],
    participantsInfo: {
      alice: { username: "alice" },
      bob: { username: "bob" },
    },
    lastMessageText: "",
    lastMessageAt: new Date(),
    lastMessageSenderId: "",
    unreadCount: { alice: 0, bob: 0 },
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


async function testMessageSecurityAndAtomicSend() {
  const conversationRef = doc(alice(), "conversations/alice_bob");
  const messageRef = doc(alice(), "conversations/alice_bob/messages/message-1");
  const opRef = doc(alice(), "conversations/alice_bob/messageOps/alice");

  const validSend = writeBatch(alice());
  validSend.set(messageRef, {
    senderId: "alice",
    text: "hello",
    imageUrl: null,
    createdAt: serverTimestamp(),
    status: "sent",
  });
  validSend.set(opRef, {
    type: "send",
    targetId: "message-1",
  });
  validSend.update(conversationRef, {
    lastMessageText: "hello",
    lastMessageAt: serverTimestamp(),
    lastMessageSenderId: "alice",
    "unreadCount.bob": 1,
  });
  await assertSucceeds(validSend.commit());

  const senderMessageRef = doc(alice(), "conversations/alice_bob/messages/message-1");
  await assertFails(updateDoc(senderMessageRef, { senderId: "bob" }));
  await assertFails(updateDoc(senderMessageRef, { createdAt: new Date() }));
  await assertFails(updateDoc(senderMessageRef, { status: "read" }));
  await assertFails(updateDoc(senderMessageRef, { blockedField: true }));
  await assertSucceeds(updateDoc(senderMessageRef, { text: "edited" }));
  await assertSucceeds(updateDoc(senderMessageRef, { imageUrl: "https://example.com/image.jpg" }));
}

async function testMessageCreationRejectsForgedMetadata() {
  const forgedSender = doc(alice(), "conversations/alice_bob/messages/forged-sender");
  const senderBatch = writeBatch(alice());
  senderBatch.set(forgedSender, {
    senderId: "bob",
    text: "forged",
    imageUrl: null,
    createdAt: serverTimestamp(),
    status: "sent",
  });
  senderBatch.set(doc(alice(), "conversations/alice_bob/messageOps/alice"), {
    type: "send",
    targetId: "forged-sender",
  });
  senderBatch.update(doc(alice(), "conversations/alice_bob"), {
    lastMessageText: "forged",
    lastMessageAt: serverTimestamp(),
    lastMessageSenderId: "bob",
    "unreadCount.bob": 1,
  });
  await assertFails(senderBatch.commit());

  const forgedTimestamp = doc(alice(), "conversations/alice_bob/messages/forged-time");
  const timestampBatch = writeBatch(alice());
  timestampBatch.set(forgedTimestamp, {
    senderId: "alice",
    text: "forged",
    imageUrl: null,
    createdAt: new Date("2000-01-01T00:00:00Z"),
    status: "sent",
  });
  timestampBatch.set(doc(alice(), "conversations/alice_bob/messageOps/alice"), {
    type: "send",
    targetId: "forged-time",
  });
  timestampBatch.update(doc(alice(), "conversations/alice_bob"), {
    lastMessageText: "forged",
    lastMessageAt: serverTimestamp(),
    lastMessageSenderId: "alice",
    "unreadCount.bob": 2,
  });
  await assertFails(timestampBatch.commit());

  await assertFails(updateDoc(
    doc(alice(), "conversations/alice_bob"),
    { lastMessageText: "tampered" }
  ));
  await assertFails(updateDoc(
    doc(alice(), "conversations/alice_bob"),
    { lastMessageSenderId: "bob" }
  ));
  await assertFails(updateDoc(
    doc(alice(), "conversations/alice_bob"),
    { "unreadCount.bob": 999 }
  ));
}

async function testMessageRecipientCanMarkReadOnly() {
  const messageRef = doc(bob(), "conversations/alice_bob/messages/message-1");
  await assertSucceeds(updateDoc(
    doc(bob(), "conversations/alice_bob"),
    { "unreadCount.bob": 0 }
  ));
  await assertSucceeds(updateDoc(messageRef, { status: "read" }));
  await assertFails(updateDoc(messageRef, { senderId: "bob" }));
  await assertFails(updateDoc(messageRef, { text: "recipient edit" }));
}

async function testMessageAccessIsLimitedToParticipants() {
  await assertFails(getDoc(doc(charlieDb, "conversations/alice_bob")));
  await assertFails(getDoc(
    doc(charlieDb, "conversations/alice_bob/messages/message-1")
  ));

  const outsiderMessage = writeBatch(charlieDb);
  outsiderMessage.set(
    doc(charlieDb, "conversations/alice_bob/messages/outsider-message"),
    {
      senderId: "charlie",
      text: "intrusion",
      imageUrl: null,
      createdAt: serverTimestamp(),
      status: "sent",
    }
  );
  outsiderMessage.set(
    doc(charlieDb, "conversations/alice_bob/messageOps/charlie"),
    {
      type: "send",
      targetId: "outsider-message",
    }
  );
  await assertFails(outsiderMessage.commit());
}

async function testMessageDeletionMustBeAtomicWithConversationDeletion() {
  const messageRef = doc(alice(), "conversations/alice_bob/messages/message-1");
  await assertFails(updateDoc(messageRef, { status: "delivered" }));

  const deleteOnlyMessage = writeBatch(alice());
  deleteOnlyMessage.delete(messageRef);
  await assertFails(deleteOnlyMessage.commit());

  const deleteConversation = writeBatch(alice());
  deleteConversation.delete(messageRef);
  deleteConversation.delete(doc(alice(), "conversations/alice_bob"));
  await assertSucceeds(deleteConversation.commit());

  await assertFails(getDoc(doc(bob(), "conversations/alice_bob/messages/message-1")));
}

try {
  testEnv = await initializeTestEnvironment({
    projectId: "demo-the-hub-security",
    firestore: { rules },
  });

  aliceDb = testEnv.authenticatedContext("alice").firestore();
  bobDb = testEnv.authenticatedContext("bob").firestore();
  charlieDb = testEnv.authenticatedContext("charlie").firestore();

  await testEnv.withSecurityRulesDisabled(seed);

  await testDirectCounterTampering();
  await testLikeCounterMustMatchLikeMutation();
  await testCommentCounterMustMatchCommentMutation();
  await testFollowerCounterMustMatchFollowMutation();
  await testRepostCounterMustMatchRepostMutation();
  await testCommentDeleteCounterMustMatchDeletion();
  await testUnfollowCounterMustMatchDeletion();
  await testRepostDeleteCounterMustMatchDeletion();
  await testMessageSecurityAndAtomicSend();
  await testMessageCreationRejectsForgedMetadata();
  await testMessageRecipientCanMarkReadOnly();
  await testMessageAccessIsLimitedToParticipants();
  await testMessageDeletionMustBeAtomicWithConversationDeletion();

  console.log("Firestore security tests (counters + messages): PASS");
} finally {
  if (testEnv) {
    await testEnv.cleanup();
  }
}
