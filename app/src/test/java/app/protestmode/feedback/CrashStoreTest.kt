package app.protestmode.feedback

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CrashStoreTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun keepsTheLastCrashUntilCleared() {
        val store = CrashStore(context)
        assertNull(store.pending())

        store.record(IllegalStateException("first"))
        store.record(IllegalArgumentException("second"))
        assertTrue(store.pending()!!.startsWith("java.lang.IllegalArgumentException: second"))

        store.clear()
        assertNull(store.pending())
    }

    @Test
    fun truncatesLongTracesButKeepsTheRootCause() {
        val trace = "java.lang.RuntimeException: top\n" + "\tat a.b.C.d(C.kt:1)\n".repeat(1_000) +
            "Caused by: java.io.IOException: root\n\tat e.f.G.h(G.kt:2)"
        val truncated = CrashStore.truncate(trace)

        assertTrue(truncated.length <= CrashStore.MAX_LENGTH)
        assertTrue(truncated.startsWith("java.lang.RuntimeException: top"))
        assertTrue(truncated.contains("Caused by: java.io.IOException: root"))
        assertEquals("short", CrashStore.truncate("short"))
    }
}
