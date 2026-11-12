package app.protestmode.feedback

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FeedbackMailTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun opensAnEmailToTheSupportAddress() {
        val intent = FeedbackMail.feedback(context)

        assertEquals(Intent.ACTION_SEND, intent.action)
        assertEquals(Intent.ACTION_SENDTO, intent.selector?.action)
        assertEquals("mailto", intent.selector?.data?.scheme)
        assertEquals(listOf(FeedbackMail.SUPPORT_EMAIL), intent.getStringArrayExtra(Intent.EXTRA_EMAIL)?.toList())
    }

    @Test
    fun prefillsTheSubjectAndTheDeviceInformation() {
        val intent = FeedbackMail.feedback(context)
        val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT)!!
        val body = intent.getStringExtra(Intent.EXTRA_TEXT)!!

        assertTrue(subject, subject.startsWith("[Protest Mode] Feedback — "))
        assertTrue(body, body.contains("App ") && body.contains(" · Android ") && body.contains("(API "))
    }

    @Test
    fun crashSubjectNamesTheExceptionTypeButNotItsMessage() {
        val trace = "java.lang.IllegalStateException: device name Pixel de Sam\n\tat a.b.C.d(C.kt:1)"
        val intent = FeedbackMail.crashReport(context, trace)
        val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT)!!

        assertTrue(subject, subject.startsWith("[Protest Mode] Crash report — "))
        assertTrue(subject, subject.endsWith(" — IllegalStateException"))
        assertFalse(subject, subject.contains("Pixel de Sam"))
        assertTrue(intent.getStringExtra(Intent.EXTRA_TEXT)!!.contains(trace))
    }

    @Test
    fun feedbackCarriesNoTrace() {
        assertFalse(FeedbackMail.feedback(context).getStringExtra(Intent.EXTRA_TEXT)!!.contains("Exception"))
    }
}
