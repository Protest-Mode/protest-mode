package app.protestmode

import android.app.Application
import app.protestmode.feedback.CrashStore

class ProtestModeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val store = CrashStore(this)
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        // The report is only kept on the phone; the previous handler still ends the process as usual.
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            store.record(error)
            previous?.uncaughtException(thread, error)
        }
    }
}
