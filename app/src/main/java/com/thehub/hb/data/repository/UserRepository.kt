package com.thehub.hb.data.repository

import android.net.Uri
import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.thehub.hb.data.local.DataStoreManager
import com.thehub.hb.data.model.BlockedUser
import com.thehub.hb.data.model.NotificationItem
import com.thehub.hb.data.model.Post
import com.thehub.hb.data.model.Report
import com.thehub.hb.data.model.User
import com.thehub.hb.data.remote.ImgbbService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class UserRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val dataStoreManager: DataStoreManager,
    private val imgbbService: ImgbbService = ImgbbService(),
    private val notificationRepository: NotificationRepository = NotificationRepository(firestore, auth)
) {
    val currentUserId: String?
        get() = auth.currentUser?.uid

    private fun userRef(uid: String) = firestore.collection("users").document(uid)

    suspend fun getUserProfile(uid: String): Result<User?> = withContext(Dispatchers.IO) {
        try {
            val doc = userRef(uid).get().await()
            if (!doc.exists()) return@withContext Result.success(null)
            val base = User.fromMap(doc.data ?: emptyMap())
            val posts = try {
                firestore.collection("posts").whereEqualTo("authorId", uid)
                    .count().get(AggregateSource.SERVER).await().count.toInt()
            } catch (_: Exception) { base.postsCount }
            val followers = try {
                userRef(uid).collection("followers")
                    .count().get(AggregateSource.SERVER).await().count.toInt()
            } catch (_: Exception) { base.followersCount }
            val following = try {
                userRef(uid).collection("following")
                    .count().get(AggregateSource.SERVER).await().count.toInt()
            } catch (_: Exception) { base.followingCount }
            Result.success(base.copy(postsCount = posts, followersCount = followers, followingCount = following))
        } catch (e: Exception) {
            Log.e("UserRepository", "Erreur profil $uid", e)
            Result.failure(e)
        }
    }

    fun observeUserProfile(uid: String): Flow<User?> = callbackFlow {
        val listener = userRef(uid).addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null || !snapshot.exists()) {
                trySend(null)
                return@addSnapshotListener
            }
            trySend(runCatching { User.fromMap(snapshot.data ?: emptyMap()) }.getOrNull())
        }
        awaitClose { listener.remove() }
    }.flowOn(Dispatchers.IO)

    suspend fun checkUsernameUnique(username: String, currentUid: String): Boolean = withContext(Dispatchers.IO) {
        val lower = username.trim().lowercase()
        if (lower.length !in 3..30 || !lower.matches(Regex("^[a-zA-Z0-9._]+$"))) return@withContext false
        try {
            val claim = firestore.collection("usernames").document(lower).get().await()
            if (claim.exists()) return@withContext claim.getString("uid") == currentUid
            val users = firestore.collection("users").whereEqualTo("usernameLower", lower).limit(2).get().await()
            users.documents.all { it.id == currentUid }
        } catch (e: Exception) {
            Log.e("UserRepository", "Erreur unicité username", e)
            false
        }
    }

    suspend fun uploadProfilePhoto(imageBytes: ByteArray): Result<String> = imgbbService.uploadImage(imageBytes)

    suspend fun updateProfile(
        displayName: String,
        username: String,
        bio: String?,
        birthdate: String?,
        photoUrl: String?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val uid = currentUserId ?: return@withContext Result.failure(Exception("Non connecté"))
        val cleanName = displayName.trim().take(80)
        val cleanUsername = username.trim()
        val newLower = cleanUsername.lowercase()
        if (cleanUsername.length !in 3..30 || !cleanUsername.matches(Regex("^[a-zA-Z0-9._]+$"))) {
            return@withContext Result.failure(Exception("Nom d'utilisateur invalide"))
        }
        try {
            val userDocument = userRef(uid)
            var oldLower: String? = null
            firestore.runTransaction { tx ->
                val current = tx.get(userDocument)
                if (!current.exists() || current.getBoolean("isDeleted") == true) throw IllegalStateException("Compte supprimé")
                oldLower = current.getString("usernameLower") ?: current.getString("username")?.lowercase()
                if (oldLower != newLower) {
                    val claimRef = firestore.collection("usernames").document(newLower)
                    val claim = tx.get(claimRef)
                    val owner = claim.getString("uid")
                    if (claim.exists() && owner != uid) throw IllegalStateException("Ce nom d'utilisateur est déjà pris")
                    tx.set(claimRef, mapOf("uid" to uid, "updatedAt" to Timestamp.now()))
                    if (!oldLower.isNullOrBlank()) {
                        tx.delete(firestore.collection("usernames").document(oldLower!!))
                    }
                }
                val updates = mutableMapOf<String, Any?>(
                    "displayName" to cleanName,
                    "username" to cleanUsername,
                    "usernameLower" to newLower,
                    "bio" to bio?.trim()?.take(400),
                    "birthdate" to birthdate?.trim()
                )
                if (photoUrl != null) updates["photoUrl"] = photoUrl
                tx.set(userDocument, updates, SetOptions.merge())
            }.await()

            auth.currentUser?.let { user ->
                user.updateProfile(userProfileChangeRequest {
                    displayName = cleanName
                    if (photoUrl != null) photoUri = Uri.parse(photoUrl)
                }).await()
            }
            dataStoreManager.saveLastUser(
                email = auth.currentUser?.email ?: "",
                username = cleanUsername,
                name = cleanName,
                photoUrl = photoUrl
            )
            UserCacheRepository.getInstance().putUser(
                com.thehub.hb.data.model.UserInfo(uid, cleanName, cleanUsername, photoUrl)
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("UserRepository", "Erreur mise à jour profil", e)
            Result.failure(e)
        }
    }

    fun isFollowing(targetUid: String): Flow<Boolean> = callbackFlow {
        val uid = currentUserId
        if (uid.isNullOrBlank() || uid == targetUid) {
            trySend(false)
            close()
            return@callbackFlow
        }
        val listener = userRef(uid).collection("following").document(targetUid)
            .addSnapshotListener { snapshot, _ -> trySend(snapshot?.exists() == true) }
        awaitClose { listener.remove() }
    }.flowOn(Dispatchers.IO)

    suspend fun checkIsFollowing(targetUid: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val uid = currentUserId ?: return@withContext Result.success(false)
        if (uid == targetUid) return@withContext Result.success(false)
        try {
            Result.success(userRef(uid).collection("following").document(targetUid).get().await().exists())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun followUser(targetUid: String): Result<Unit> = withContext(Dispatchers.IO) {
        val uid = currentUserId ?: return@withContext Result.failure(Exception("Non connecté"))
        if (uid == targetUid) return@withContext Result.failure(Exception("Impossible de se suivre soi-même"))
        try {
            val me = userRef(uid)
            val target = userRef(targetUid)
            val following = me.collection("following").document(targetUid)
            val follower = target.collection("followers").document(uid)
            val now = Timestamp.now()
            var created = false
            firestore.runTransaction { tx ->
                val meDoc = tx.get(me)
                val targetDoc = tx.get(target)
                if (!meDoc.exists() || !targetDoc.exists() || meDoc.getBoolean("isDeleted") == true || targetDoc.getBoolean("isDeleted") == true) {
                    throw IllegalStateException("Compte indisponible")
                }
                val blockedByMe = tx.get(me.collection("blockedUsers").document(targetUid)).exists()
                val blockedByTarget = tx.get(target.collection("blockedUsers").document(uid)).exists()
                if (blockedByMe || blockedByTarget) throw IllegalStateException("Action impossible entre comptes bloqués")
                if (!tx.get(following).exists()) {
                    tx.set(following, mapOf("followedAt" to now, "followingId" to targetUid, "uid" to targetUid))
                    tx.set(follower, mapOf("followedAt" to now, "followerId" to uid, "uid" to uid))
                    tx.update(me, "followingCount", FieldValue.increment(1))
                    tx.update(target, "followersCount", FieldValue.increment(1))
                    created = true
                }
            }.await()
            if (created) notificationRepository.createNotification(targetUid, NotificationItem.TYPE_FOLLOW)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("UserRepository", "Erreur follow $targetUid", e)
            Result.failure(e)
        }
    }

    suspend fun unfollowUser(targetUid: String): Result<Unit> = withContext(Dispatchers.IO) {
        val uid = currentUserId ?: return@withContext Result.failure(Exception("Non connecté"))
        if (uid == targetUid) return@withContext Result.failure(Exception("Action non permise"))
        try {
            val me = userRef(uid)
            val target = userRef(targetUid)
            val following = me.collection("following").document(targetUid)
            val follower = target.collection("followers").document(uid)
            firestore.runTransaction { tx ->
                if (tx.get(following).exists()) {
                    tx.delete(following)
                    tx.delete(follower)
                    tx.update(me, "followingCount", FieldValue.increment(-1))
                    tx.update(target, "followersCount", FieldValue.increment(-1))
                }
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("UserRepository", "Erreur unfollow $targetUid", e)
            Result.failure(e)
        }
    }

    suspend fun checkIsFriend(targetUid: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val uid = currentUserId ?: return@withContext Result.success(false)
        if (uid == targetUid) return@withContext Result.success(false)
        try {
            val following = userRef(uid).collection("following").document(targetUid).get().await().exists()
            if (!following) return@withContext Result.success(false)
            Result.success(userRef(targetUid).collection("following").document(uid).get().await().exists())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun isFriend(targetUid: String): Flow<Boolean> = callbackFlow {
        val uid = currentUserId
        if (uid.isNullOrBlank() || uid == targetUid) {
            trySend(false)
            close()
            return@callbackFlow
        }
        val listener = userRef(uid).collection("following").document(targetUid)
            .addSnapshotListener { _, _ -> launch { trySend(checkIsFriend(targetUid).getOrDefault(false)) } }
        trySend(checkIsFriend(targetUid).getOrDefault(false))
        awaitClose { listener.remove() }
    }.flowOn(Dispatchers.IO)

    suspend fun getFriends(uid: String): Result<List<User>> = withContext(Dispatchers.IO) {
        try {
            val following = userRef(uid).collection("following").limit(200).get().await().documents.map { it.id }.toSet()
            val followers = userRef(uid).collection("followers").limit(200).get().await().documents.map { it.id }.toSet()
            val ids = following.intersect(followers)
            val users = coroutineScope {
                ids.map { id -> async {
                    userRef(id).get().await().takeIf { it.exists() }?.let { User.fromMap(it.data ?: emptyMap()) }
                }}.awaitAll().filterNotNull()
            }
            Result.success(users.filterNot { it.isDeleted })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getFollowers(uid: String): Result<List<User>> = getUserListFromRelationship(uid, "followers")
    suspend fun getFollowing(uid: String): Result<List<User>> = getUserListFromRelationship(uid, "following")

    private suspend fun getUserListFromRelationship(uid: String, relation: String): Result<List<User>> = withContext(Dispatchers.IO) {
        try {
            val docs = userRef(uid).collection(relation).orderBy("followedAt", Query.Direction.DESCENDING).limit(100).get().await()
            val users = coroutineScope {
                docs.documents.map { doc ->
                    val id = doc.id
                    async { userRef(id).get().await().takeIf { it.exists() }?.let { User.fromMap(it.data ?: emptyMap()) } }
                }.awaitAll().filterNotNull().filterNot { it.isDeleted }
            }
            Result.success(users)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun blockUser(targetUid: String): Result<Unit> = withContext(Dispatchers.IO) {
        val uid = currentUserId ?: return@withContext Result.failure(Exception("Non connecté"))
        if (uid == targetUid) return@withContext Result.failure(Exception("Impossible de se bloquer soi-même"))
        try {
            val me = userRef(uid)
            val target = userRef(targetUid)
            val following = me.collection("following").document(targetUid)
            val follower = target.collection("followers").document(uid)
            firestore.runTransaction { tx ->
                tx.set(me.collection("blockedUsers").document(targetUid), mapOf("blockedAt" to Timestamp.now()))
                if (tx.get(following).exists()) {
                    tx.delete(following)
                    tx.delete(follower)
                    tx.update(me, "followingCount", FieldValue.increment(-1))
                    tx.update(target, "followersCount", FieldValue.increment(-1))
                }
            }.await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun unblockUser(targetUid: String): Result<Unit> = withContext(Dispatchers.IO) {
        val uid = currentUserId ?: return@withContext Result.failure(Exception("Non connecté"))
        try {
            userRef(uid).collection("blockedUsers").document(targetUid).delete().await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getBlockedUsers(): Result<List<BlockedUser>> = withContext(Dispatchers.IO) {
        val uid = currentUserId ?: return@withContext Result.success(emptyList())
        try {
            val docs = userRef(uid).collection("blockedUsers").orderBy("blockedAt", Query.Direction.DESCENDING).get().await()
            val result = coroutineScope {
                docs.documents.map { doc ->
                    async {
                        val id = doc.id
                        val at = doc.getTimestamp("blockedAt") ?: Timestamp.now()
                        val user = userRef(id).get().await()
                        BlockedUser(id, user.getString("username") ?: "thehub_user", user.getString("displayName"), user.getString("photoUrl"), at)
                    }
                }.awaitAll()
            }
            Result.success(result)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getBlockedUserIds(): Set<String> = withContext(Dispatchers.IO) {
        val uid = currentUserId ?: return@withContext emptySet()
        runCatching { userRef(uid).collection("blockedUsers").get().await().documents.map { it.id }.toSet() }.getOrDefault(emptySet())
    }

    suspend fun isBlocked(targetUid: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val uid = currentUserId ?: return@withContext Result.success(false)
        try {
            val mine = userRef(uid).collection("blockedUsers").document(targetUid).get().await().exists()
            val theirs = userRef(targetUid).collection("blockedUsers").document(uid).get().await().exists()
            Result.success(mine || theirs)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun isBlockedByMe(targetUid: String): Boolean = withContext(Dispatchers.IO) {
        val uid = currentUserId ?: return@withContext false
        runCatching { userRef(uid).collection("blockedUsers").document(targetUid).get().await().exists() }.getOrDefault(false)
    }

    suspend fun reportContent(targetType: String, targetId: String, reason: String, details: String? = null): Result<Unit> = withContext(Dispatchers.IO) {
        val uid = currentUserId ?: return@withContext Result.failure(Exception("Non connecté"))
        try {
            val ref = firestore.collection("reports").document()
            ref.set(Report(ref.id, uid, targetType, targetId.trim(), reason.trim(), details?.trim()?.takeIf { it.isNotBlank() }, Timestamp.now(), "pending").toMap()).await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getUserPosts(userId: String): Result<List<Post>> = withContext(Dispatchers.IO) {
        if (userId.isBlank()) return@withContext Result.success(emptyList())
        try {
            val currentUid = currentUserId
            val bookmarked = currentUid?.let { runCatching { userRef(it).collection("bookmarks").get().await().documents.map { d -> d.id }.toSet() }.getOrDefault(emptySet()) } ?: emptySet()
            val snap = firestore.collection("posts").whereEqualTo("authorId", userId).limit(100).get().await()
            val posts = coroutineScope {
                snap.documents.map { doc ->
                    async {
                        val post = Post.fromSnapshot(doc, currentUid)
                        val liked = currentUid?.let { runCatching { doc.reference.collection("likes").document(it).get().await().exists() }.getOrDefault(false) } ?: false
                        post.copy(isLikedByCurrentUser = liked, isBookmarkedByCurrentUser = bookmarked.contains(post.id))
                    }
                }.awaitAll()
            }.sortedByDescending { it.createdAt.toDate().time }
            Result.success(posts)
        } catch (e: Exception) { Result.failure(e) }
    }

    fun observeUserPosts(userId: String): Flow<List<Post>> = callbackFlow {
        if (userId.isBlank()) {
            trySend(emptyList()); close(); return@callbackFlow
        }
        val listener = firestore.collection("posts").whereEqualTo("authorId", userId).limit(100)
            .addSnapshotListener { _, error ->
                if (error != null) return@addSnapshotListener
                launch(Dispatchers.IO) { getUserPosts(userId).onSuccess { trySend(it) } }
            }
        awaitClose { listener.remove() }
    }.flowOn(Dispatchers.IO)

    suspend fun deletePost(postId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val uid = currentUserId ?: return@withContext Result.failure(Exception("Non connecté"))
        try {
            val ref = firestore.collection("posts").document(postId)
            val post = ref.get().await()
            if (!post.exists() || post.getString("authorId") != uid) return@withContext Result.failure(Exception("Publication introuvable ou non autorisée"))
            val likes = ref.collection("likes").get().await().documents
            likes.chunked(400).forEach { chunk ->
                val batch = firestore.batch(); chunk.forEach { batch.delete(it.reference) }; batch.commit().await()
            }
            val comments = ref.collection("comments").get().await().documents
            comments.forEach { comment ->
                val commentLikes = comment.reference.collection("likes").get().await().documents
                commentLikes.chunked(200).forEach { chunk ->
                    val batch = firestore.batch(); chunk.forEach { batch.delete(it.reference) }; batch.commit().await()
                }
            }
            comments.chunked(300).forEach { chunk ->
                val batch = firestore.batch(); chunk.forEach { batch.delete(it.reference) }; batch.commit().await()
            }
            firestore.runTransaction { tx ->
                if (tx.get(ref).exists()) {
                    tx.delete(ref)
                    tx.update(userRef(uid), "postsCount", FieldValue.increment(-1))
                }
            }.await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun deleteAccount(): Result<Unit> = withContext(Dispatchers.IO) {
        val uid = currentUserId ?: return@withContext Result.failure(Exception("Non connecté"))
        val authUser = auth.currentUser ?: return@withContext Result.failure(Exception("Utilisateur introuvable"))
        try {
            val me = userRef(uid)
            val current = me.get().await()
            val oldLower = current.getString("usernameLower") ?: current.getString("username")?.lowercase()
            val followingDocs = me.collection("following").limit(400).get().await().documents
            for (doc in followingDocs) {
                val targetId = doc.id
                val target = userRef(targetId)
                val followerRef = target.collection("followers").document(uid)
                runCatching {
                    firestore.runTransaction { tx ->
                        tx.delete(doc.reference)
                        if (tx.get(followerRef).exists()) tx.delete(followerRef)
                        if (tx.get(target).exists()) tx.update(target, "followersCount", FieldValue.increment(-1))
                    }.await()
                }
            }
            me.collection("blockedUsers").get().await().documents.chunked(400).forEach { chunk ->
                val batch = firestore.batch(); chunk.forEach { batch.delete(it.reference) }; batch.commit().await()
            }
            me.collection("bookmarks").get().await().documents.chunked(400).forEach { chunk ->
                val batch = firestore.batch(); chunk.forEach { batch.delete(it.reference) }; batch.commit().await()
            }
            firestore.collection("posts").whereEqualTo("authorId", uid).limit(100).get().await().documents.forEach { post ->
                runCatching { deletePost(post.id) }
            }

            firestore.runTransaction { tx ->
                if (!oldLower.isNullOrBlank()) {
                    val claimRef = firestore.collection("usernames").document(oldLower!!)
                    val claim = tx.get(claimRef)
                    if (claim.getString("uid") == uid) tx.delete(claimRef)
                }
                val tombstone = "compte_supprime_${uid.take(8)}"
                tx.set(me, mapOf(
                    "isDeleted" to true,
                    "displayName" to "Compte supprimé",
                    "username" to tombstone,
                    "usernameLower" to tombstone,
                    "bio" to null,
                    "birthdate" to null,
                    "photoUrl" to null,
                    "email" to null
                ), SetOptions.merge())
            }.await()

            runCatching { dataStoreManager.clearAll() }
            UserCacheRepository.getInstance().clear()
            runCatching { authUser.delete().await() }
            auth.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("UserRepository", "Erreur suppression compte", e)
            auth.signOut()
            runCatching { dataStoreManager.clearAll() }
            Result.failure(e)
        }
    }
}
