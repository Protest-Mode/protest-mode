package app.protestmode.disguise

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import app.protestmode.MainActivity

/** Switches the launcher entry: the chosen disguise is the only enabled alias. Nothing else is stored. */
class DisguiseManager(context: Context) {
    private val appContext = context.applicationContext
    private val packageManager = appContext.packageManager

    fun current(): Disguise = Disguise.entries.firstOrNull { isEnabled(it) } ?: Disguise.ORIGINAL

    fun apply(target: Disguise) {
        // The target is enabled first, so that the launcher never ends up without an entry for the app.
        setEnabled(target, true)
        Disguise.entries.filter { it != target }.forEach { setEnabled(it, false) }
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
