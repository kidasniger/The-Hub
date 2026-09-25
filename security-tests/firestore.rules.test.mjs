import fs from "node:fs";
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from "@firebase/rules-unit-testing";
import {
  deleteDoc,
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
let eveDb;
let anonDb;

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

  await setDoc(doc(db, "posts/post-hidden"), {
    authorId: "alice",
    authorUsername: "alice",
    text: "hidden",
    createdAt: new Date(),
    likesCount: 0,
    commentsCount: 0,
    repostsCount: 0,
    isRepost: false,
    isHidden: true,
  });

  await setDoc(doc(db, "posts/post-deleted"), {
    authorId: "alice",
    authorUsername: "alice",
    text: "deleted",
    createdAt: new Date(),
    likesCount: 0,
    commentsCount: 0,
    repostsCount: 0,
    isRepost: false,
    isDeletedByAdmin: true,
  });
}

async function testPublicPostPreviewReadSecurity() {
  // Public publication documents are readable without authentication.
  await assertSucceeds(getDoc(doc(anonDb, "posts/post-1")));

  // Moderated/hidden publications stay inaccessible to anonymous visitors.
  await assertFails(getDoc(doc(anonDb, "posts/post-hidden")));
  await assertFails(getDoc(doc(anonDb, "posts/post-deleted")));

  // Public read access is read-only; anonymous clients cannot write posts.
  await assertFails(setDoc(doc(anonDb, "posts/anon-post"), {
    authorId: "anon",
    authorUsername: "anonymous",
    text: "forbidden",
    createdAt: new Date(),
    likesCount: 0,
    commentsCount: 0,
    repostsCount: 0,
    isRepost: false,
  }));
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
  otherUserDelete.delete(
    doc(bobDb, "posts/post-1/likes/charlie")
  );
  await assertFails(otherUserDelete.commit());

  const deleteLike = writeBatch(charlieDb);
  deleteLike.delete(likeRef);
  deleteLike.update(doc(charlieDb, "posts/post-1"), { likesCount: 1 });
  await assertSucceeds(deleteLike.commit());

  const bookmarkRef = doc(charlieDb, "users/charlie/bookmarks/post-1");
  await assertSucceeds(setDoc(bookmarkRef, { savedAt: new Date() }));
  await assertFails(getDoc(doc(bobDb, "users/charlie/bookmarks/post-1")));
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
  otherUserBookmarkDelete.delete(
    doc(bobDb, "users/charlie/bookmarks/post-1")
  );
  await assertFails(otherUserBookmarkDelete.commit());

  const deleteBookmark = writeBatch(charlieDb);
  deleteBookmark.delete(bookmarkRef);
  await assertSucceeds(deleteBookmark.commit());
}

