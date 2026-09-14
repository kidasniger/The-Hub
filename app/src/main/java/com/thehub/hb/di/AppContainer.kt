package com.thehub.hb.di

import android.content.Context
import com.thehub.hb.data.local.DataStoreManager
import com.thehub.hb.data.remote.ImgbbService
import com.thehub.hb.data.repository.AuthRepository
import com.thehub.hb.data.repository.MessageRepository
import com.thehub.hb.data.repository.NotificationRepository
import com.thehub.hb.data.repository.PostRepository
import com.thehub.hb.data.repository.SearchRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

interface AppContainer {
    val dataStoreManager: DataStoreManager
    val authRepository: AuthRepository
    val postRepository: PostRepository
    val messageRepository: MessageRepository
    val notificationRepository: NotificationRepository
    val searchRepository: SearchRepository
}

class DefaultAppContainer(private val context: Context) : AppContainer {
    private val firestoreInstance: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val authInstance: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val imgbbServiceInstance: ImgbbService by lazy { ImgbbService() }

    override val dataStoreManager: DataStoreManager by lazy {
        DataStoreManager(context)
    }

    override val notificationRepository: NotificationRepository by lazy {
        NotificationRepository(
            firestore = firestoreInstance,
            auth = authInstance
        )
    }

    override val searchRepository: SearchRepository by lazy {
        SearchRepository(
            firestore = firestoreInstance,
            auth = authInstance,
            dataStoreManager = dataStoreManager
        )
    }

    override val authRepository: AuthRepository by lazy {
        AuthRepository(
            auth = authInstance,
            firestore = firestoreInstance,
            imgbbService = imgbbServiceInstance,
            dataStoreManager = dataStoreManager
        )
    }

    override val postRepository: PostRepository by lazy {
        PostRepository(
            firestore = firestoreInstance,
            auth = authInstance,
            imgbbService = imgbbServiceInstance,
            notificationRepository = notificationRepository
        )
    }

    override val messageRepository: MessageRepository by lazy {
        MessageRepository(
            firestore = firestoreInstance,
            auth = authInstance,
            imgbbService = imgbbServiceInstance,
            notificationRepository = notificationRepository
        )
    }
}
