package app.protestmode.disguise

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import app.protestmode.R

/** How the app appears in the launcher and in recent apps. Each entry matches an activity-alias of the manifest. */
enum class Disguise(val alias: String, @param:StringRes val labelRes: Int, @param:DrawableRes val iconRes: Int?) {
    ORIGINAL("LauncherOriginal", R.string.app_name, null),
    WEATHER("LauncherWeather", R.string.disguise_weather, R.drawable.disguise_weather),
    CLOCK("LauncherClock", R.string.disguise_clock, R.drawable.disguise_clock),
    CALCULATOR("LauncherCalculator", R.string.disguise_calculator, R.drawable.disguise_calculator),
    FLASHLIGHT("LauncherFlashlight", R.string.disguise_flashlight, R.drawable.disguise_flashlight),
    COMPASS("LauncherCompass", R.string.disguise_compass, R.drawable.disguise_compass)
}
