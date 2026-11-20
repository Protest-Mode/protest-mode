package app.protestmode.ui

import app.protestmode.setting.SettingCategory
import app.protestmode.setting.SettingId
import app.protestmode.setting.SettingState
import app.protestmode.setting.protectiveState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ListEntriesTest {
    private fun row(id: SettingId, protected: Boolean): SettingRow {
        val on = protected == (id.protectiveState == SettingState.ON)
        return SettingRow(id, if (on) SettingState.ON else SettingState.OFF, confirmed = false)
    }

    private val rows =
        listOf(
            row(SettingId.AIRPLANE, protected = true),
            row(SettingId.WIFI, protected = true),
            row(SettingId.SCREEN_LOCK, protected = false),
            row(SettingId.ENCRYPTION, protected = true),
            row(SettingId.DEVICE_NAME, protected = true)
        )

    @Test
    fun theBannerAndFiltersComeFirstThenEachCategoryInOrder() {
        val entries = listEntries(rows)
        assertEquals(ListEntry.Banner, entries[0])
        assertEquals(ListEntry.Labels, entries[1])
        val categories = entries.filterIsInstance<ListEntry.Header>().map { it.category }
        assertEquals(listOf(SettingCategory.TRACKING, SettingCategory.SECURITY, SettingCategory.ANONYMITY), categories)
        assertEquals(2 + rows.size + 2 * categories.size, entries.size)
    }

    @Test
    fun eachCategoryIsAHeaderItsSettingsThenASpace() {
        val entries = listEntries(rows)
        val security = entries.indexOfFirst { it is ListEntry.Header && it.category == SettingCategory.SECURITY }
        assertEquals(ListEntry.Header(SettingCategory.SECURITY, protectedCount = 1, total = 2), entries[security])
        assertEquals(ListEntry.Setting(rows[2], index = 0, lastIndex = 1), entries[security + 1])
        assertEquals(ListEntry.Setting(rows[3], index = 1, lastIndex = 1), entries[security + 2])
        assertEquals(ListEntry.End(SettingCategory.SECURITY), entries[security + 3])
    }

    @Test
    fun theTourPointsAtTheFirstSettingToChangeAndScrollsToItsHeader() {
        val entries = listEntries(rows)
        val setting = entries.tourSetting()!!
        assertEquals(SettingId.SCREEN_LOCK, (entries[setting] as ListEntry.Setting).row.id)
        val scroll = entries.tourScroll()!!
        assertEquals(ListEntry.Header(SettingCategory.SECURITY, 1, 2), entries[scroll])
        assertTrue(scroll < setting)
    }

    @Test
    fun theTourHasNoSettingStepWhenEverythingIsProtected() {
        val entries = listEntries(rows.map { row(it.id, protected = true) })
        assertNull(entries.tourSetting())
        assertNull(entries.tourScroll())
    }
}
