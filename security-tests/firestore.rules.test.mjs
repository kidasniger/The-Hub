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
let legacyDb;
let charlieDb;
let daveDb;

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
  await setDoc(doc(db, "users/legacy"), {
    username: "legacy",
  });
  await setDoc(doc(db, "users/legacy-target"), {
    username: "legacy-target",
  });
  await setDoc(doc(db, "users/another-target"), {
    username: "another-target",
  });
  await setDoc(doc(db, "users/charlie"), {
    uid: "charlie",
    username: "charlie",
    followersCount: 0,
    followingCount: 0,
  });
  await setDoc(doc(db, "users/dave"), {
    uid: "dave",
    username: "dave",
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

  // Legacy conversation: the unreadCount field did not exist in older data.
  await setDoc(doc(db, "conversations/legacy_bob"), {
    participantIds: ["legacy", "bob"],
    participantsInfo: {
      legacy: { username: "legacy" },
      bob: { username: "bob" },
    },
    lastMessageText: "",
    lastMessageAt: new Date(),
    lastMessageSenderId: "",
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



async function testRelationshipSecurity() {
  const charlieFollowingDave = doc(
    charlieDb,
    "users/charlie/following/dave"
  );
  const daveFollowerCharlie = doc(
    charlieDb,
    "users/dave/followers/charlie"
  );

  // A single half of a follow relationship is not sufficient.
  await assertFails(setDoc(charlieFollowingDave, {
    followedAt: new Date(),
    followingId: "dave",
    uid: "dave",
  }));
  await assertFails(setDoc(daveFollowerCharlie, {
    followedAt: new Date(),
    followerId: "charlie",
    uid: "charlie",
  }));

  // The relationship document identity must match both its path and the actor.
  await assertFails(setDoc(charlieFollowingDave, {
    followedAt: new Date(),
    followingId: "alice",
    uid: "alice",
  }));

  // Valid follow: both sides + operation marker + both counters in one atomic write.
  const charlieFollowsDave = writeBatch(charlieDb);
  charlieFollowsDave.set(charlieFollowingDave, {
    followedAt: new Date(),
    followingId: "dave",
    uid: "dave",
  });
  charlieFollowsDave.set(
    doc(charlieDb, "users/charlie/followOps/charlie"),
    {
      type: "follow",
      targetId: "dave",
    }
  );
  charlieFollowsDave.set(daveFollowerCharlie, {
    followedAt: new Date(),
    followerId: "charlie",
    uid: "charlie",
  });
  charlieFollowsDave.update(doc(charlieDb, "users/charlie"), {
    followingCount: 1,
  });
  charlieFollowsDave.update(doc(charlieDb, "users/dave"), {
    followersCount: 1,
  });
  await assertSucceeds(charlieFollowsDave.commit());

  // Relationship documents are immutable.
  await assertFails(updateDoc(charlieFollowingDave, {
    uid: "alice",
  }));
  await assertFails(updateDoc(daveFollowerCharlie, {
    followerId: "alice",
  }));

  // One-sided deletion is forbidden.
  const oneSidedFollowingDelete = writeBatch(charlieDb);
  oneSidedFollowingDelete.delete(charlieFollowingDave);
  await assertFails(oneSidedFollowingDelete.commit());

  // Establish the reciprocal follow.
  const daveFollowsCharlie = writeBatch(daveDb);
  daveFollowsCharlie.set(
    doc(daveDb, "users/dave/following/charlie"),
    {
      followedAt: new Date(),
      followingId: "charlie",
      uid: "charlie",
    }
  );
  daveFollowsCharlie.set(
    doc(daveDb, "users/dave/followOps/dave"),
    {
      type: "follow",
      targetId: "charlie",
    }
  );
  daveFollowsCharlie.set(
    doc(daveDb, "users/charlie/followers/dave"),
    {
      followedAt: new Date(),
      followerId: "dave",
      uid: "dave",
    }
  );
  daveFollowsCharlie.update(doc(daveDb, "users/dave"), {
    followingCount: 1,
  });
  daveFollowsCharlie.update(doc(daveDb, "users/charlie"), {
    followersCount: 1,
  });
  await assertSucceeds(daveFollowsCharlie.commit());

  // Friends require the two users to follow each other and preserve identity.
  const charlieFriendDave = doc(
    charlieDb,
    "users/charlie/friends/dave"
  );
  const daveFriendCharlie = doc(
    charlieDb,
    "users/dave/friends/charlie"
  );

  await assertSucceeds(setDoc(charlieFriendDave, {
    friendedAt: new Date(),
    uid: "dave",
  }));
  await assertSucceeds(setDoc(daveFriendCharlie, {
    friendedAt: new Date(),
    uid: "charlie",
  }));

  await assertFails(setDoc(
    doc(charlieDb, "users/charlie/friends/alice"),
    { friendedAt: new Date(), uid: "alice" }
  ));
  await assertFails(updateDoc(charlieFriendDave, { uid: "alice" }));
  await assertSucceeds(updateDoc(charlieFriendDave, {
    friendedAt: new Date(),
  }));

  // A friend cannot be deleted while the acting user's follow still exists.
  const earlyFriendDelete = writeBatch(charlieDb);
  earlyFriendDelete.delete(charlieFriendDave);
  await assertFails(earlyFriendDelete.commit());

  // Unfollow Charlie -> Dave, then the reciprocal friend entries can be removed.
  const charlieUnfollowsDave = writeBatch(charlieDb);
  charlieUnfollowsDave.set(
    doc(charlieDb, "users/charlie/followOps/charlie"),
    {
      type: "unfollow",
      targetId: "dave",
    }
  );
  charlieUnfollowsDave.delete(charlieFollowingDave);
  charlieUnfollowsDave.delete(daveFollowerCharlie);
  charlieUnfollowsDave.update(doc(charlieDb, "users/charlie"), {
    followingCount: 0,
  });
  charlieUnfollowsDave.update(doc(charlieDb, "users/dave"), {
    followersCount: 0,
  });
  await assertSucceeds(charlieUnfollowsDave.commit());

  const deleteFriends = writeBatch(charlieDb);
  deleteFriends.delete(charlieFriendDave);
  deleteFriends.delete(daveFriendCharlie);
  await assertSucceeds(deleteFriends.commit());
}

async function testLikeAndBookmarkSecurity() {
  const likeRef = doc(charlieDb, "posts/post-1/likes/charlie");

  await assertFails(setDoc(likeRef, {
    likedAt: new Date(),
    forgedUserId: "bob",
  }));
  await assertFails(setDoc(
    doc(charlieDb, "posts/does-not-exist/likes/charlie"),
    { likedAt: new Date() }
  ));

  const likeBatch = writeBatch(charlieDb);
  likeBatch.set(likeRef, { likedAt: new Date() });
  likeBatch.update(doc(charlieDb, "posts/post-1"), { likesCount: 2 });
  await assertSucceeds(likeBatch.commit());

  await assertFails(updateDoc(likeRef, { likedAt: new Date() }));
  await assertFails(updateDoc(likeRef, { forged: true }));

  const otherUserDelete = writeBatch(bobDb);
  otherUserDelete.delete(likeRef);
  await assertFails(otherUserDelete.commit());

  const deleteLike = writeBatch(charlieDb);
  deleteLike.delete(likeRef);
  deleteLike.update(doc(charlieDb, "posts/post-1"), { likesCount: 1 });
  await assertSucceeds(deleteLike.commit());

  const bookmarkRef = doc(charlieDb, "users/charlie/bookmarks/post-1");
  await assertSucceeds(setDoc(bookmarkRef, { savedAt: new Date() }));
  await assertFails(setDoc(
    doc(charlieDb, "users/charlie/bookmarks/does-not-exist"),
    { savedAt: new Date() }
  ));
  await assertFails(setDoc(bookmarkRef, {
    savedAt: new Date(),
    forgedPostId: "post-2",
  }));
  await assertFails(updateDoc(bookmarkRef, { forgedField: true }));
  await assertSucceeds(updateDoc(bookmarkRef, { savedAt: new Date() }));

  const otherUserBookmarkDelete = writeBatch(bobDb);
  otherUserBookmarkDelete.delete(bookmarkRef);
  await assertFails(otherUserBookmarkDelete.commit());

  const deleteBookmark = writeBatch(charlieDb);
  deleteBookmark.delete(bookmarkRef);
  await assertSucceeds(deleteBookmark.commit());
}

async function testLegacyUserFollowCompatibility() {
  // Both user documents are legacy-shaped: uid and both counters are absent.
  // This is the closest equivalent to the Android follow transaction after
  // the fix: both relation documents plus concrete counter values.
  const batch = writeBatch(legacyDb);
  batch.set(doc(legacyDb, "users/legacy/following/legacy-target"), {
    followedAt: new Date(),
    followingId: "legacy-target",
    uid: "legacy-target",
  });
  batch.set(doc(legacyDb, "users/legacy-target/followers/legacy"), {
    followedAt: new Date(),
    followerId: "legacy",
    uid: "legacy",
  });
  batch.set(doc(legacyDb, "users/legacy/followOps/legacy"), {
    type: "follow",
    targetId: "legacy-target",
  });
  batch.update(doc(legacyDb, "users/legacy"), { followingCount: 1 });
  batch.update(doc(legacyDb, "users/legacy-target"), { followersCount: 1 });
  await assertSucceeds(batch.commit());

  const secondFollow = writeBatch(legacyDb);
  secondFollow.set(doc(legacyDb, "users/legacy/following/another-target"), {
    followedAt: new Date(),
    followingId: "another-target",
    uid: "another-target",
  });
  secondFollow.set(doc(legacyDb, "users/another-target/followers/legacy"), {
    followedAt: new Date(),
    followerId: "legacy",
    uid: "legacy",
  });
  secondFollow.set(doc(legacyDb, "users/legacy/followOps/legacy"), {
    type: "follow",
    targetId: "another-target",
  });
  secondFollow.update(doc(legacyDb, "users/legacy"), { followingCount: 2 });
  secondFollow.update(doc(legacyDb, "users/another-target"), { followersCount: 1 });
  await assertSucceeds(secondFollow.commit());

  await assertFails(updateDoc(doc(legacyDb, "users/legacy"), {
    followingCount: 3,
  }));
  await assertFails(updateDoc(doc(legacyDb, "users/legacy-target"), {
    followersCount: 2,
  }));
  await assertFails(updateDoc(doc(legacyDb, "users/legacy-target"), {
    followersCount: 99,
  }));
}

async function testLegacyConversationUnreadCountCompatibility() {
  const messageBatch = writeBatch(legacyDb);
  messageBatch.set(
    doc(legacyDb, "conversations/legacy_bob/messages/legacy-message-1"),
    {
      senderId: "legacy",
      text: "message legacy",
      imageUrl: null,
      createdAt: serverTimestamp(),
      status: "sent",
    }
  );
  messageBatch.set(
    doc(legacyDb, "conversations/legacy_bob/messageOps/legacy"),
    {
      type: "send",
      targetId: "legacy-message-1",
    }
  );
  messageBatch.update(doc(legacyDb, "conversations/legacy_bob"), {
    lastMessageText: "message legacy",
    lastMessageAt: serverTimestamp(),
    lastMessageSenderId: "legacy",
    unreadCount: { legacy: 0, bob: 1 },
  });
  await assertSucceeds(messageBatch.commit());

  const normalizedConversation = await getDoc(
    doc(legacyDb, "conversations/legacy_bob")
  );
  if (!normalizedConversation.exists()) {
    throw new Error("Legacy conversation disappeared unexpectedly");
  }
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
  legacyDb = testEnv.authenticatedContext("legacy").firestore();
  charlieDb = testEnv.authenticatedContext("charlie").firestore();
  daveDb = testEnv.authenticatedContext("dave").firestore();

  await testEnv.withSecurityRulesDisabled(seed);

  await testDirectCounterTampering();
  await testLikeCounterMustMatchLikeMutation();
  await testCommentCounterMustMatchCommentMutation();
  await testFollowerCounterMustMatchFollowMutation();
  await testRepostCounterMustMatchRepostMutation();
  await testCommentDeleteCounterMustMatchDeletion();
  await testUnfollowCounterMustMatchDeletion();
  await testRelationshipSecurity();
  await testLikeAndBookmarkSecurity();
  await testRepostDeleteCounterMustMatchDeletion();
  await testLegacyUserFollowCompatibility();
  await testLegacyConversationUnreadCountCompatibility();
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
