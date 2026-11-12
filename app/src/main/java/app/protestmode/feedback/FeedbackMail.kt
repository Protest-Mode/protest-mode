package app.protestmode.feedback

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat

object FeedbackMail {
    const val SUPPORT_EMAIL = "playstore.tablet712@passfwd.com"
    private const val PREFIX = "[Protest Mode]"

    fun feedback(context: Context): Intent =
        intent("$PREFIX Feedback — ${versionName(context)}", body(context, trace = null))

    fun crashReport(context: Context, trace: String): Intent =
        intent("$PREFIX Crash report — ${versionName(context)} — ${exceptionType(trace)}", body(context, trace))

    /** Only the class name of the exception: its message could hold a value read from the phone. */
    internal fun exceptionType(trace: String): String = trace.lineSequence()
        .firstOrNull()
        .orEmpty()
        .substringBefore(':')
        .substringAfterLast('.')
        .trim()
        .ifEmpty { "Unknown" }

    internal fun deviceLine(context: Context): String {
        @Suppress("DEPRECATION")
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        return "App ${info.versionName} (${PackageInfoCompat.getLongVersionCode(info)})" +
            " · Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})" +
            " · ${Build.MANUFACTURER} ${Build.MODEL}"
    }

    private fun versionName(context: Context): String {
        @Suppress("DEPRECATION")
        return context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
    }

    private fun body(context: Context, trace: String?): String = buildString {
        append("\n\n\n—\n")
        append(deviceLine(context))
        if (trace != null) append("\n\n").append(trace)
    }

    // ACTION_SEND keeps the subject and body in every email app; the mailto selector limits the
    // chooser to email apps.
    private fun intent(subject: String, body: String): Intent = Intent(Intent.ACTION_SEND).apply {
        putExtra(Intent.EXTRA_EMAIL, arrayOf(SUPPORT_EMAIL))
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, body)
        selector = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"))
    }
}
