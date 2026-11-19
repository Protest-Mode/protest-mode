package app.protestmode.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.protestmode.MainActivity
import app.protestmode.R
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "fr")
class MainActivityTest {
    // The first-launch tour would cover the screen: it is marked as seen before the activity starts.
    @get:Rule(order = 0)
    val tourSeen =
        object : ExternalResource() {
            override fun before() {
                ApplicationProvider.getApplicationContext<Context>()
                    .getSharedPreferences("walkthrough", Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean("seen", true)
                    .commit()
            }
        }

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<MainActivity>()

    @Before
    fun waitForSettings() {
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodes(hasText(string(R.string.category_tracking))).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun string(id: Int) = compose.activity.getString(id)

    @Test
    fun showsBannerLabelsAndList() {
        listOf(
            R.string.label_device_access,
            R.string.label_location_tracking,
            R.string.label_fingerprinting,
            R.string.label_activity_history
        )
            .forEach { compose.onNodeWithText(string(it)).assertExists() }
        compose.onNodeWithText(string(R.string.category_tracking)).assertExists()
    }

    @Test
    fun tappingALabelFiltersTheList() {
        compose.onNodeWithText(string(R.string.label_device_access)).performClick()
        compose.onNodeWithText(string(R.string.label_device_access)).assertIsSelected()

        assertTrue(compose.onAllNodes(hasText(string(R.string.setting_screen_lock))).fetchSemanticsNodes().isNotEmpty())
        assertTrue(compose.onAllNodes(hasText(string(R.string.setting_wifi))).fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun requestsNoInternetNorDangerousPermission() {
        val activity = compose.activity
        val info: PackageInfo = activity.packageManager.getPackageInfo(
            activity.packageName,
            PackageManager.GET_PERMISSIONS
        )
        val requested = info.requestedPermissions.orEmpty().toList()

        assertFalse(Manifest.permission.INTERNET in requested)
        requested.forEach { name ->
            val protection = runCatching { activity.packageManager.getPermissionInfo(name, 0).protection }.getOrNull()
            assertTrue("$name is dangerous", protection != PermissionInfo.PROTECTION_DANGEROUS)
        }
    }
}
