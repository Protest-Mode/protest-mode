package app.protestmode.disguise

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.w3c.dom.Element

@RunWith(AndroidJUnit4::class)
class DisguiseTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun enabledAliases(): List<Disguise> = Disguise.entries.filter {
        val state = context.packageManager.getComponentEnabledSetting(
            ComponentName(context, "app.protestmode.${it.alias}")
        )
        state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED ||
            (state == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT && it == Disguise.ORIGINAL)
    }

    @Test
    fun theOriginalLauncherEntryIsTheDefault() {
        assertEquals(Disguise.ORIGINAL, DisguiseManager(context).current())
        assertEquals(listOf(Disguise.ORIGINAL), enabledAliases())
    }

    @Test
    fun exactlyOneLauncherEntryIsEnabledAfterASwitch() {
        val manager = DisguiseManager(context)

        assertTrue(manager.apply(Disguise.WEATHER))
        assertEquals(Disguise.WEATHER, manager.current())
        assertEquals(listOf(Disguise.WEATHER), enabledAliases())

        manager.apply(Disguise.ORIGINAL)
        assertEquals(listOf(Disguise.ORIGINAL), enabledAliases())
    }

    @Test
    fun anUnreadableLauncherStateFallsBackToTheOriginal() {
        Disguise.entries.forEach {
            context.packageManager.setComponentEnabledSetting(
                ComponentName(context, "app.protestmode.${it.alias}"),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
        }
        assertEquals(Disguise.ORIGINAL, DisguiseManager(context).current())
        assertTrue(DisguiseManager(context).apply(Disguise.ORIGINAL))
        assertEquals(listOf(Disguise.ORIGINAL), enabledAliases())
    }

    @Test
    fun everyDisguiseHasALauncherAliasInTheManifest() {
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
            File("src/main/AndroidManifest.xml")
        )
        val aliases = document.getElementsByTagName("activity-alias")
        val declared = (0 until aliases.length).map { aliases.item(it) as Element }.associateBy {
            it.getAttribute("android:name").removePrefix(".")
        }
        assertEquals(Disguise.entries.map { it.alias }.toSet(), declared.keys)
        declared.forEach { (name, alias) ->
            assertEquals(".MainActivity", alias.getAttribute("android:targetActivity"))
            assertEquals((name == Disguise.ORIGINAL.alias).toString(), alias.getAttribute("android:enabled"))
            assertTrue(
                name,
                alias.getElementsByTagName("category").item(0).let {
                    (it as Element).getAttribute("android:name") == "android.intent.category.LAUNCHER"
                }
            )
        }

        val activities = document.getElementsByTagName("activity")
        val main = (0 until activities.length).map { activities.item(it) as Element }
            .first { it.getAttribute("android:name") == ".MainActivity" }
        assertEquals("The launcher entry belongs to the aliases", 0, main.getElementsByTagName("intent-filter").length)
    }
}