async function establishMessagingRelation(db, followerId, targetId) {
  const followerRef = doc(db, `users/${followerId}`);
  const targetRef = doc(db, `users/${targetId}`);
  const [followerSnap, targetSnap] = await Promise.all([
    getDoc(followerRef),
    getDoc(targetRef),
  ]);

  const followerCount = Number(followerSnap.data()?.followingCount ?? 0);
  const targetCount = Number(targetSnap.data()?.followersCount ?? 0);

  const batch = writeBatch(db);
  batch.set(doc(db, `users/${followerId}/following/${targetId}`), {
    followedAt: new Date(),
    followingId: targetId,
    uid: targetId,
  });
  batch.set(doc(db, `users/${targetId}/followers/${followerId}`), {
    followedAt: new Date(),
    followerId,
    uid: followerId,
  });
  batch.set(doc(db, `users/${followerId}/followOps/${followerId}`), {
    type: "follow",
    targetId,
  });
  batch.update(followerRef, { followingCount: followerCount + 1 });
  batch.update(targetRef, { followersCount: targetCount + 1 });

  await assertSucceeds(batch.commit());
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
  await establishMessagingRelation(legacyDb, "legacy", "bob");

  // Continue with the legacy conversation send path.
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
  await establishMessagingRelation(alice(), "alice", "bob");

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

async function testCurrentAppMessageBatchWithoutMessageOps() {
  // The current Android sender writes the message independently. Conversation
  // summary/unread metadata must never be able to reject the actual message.
  const messageRef = doc(
    legacyDb,
    "conversations/legacy_bob/messages/current-app-message"
  );

  await assertSucceeds(setDoc(messageRef, {
    senderId: "legacy",
    text: "current app send",
    imageUrl: null,
    createdAt: serverTimestamp(),
    status: "sent",
    replyToMessageId: null,
    replyToText: null,
    isDeleted: false,
    editedAt: null,
    deletedAt: null,
    deletedBy: null,
  }));

  const saved = await getDoc(messageRef);
  if (!saved.exists()) {
    throw new Error("Current-app message was not persisted.");
  }
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

async function testAdvancedMessengerSecurity() {
  const advancedMessageRef = doc(
    aliceDb,
    "conversations/alice_bob/messages/advanced-message"
  );
  const advancedSend = writeBatch(aliceDb);
  advancedSend.set(advancedMessageRef, {
    senderId: "alice",
    text: "advanced messenger test",
    imageUrl: null,
    createdAt: serverTimestamp(),
    status: "sent",
    replyToMessageId: "message-1",
    replyToText: "hello",
    isDeleted: false,
  });
  advancedSend.set(
    doc(aliceDb, "conversations/alice_bob/messageOps/alice"),
    {
      type: "send",
      targetId: "advanced-message",
    }
  );
  advancedSend.update(
    doc(aliceDb, "conversations/alice_bob"),
    {
      lastMessageText: "advanced messenger test",
      lastMessageAt: serverTimestamp(),
      lastMessageSenderId: "alice",
      "unreadCount.bob": 1,
    }
  );
  await assertSucceeds(advancedSend.commit());

  const presenceRef = doc(aliceDb, "users/alice/presence/current");
  await assertSucceeds(setDoc(presenceRef, {
    userId: "alice",
    online: true,
    lastSeen: new Date(),
  }));
  await assertSucceeds(updateDoc(presenceRef, {
    online: false,
    lastSeen: new Date(),
  }));
  await assertFails(updateDoc(
    doc(bobDb, "users/alice/presence/current"),
    { online: true }
  ));

  const typingRef = doc(
    aliceDb,
    "conversations/alice_bob/typing/alice"
  );
  await assertSucceeds(setDoc(typingRef, {
    userId: "alice",
    typing: true,
    updatedAt: new Date(),
  }));
  await assertSucceeds(deleteDoc(typingRef));
  await assertFails(setDoc(
    doc(bobDb, "conversations/alice_bob/typing/alice"),
    {
      userId: "bob",
      typing: true,
      updatedAt: new Date(),
    }
  ));

  const reactionRef = doc(
    aliceDb,
    "conversations/alice_bob/reactions/advanced-message_alice"
  );
  await assertSucceeds(setDoc(reactionRef, {
    messageId: "advanced-message",
    userId: "alice",
    emoji: "👍",
    updatedAt: new Date(),
  }));
  await assertSucceeds(updateDoc(reactionRef, {
    emoji: "❤️",
    updatedAt: new Date(),
  }));
  await assertFails(updateDoc(
    doc(bobDb, "conversations/alice_bob/reactions/advanced-message_alice"),
    { emoji: "🔥" }
  ));
  await assertSucceeds(deleteDoc(reactionRef));

  await assertSucceeds(updateDoc(
    doc(bobDb, "conversations/alice_bob/messages/advanced-message"),
    { status: "delivered" }
  ));
  await assertSucceeds(updateDoc(
    doc(bobDb, "conversations/alice_bob/messages/advanced-message"),
    { status: "read" }
  ));

  await assertSucceeds(updateDoc(
    advancedMessageRef,
    {
      text: "edited",
      editedAt: serverTimestamp(),
    }
  ));

  await assertSucceeds(updateDoc(
    advancedMessageRef,
    {
      text: null,
      imageUrl: null,
      isDeleted: true,
      deletedAt: serverTimestamp(),
      deletedBy: "alice",
    }
  ));

  await assertFails(updateDoc(
    doc(bobDb, "conversations/alice_bob/messages/advanced-message"),
    {
      text: null,
      imageUrl: null,
      isDeleted: true,
      deletedAt: new Date(),
      deletedBy: "bob",
    }
  ));
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


async function testSensitiveCollectionWrites() {
  // Every write below is intentionally isolated from the earlier relationship
  // and counter scenarios so the matrix does not depend on a mutable fixture.

  // users/{userId}: owner create/update/delete; forged owner is denied.
  const eveUserRef = doc(eveDb, "users/eve");
  await assertSucceeds(setDoc(eveUserRef, {
    uid: "eve",
    username: "eve",
    followersCount: 0,
    followingCount: 0,
  }));
  await assertFails(setDoc(doc(bobDb, "users/eve"), {
    uid: "eve",
    username: "forged",
    followersCount: 0,
    followingCount: 0,
  }));
  await assertSucceeds(updateDoc(eveUserRef, {
    username: "eve-updated",
  }));
  await assertFails(updateDoc(
    doc(bobDb, "users/eve"),
    { username: "bob-forged" }
  ));
  await assertFails(updateDoc(
    eveUserRef,
    { uid: "bob" }
  ));

  // usernames/{usernameLower}: owner create/update/delete; another user denied.
  const usernameRef = doc(eveDb, "usernames/eve-security");
  await assertSucceeds(setDoc(usernameRef, {
    uid: "eve",
    username: "eve-security",
  }));
  await assertFails(setDoc(doc(bobDb, "usernames/eve-security-forged"), {
    uid: "eve",
    username: "forged",
  }));
  await assertSucceeds(updateDoc(usernameRef, {
    username: "eve-security-renamed",
  }));
  await assertFails(updateDoc(
    doc(bobDb, "usernames/eve-security"),
    { username: "bob-forged" }
  ));

  // users/{userId}/blockedUsers: only the owner may write the block list.
  const blockedRef = doc(eveDb, "users/eve/blockedUsers/bob");
  await assertSucceeds(setDoc(blockedRef, {
    blockedAt: new Date(),
  }));
  await assertSucceeds(updateDoc(blockedRef, {
    reason: "security-test",
  }));
  await assertFails(setDoc(
    doc(bobDb, "users/eve/blockedUsers/charlie"),
    { blockedAt: new Date() }
  ));
  const deleteBlocked = writeBatch(eveDb);
  deleteBlocked.delete(blockedRef);
  await assertSucceeds(deleteBlocked.commit());

  // reports: only the authenticated reporter may create a report for itself;
  // reports cannot be edited/deleted by clients.
  const reportRef = doc(eveDb, "reports/eve-security");
  await assertSucceeds(setDoc(reportRef, {
    reporterId: "eve",
    targetId: "alice",
    reason: "test",
  }));
  await assertFails(setDoc(doc(bobDb, "reports/forged-reporter"), {
    reporterId: "eve",
    targetId: "alice",
    reason: "forged",
  }));
  await assertFails(updateDoc(reportRef, { reason: "changed" }));
  const deleteReport = writeBatch(eveDb);
  deleteReport.delete(reportRef);
  await assertFails(deleteReport.commit());

  // posts: owner create/update/delete; forged author and outsider writes fail.
  const postRef = doc(eveDb, "posts/eve-security-post");
  await assertSucceeds(setDoc(postRef, {
    authorId: "eve",
    authorUsername: "eve",
    text: "security fixture",
    createdAt: new Date(),
    likesCount: 0,
    commentsCount: 0,
    repostsCount: 0,
    isRepost: false,
  }));
  await assertFails(setDoc(doc(bobDb, "posts/eve-forged-post"), {
    authorId: "eve",
    authorUsername: "eve",
    text: "forged",
    createdAt: new Date(),
    likesCount: 0,
    commentsCount: 0,
    repostsCount: 0,
    isRepost: false,
  }));
  await assertSucceeds(updateDoc(postRef, {
    text: "edited by owner",
  }));
  await assertFails(updateDoc(
    doc(bobDb, "posts/eve-security-post"),
    { text: "edited by outsider" }
  ));
  await assertFails(updateDoc(
    postRef,
    { authorId: "bob" }
  ));
  await assertFails(updateDoc(
    postRef,
    { likesCount: 999 }
  ));
  await assertFails(setDoc(
    doc(anonDb, "posts/anon-post"),
    {
      authorId: "anon",
      text: "unauthenticated",
      createdAt: new Date(),
      likesCount: 0,
      commentsCount: 0,
      repostsCount: 0,
      isRepost: false,
    }
  ));

  // bookmarks: owner create/update/delete; target and extra fields are checked.
  const bookmarkRef = doc(eveDb, "users/eve/bookmarks/eve-security-post");
  await assertSucceeds(setDoc(bookmarkRef, { savedAt: new Date() }));
  await assertFails(getDoc(doc(bobDb, "users/eve/bookmarks/eve-security-post")));
  await assertFails(setDoc(
    doc(bobDb, "users/eve/bookmarks/eve-security-post"),
    { savedAt: new Date() }
  ));
  await assertFails(setDoc(
    doc(eveDb, "users/eve/bookmarks/missing-target"),
    { savedAt: new Date() }
  ));
  await assertFails(setDoc(bookmarkRef, {
    savedAt: new Date(),
    forged: true,
  }));
  await assertSucceeds(updateDoc(bookmarkRef, {
    savedAt: new Date(),
  }));
  await assertFails(updateDoc(
    bookmarkRef,
    { forged: true }
  ));

  // comments: atomic create/delete, owner-only content edit, outsider denied.
  const commentRef = doc(
    eveDb,
    "posts/eve-security-post/comments/eve-security-comment"
  );
  const createComment = writeBatch(eveDb);
  createComment.set(commentRef, {
    authorId: "eve",
    authorUsername: "eve",
    text: "comment",
    createdAt: new Date(),
    likesCount: 0,
  });
  createComment.set(
    doc(eveDb, "posts/eve-security-post/counterOps/eve"),
    {
      type: "comment_create",
      targetId: "eve-security-comment",
    }
  );
  createComment.update(postRef, { commentsCount: 1 });
  await assertSucceeds(createComment.commit());

  await assertSucceeds(updateDoc(commentRef, {
    text: "edited comment",
    isEdited: true,
    editedAt: new Date(),
  }));
  await assertFails(updateDoc(
    doc(bobDb, "posts/eve-security-post/comments/eve-security-comment"),
    { text: "outsider edit" }
  ));
  await assertFails(updateDoc(commentRef, {
    authorId: "bob",
  }));
  await assertFails(updateDoc(commentRef, {
    likesCount: 999,
  }));

  // comment likes: any authenticated user may create its own like, but only
  // the like owner may remove it; identity/update forgery is denied.
  const bobCommentLikeRef = doc(
    bobDb,
    "posts/eve-security-post/comments/eve-security-comment/likes/bob"
  );
  await assertFails(setDoc(
    doc(bobDb, "posts/eve-security-post/comments/eve-security-comment/likes/eve"),
    { likedAt: new Date() }
  ));
  await assertFails(setDoc(bobCommentLikeRef, {
    likedAt: new Date(),
    forged: true,
  }));

  const createCommentLike = writeBatch(bobDb);
  createCommentLike.set(bobCommentLikeRef, {
    likedAt: new Date(),
  });
  createCommentLike.update(
    doc(bobDb, "posts/eve-security-post/comments/eve-security-comment"),
    { likesCount: 1 }
  );
  await assertSucceeds(createCommentLike.commit());

  await assertFails(updateDoc(bobCommentLikeRef, {
    likedAt: new Date(),
  }));
  const eveDeleteBobLike = writeBatch(eveDb);
  eveDeleteBobLike.delete(
    doc(eveDb, "posts/eve-security-post/comments/eve-security-comment/likes/bob")
  );
  await assertFails(eveDeleteBobLike.commit());

  const deleteCommentLike = writeBatch(bobDb);
  deleteCommentLike.delete(bobCommentLikeRef);
  deleteCommentLike.update(
    doc(bobDb, "posts/eve-security-post/comments/eve-security-comment"),
    { likesCount: 0 }
  );
  await assertSucceeds(deleteCommentLike.commit());

  const deleteComment = writeBatch(eveDb);
  deleteComment.set(
    doc(eveDb, "posts/eve-security-post/counterOps/eve"),
    {
      type: "comment_delete",
      targetId: "eve-security-comment",
    }
  );
  deleteComment.delete(commentRef);
  deleteComment.update(postRef, { commentsCount: 0 });
  await assertSucceeds(deleteComment.commit());

  // Internal relationship/counter/message markers are not client-deletable.
  const markerDelete = writeBatch(eveDb);
  markerDelete.delete(
    doc(eveDb, "users/charlie/followOps/charlie")
  );
  markerDelete.delete(
    doc(eveDb, "posts/post-1/counterOps/bob")
  );
  markerDelete.delete(
    doc(eveDb, "conversations/alice_bob/messageOps/alice")
  );
  await assertFails(markerDelete.commit());

  // followOps: direct fabricated operation cannot be created without the
  // corresponding following document.
  await assertFails(setDoc(
    doc(eveDb, "users/eve/followOps/eve"),
    { type: "follow", targetId: "alice" }
  ));

  // reposts: only the reposter can create/delete its marker as part of an
  // atomic repost; update is not permitted.
  const repostRef = doc(eveDb, "posts/eve-security-repost");
  const repostMarkerRef = doc(eveDb, "posts/post-1/reposts/eve");
  const createRepost = writeBatch(eveDb);
  createRepost.set(repostRef, {
    authorId: "eve",
    authorUsername: "eve",
    text: "",
    createdAt: new Date(),
    likesCount: 0,
    commentsCount: 0,
    repostsCount: 0,
    isRepost: true,
    originalPostId: "post-1",
  });
  createRepost.set(repostMarkerRef, {
    postId: "post-1",
    reposterId: "eve",
    repostId: "eve-security-repost",
    createdAt: new Date(),
  });
  createRepost.update(
    doc(eveDb, "posts/post-1"),
    { repostsCount: 1 }
  );
  await assertSucceeds(createRepost.commit());

  await assertFails(updateDoc(repostMarkerRef, {
    repostId: "forged-repost",
  }));
  const wrongReposterDelete = writeBatch(bobDb);
  wrongReposterDelete.delete(
    doc(bobDb, "posts/post-1/reposts/eve")
  );
  await assertFails(wrongReposterDelete.commit());

  const deleteRepost = writeBatch(eveDb);
  deleteRepost.delete(repostRef);
  deleteRepost.delete(repostMarkerRef);
  deleteRepost.update(
    doc(eveDb, "posts/post-1"),
    { repostsCount: 0 }
  );
  await assertSucceeds(deleteRepost.commit());

  // conversations: participant can create/delete; a nonparticipant cannot
  // create a conversation that excludes itself or delete one it is not in.
  await establishMessagingRelation(eveDb, "eve", "alice");

  const conversationRef = doc(eveDb, "conversations/eve_security_alice");
  await assertFails(setDoc(
    doc(bobDb, "conversations/eve_security_forbidden"),
    {
      participantIds: ["eve", "alice"],
      participantsInfo: {
        eve: { username: "eve" },
        alice: { username: "alice" },
      },
      lastMessageText: "",
      lastMessageAt: new Date(),
      lastMessageSenderId: "",
      unreadCount: { eve: 0, alice: 0 },
    }
  ));
  await assertSucceeds(setDoc(conversationRef, {
    participantIds: ["eve", "alice"],
    participantsInfo: {
      eve: { username: "eve" },
      alice: { username: "alice" },
    },
    lastMessageText: "",
    lastMessageAt: new Date(),
    lastMessageSenderId: "",
    unreadCount: { eve: 0, alice: 0 },
  }));
  await assertFails(updateDoc(
    doc(bobDb, "conversations/eve_security_alice"),
    { lastMessageText: "intrusion" }
  ));
  const outsiderDeleteConversation = writeBatch(bobDb);
  outsiderDeleteConversation.delete(
    doc(bobDb, "conversations/eve_security_alice")
  );
  await assertFails(outsiderDeleteConversation.commit());

  const deleteConversation = writeBatch(eveDb);
  deleteConversation.delete(conversationRef);
  await assertSucceeds(deleteConversation.commit());

  // notifications: actor may create for someone else; only recipient may
  // change isRead; deletion and identity/content edits are denied.
  const notificationRef = doc(eveDb, "notifications/eve-security-notification");
  await assertSucceeds(setDoc(notificationRef, {
    actorId: "eve",
    recipientId: "bob",
    type: "follow",
    isRead: false,
  }));
  await assertFails(setDoc(
    doc(bobDb, "notifications/bob-security-forged"),
    {
      actorId: "eve",
      recipientId: "bob",
      type: "follow",
      isRead: false,
    }
  ));
  await assertFails(setDoc(
    doc(eveDb, "notifications/eve-self-notification"),
    {
      actorId: "eve",
      recipientId: "eve",
      type: "follow",
      isRead: false,
    }
  ));
  await assertFails(updateDoc(
    notificationRef,
    { actorId: "bob" }
  ));
  await assertSucceeds(updateDoc(
    doc(bobDb, "notifications/eve-security-notification"),
    { isRead: true }
  ));
  await assertFails(updateDoc(
    doc(bobDb, "notifications/eve-security-notification"),
    { type: "message" }
  ));
  const deleteNotification = writeBatch(bobDb);
  deleteNotification.delete(
    doc(bobDb, "notifications/eve-security-notification")
  );
  await assertFails(deleteNotification.commit());

  // FCM device tokens: only the owner can register or delete a token;
  // clients cannot read token credentials or write another user's tokens.
  const fcmTokenRef = doc(eveDb, "users/eve/fcmTokens/device-token-1");
  await assertSucceeds(setDoc(fcmTokenRef, {
    token: "test-fcm-token",
    platform: "android",
    updatedAt: new Date(),
  }));
  await assertFails(getDoc(
    doc(bobDb, "users/eve/fcmTokens/device-token-1")
  ));
  await assertFails(setDoc(
    doc(bobDb, "users/eve/fcmTokens/forged-token"),
    {
      token: "forged-token",
      platform: "android",
      updatedAt: new Date(),
    }
  ));
  await assertSucceeds(updateDoc(
    fcmTokenRef,
    {
      token: "rotated-fcm-token",
      platform: "android",
      updatedAt: new Date(),
    }
  ));
  await assertSucceeds(deleteDoc(fcmTokenRef));

  // Representative unauthenticated writes must be denied as well.
  await assertFails(setDoc(
    doc(anonDb, "users/eve"),
    { uid: "eve" }
  ));
  await assertFails(updateDoc(
    doc(anonDb, "users/alice"),
    { username: "anon-forged" }
  ));
  await assertFails(setDoc(
    doc(anonDb, "reports/anon-report"),
    { reporterId: "anon" }
  ));
  await assertFails(setDoc(
    doc(anonDb, "users/alice/bookmarks/post-1"),
    { savedAt: new Date() }
  ));
  await assertFails(setDoc(
    doc(anonDb, "posts/post-1/likes/anon"),
    { likedAt: new Date() }
  ));

  // Cleanup only after every authorized write has been verified. This also
  // explicitly verifies that the owner can delete the profile and username.
  const cleanup = writeBatch(eveDb);
  cleanup.delete(usernameRef);
  cleanup.delete(eveUserRef);
  await assertSucceeds(cleanup.commit());

  const deleteBookmark = writeBatch(eveDb);
  deleteBookmark.delete(bookmarkRef);
  await assertSucceeds(deleteBookmark.commit());

  const deletePost = writeBatch(eveDb);
  deletePost.delete(postRef);
  await assertSucceeds(deletePost.commit());
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
  eveDb = testEnv.authenticatedContext("eve").firestore();
  anonDb = testEnv.unauthenticatedContext().firestore();

  await testEnv.withSecurityRulesDisabled(seed);
  await testPublicPostPreviewReadSecurity();
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
  await testCurrentAppMessageBatchWithoutMessageOps();
  await testMessageCreationRejectsForgedMetadata();
  await testMessageRecipientCanMarkReadOnly();
  await testAdvancedMessengerSecurity();
  await testMessageAccessIsLimitedToParticipants();
  await testMessageDeletionMustBeAtomicWithConversationDeletion();
  await testSensitiveCollectionWrites();

  console.log("Firestore security tests (counters + messages): PASS");
} finally {
  if (testEnv) {
    await testEnv.cleanup();
  }
}
