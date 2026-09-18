package com.thehub.hb.utils

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class RemoteImageVerifierTest {

    @Test
    fun imgBbUrlDetectionAcceptsImgBbHostsAndRejectsOtherHosts() {
        assertEquals(true, RemoteImageVerifier.isImgBbUrl("https://i.ibb.co/abc/image.jpg"))
        assertEquals(true, RemoteImageVerifier.isImgBbUrl("https://imgbb.com/abc"))
        assertEquals(true, RemoteImageVerifier.isImgBbUrl("https://sub.imgbb.com/abc"))
        assertEquals(false, RemoteImageVerifier.isImgBbUrl("https://example.com/image.jpg"))
    }

    @Test
    fun successfulHeadResponseMeansImageIsAvailable() = runBlocking {
        val verifier = RemoteImageVerifier(fakeClient(200))

        assertEquals(
            true,
            verifier.checkIfAvailable("https://i.ibb.co/abc/image.jpg", force = true)
        )
    }

    @Test
    fun notFoundAndGoneMeanImageIsUnavailable() = runBlocking {
        assertEquals(
            false,
            RemoteImageVerifier(fakeClient(404))
                .checkIfAvailable("https://i.ibb.co/abc/image.jpg", force = true)
        )
        assertEquals(
            false,
            RemoteImageVerifier(fakeClient(410))
                .checkIfAvailable("https://i.ibb.co/abc/image.jpg", force = true)
        )
    }

    @Test
    fun temporaryServerErrorDoesNotHideCachedImage() = runBlocking {
        assertEquals(
            null,
            RemoteImageVerifier(fakeClient(503))
                .checkIfAvailable("https://i.ibb.co/abc/image.jpg", force = true)
        )
    }

    @Test
    fun unsupportedHeadFallsBackToGet() = runBlocking {
        val requests = AtomicInteger(0)
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val code = if (requests.getAndIncrement() == 0) 405 else 206
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(code)
                    .message("test")
                    .body(ByteArray(0).toResponseBody(null))
                    .build()
            }
            .build()

        assertEquals(
            true,
            RemoteImageVerifier(client)
                .checkIfAvailable("https://i.ibb.co/abc/image.jpg", force = true)
        )
        assertEquals(2, requests.get())
    }

    private fun fakeClient(code: Int): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(code)
                    .message("test")
                    .body(ByteArray(0).toResponseBody(null))
                    .build()
            }
            .build()
    }
}
