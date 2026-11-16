package app.protestmode.data

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import app.protestmode.setting.Reading
import app.protestmode.setting.SettingId
import app.protestmode.setting.enumByName
import java.security.MessageDigest

/**
 * The user's "I checked" confirmations. One that applies to a comparable value (the device name) is
 * kept across launches as a SHA-256 fingerprint of that value, never the value itself, so that a name
 * holding a first name is never stored in clear; the others only last for the session. A confirmation
 * is dropped as soon as the confirmed value changes.
 */
class Confirmations(context: Context, private val savedState: SavedStateHandle) {
    private val prefs = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun confirm(id: SettingId, confirmationKey: String?) {
        save(load() + (id to fingerprint(confirmationKey)))
    }

    /** Confirmed settings whose value has not changed since; outdated confirmations are forgotten. */
    fun valid(readings: Map<SettingId, Reading>): Set<SettingId> {
        val stored = load()
        val valid = stored.filter { (id, key) -> readings[id]?.let { key == fingerprint(it.confirmationKey) } == true }
        if (valid.size != stored.size) save(valid)
        return valid.keys
    }

    private fun load(): Map<SettingId, String> {
        val persisted = prefs.all.mapNotNull { (name, value) ->
            enumByName<SettingId>(name)?.let { id -> (value as? String)?.let { id to it } }
        }
        val session = savedState.get<HashMap<String, String>>(KEY_SESSION).orEmpty().mapNotNull { (name, key) ->
            enumByName<SettingId>(name)?.let { it to key }
        }
        return (persisted + session).toMap()
    }

    private fun save(confirmations: Map<SettingId, String>) {
        savedState[KEY_SESSION] = HashMap(confirmations.filterValues { it.isEmpty() }.mapKeys { it.key.name })
        val editor = prefs.edit().clear()
        confirmations.filterValues {
            it.isNotEmpty()
        }.forEach { (id, fingerprint) -> editor.putString(id.name, fingerprint) }
        editor.apply()
    }

    internal companion object {
        const val FILE = "confirmations"
        const val KEY_SESSION = "confirmed"

        /** Empty when nothing can be compared: such a confirmation only lasts for the session. */
        fun fingerprint(value: String?): String {
            if (value == null) return ""
            val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
            return digest.joinToString("") { "%02x".format(it) }
        }
    }
}
