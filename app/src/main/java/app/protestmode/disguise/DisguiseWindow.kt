package app.protestmode.disguise

import android.app.Activity
import android.app.ActivityManager
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap

object DisguiseWindow {
    @Suppress("DEPRECATION")
    fun apply(activity: Activity, disguise: Disguise) {
        val label = activity.getString(disguise.labelRes)
        val icon = disguise.iconRes?.let { ContextCompat.getDrawable(activity, it)?.toBitmap(192, 192) }
        activity.setTaskDescription(
            if (icon == null) ActivityManager.TaskDescription(label) else ActivityManager.TaskDescription(label, icon)
        )
        // FLAG_SECURE blanks the recent apps preview, which would otherwise show the real content.
        if (disguise == Disguise.ORIGINAL) {
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}
