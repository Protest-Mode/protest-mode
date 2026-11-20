package app.protestmode

import android.app.Application
import app.protestmode.feedback.CrashStore

class ProtestModeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val store = CrashStore(this)
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            store.record(error)
            previous?.uncaughtException(thread, error)
        }
    }
}
