package app.protestmode.feedback

import android.content.Context
import java.io.File

class CrashStore(context: Context) {
    private val appContext = context.applicationContext
    private val file = File(appContext.filesDir, FILE)

    // Called by the uncaught exception handler, right before the process dies: the write is synchronous.
    fun record(throwable: Throwable) {
        runCatching { file.writeText(truncate(throwable.stackTraceToString())) }
    }

    fun pending(): String? =
        runCatching { file.takeIf { it.exists() }?.readText() }.getOrNull()?.takeIf { it.isNotBlank() }

    fun clear() {
        file.delete()
    }

    internal companion object {
        const val FILE = "crash.txt"
        const val MAX_LENGTH = 6_000

        /** Keeps the top of the trace and, past the limit, the "Caused by" lines that name the root cause. */
        fun truncate(trace: String): String {
            if (trace.length <= MAX_LENGTH) return trace
            val causes = trace.lines().filter { it.startsWith("Caused by:") }.joinToString("\n")
            val separator = "\n…\n"
            val head = trace.take((MAX_LENGTH - causes.length - separator.length).coerceAtLeast(MAX_LENGTH / 2))
            return (head + separator + causes).take(MAX_LENGTH)
        }
    }
}
