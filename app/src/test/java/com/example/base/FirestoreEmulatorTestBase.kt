package com.example.base

import android.content.Context
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.example.R
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.MemoryCacheSettings
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@LooperMode(LooperMode.Mode.PAUSED)
abstract class FirestoreEmulatorTestBase {

    protected lateinit var firestore: FirebaseFirestore
    protected lateinit var auth: FirebaseAuth
    protected lateinit var databaseId: String

    companion object {
        private const val PROJECT_ID = "demo-no-project"
        private const val FIRESTORE_HOST = "127.0.0.1"
        private const val FIRESTORE_PORT = 8085
        private const val AUTH_HOST = "127.0.0.1"
        private const val AUTH_PORT = 9099
    }

    @Before
    fun setUpFirebase() {
        org.junit.Assume.assumeTrue("Firebase emulator is not running on $FIRESTORE_HOST:$FIRESTORE_PORT", isEmulatorAvailable())

        val context = ApplicationProvider.getApplicationContext<Context>()
        databaseId = context.getString(R.string.firestore_database_id)

        FirebaseApp.clearInstancesForTest()

        val options = FirebaseOptions.Builder()
            .setProjectId(PROJECT_ID)
            .setApplicationId("1:1234567890:android:abcdef")
            .setApiKey("fake-api-key-for-emulator")
            .build()
        val app = FirebaseApp.initializeApp(context, options)

        auth = FirebaseAuth.getInstance(app).apply {
            useEmulator(AUTH_HOST, AUTH_PORT)
        }

        firestore = FirebaseFirestore.getInstance(app, databaseId).apply {
            useEmulator(FIRESTORE_HOST, FIRESTORE_PORT)
            firestoreSettings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(MemoryCacheSettings.newBuilder().build())
                .build()
        }

        clearEmulatorData()
    }

    @After
    fun tearDownFirebase() {
        if (::firestore.isInitialized) {
            try { awaitTask(firestore.terminate()) } catch (_: Exception) {}
        }
        FirebaseApp.clearInstancesForTest()
    }

    protected fun signInTestUser(email: String = "alice@example.com"): String {
        val result = try {
            awaitTask(auth.createUserWithEmailAndPassword(email, "password123"))
        } catch (_: Exception) {
            awaitTask(auth.signInWithEmailAndPassword(email, "password123"))
        }
        return checkNotNull(result?.user?.uid) { "Emulator Auth produced a null UID" }
    }

    protected fun signOutTestUser() {
        auth.signOut()
        shadowOf(Looper.getMainLooper()).idle()
    }

    protected fun <T> awaitTask(task: Task<T>, timeoutSec: Long = 10): T? {
        val deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(timeoutSec)
        while (!task.isComplete) {
            if (System.currentTimeMillis() > deadline) {
                throw TimeoutException("Firebase task timed out after ${timeoutSec}s — is the emulator running?")
            }
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(10)
        }
        if (task.isSuccessful) return task.result
        throw task.exception ?: RuntimeException("Task failed without exception")
    }

    private fun isEmulatorAvailable(): Boolean {
        return try {
            java.net.Socket().use { socket ->
                socket.connect(java.net.InetSocketAddress(FIRESTORE_HOST, FIRESTORE_PORT), 300)
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun clearEmulatorData() {
        httpDelete("http://$FIRESTORE_HOST:$FIRESTORE_PORT/emulator/v1/projects/$PROJECT_ID/databases/$databaseId/documents")
        httpDelete("http://$AUTH_HOST:$AUTH_PORT/emulator/v1/projects/$PROJECT_ID/accounts")
    }

    private fun httpDelete(urlStr: String) {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            requestMethod = "DELETE"
            connectTimeout = 2000
            readTimeout = 2000
        }
        try {
            val code = conn.responseCode
            check(code in 200..299) { "Emulator clear failed ($code) for $urlStr" }
        } finally {
            conn.disconnect()
        }
    }
}
