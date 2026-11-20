package app.protestmode.disguise

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import app.protestmode.MainActivity

class DisguiseManager(context: Context) {
    private val appContext = context.applicationContext
    private val packageManager = appContext.packageManager

    fun current(): Disguise =
        runCatching { Disguise.entries.firstOrNull { isEnabled(it) } }.getOrNull() ?: Disguise.ORIGINAL

    /** Returns false, leaving the launcher entries unchanged, when the target cannot be enabled. */
    fun apply(target: Disguise): Boolean {
        // The target is enabled and checked first, so that the launcher never ends up without an entry.
        val enabled = runCatching {
            setEnabled(target, true)
            isEnabled(target)
        }.getOrDefault(false)
        if (!enabled) return false
        Disguise.entries.filter { it != target }.forEach { runCatching { setEnabled(it, false) } }
        return true
    }

    private fun component(disguise: Disguise) =
        ComponentName(appContext, "${MainActivity::class.java.`package`!!.name}.${disguise.alias}")

    private fun isEnabled(disguise: Disguise): Boolean =
        when (packageManager.getComponentEnabledSetting(component(disguise))) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
            PackageManager.COMPONENT_ENABLED_STATE_DEFAULT -> disguise == Disguise.ORIGINAL
            else -> false
        }

    private fun setEnabled(disguise: Disguise, enabled: Boolean) {
        val state = if (enabled) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }
        packageManager.setComponentEnabledSetting(component(disguise), state, PackageManager.DONT_KILL_APP)
    }
}
