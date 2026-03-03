package io.homeassistant.companion.android.common.data.shortcuts.entities

import android.content.Intent
import android.os.Bundle
import io.homeassistant.companion.android.common.data.shortcuts.impl.ShortcutIntentCodecImpl
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ShortcutDraftCompatTest {

    private val codec = ShortcutIntentCodecImpl()

    @Test
    fun `Given v1 shortcut payload when parsed then uses legacy action path and icon format`() {
        val extras = extras(
            strings = mapOf(
                "iconName" to "mdi:flash",
            ),
        )

        val path = resolveShortcutPathCompat(
            extras = extras,
            action = "entityId:light.kitchen",
        )
        val destination = codec.parseDestination(path)
        val icon = codec.parseIcon(extras = extras, iconIdToName = emptyMap())

        assertEquals("entityId:light.kitchen", path)
        assertEquals(ShortcutDestination.Entity("light.kitchen"), destination)
        assertEquals("mdi:flash", icon)
    }

    @Test
    fun `Given ACTION_VIEW payload with legacy path extra when parsed then reads legacy path extra`() {
        val extras = extras(
            strings = mapOf(
                "path" to "/lovelace/home",
            ),
        )

        val path = resolveShortcutPathCompat(
            extras = extras,
            action = Intent.ACTION_VIEW,
        )
        val destination = codec.parseDestination(path)

        assertEquals("/lovelace/home", path)
        assertEquals(ShortcutDestination.Lovelace("/lovelace/home"), destination)
    }

    @Test
    fun `Given path extra and action when parsed then path extra has priority`() {
        val extras = extras(
            strings = mapOf(
                "path" to "entityId:switch.kitchen",
                "iconName" to "flash",
            ),
        )

        val path = resolveShortcutPathCompat(
            extras = extras,
            action = Intent.ACTION_VIEW,
        )
        val destination = codec.parseDestination(path)
        val icon = codec.parseIcon(extras = extras, iconIdToName = emptyMap())

        assertEquals("entityId:switch.kitchen", path)
        assertEquals(ShortcutDestination.Entity("switch.kitchen"), destination)
        assertEquals("mdi:flash", icon)
    }

    @Test
    fun `Given type extra but no entity prefix when parsed then destination is lovelace`() {
        val extras = extras(
            strings = mapOf(
                "type" to "ENTITY_ID",
            ),
        )

        val path = resolveShortcutPathCompat(
            extras = extras,
            action = "/lovelace/home",
        )
        val destination = codec.parseDestination(path)

        assertEquals(ShortcutDestination.Lovelace("/lovelace/home"), destination)
    }

    private fun extras(strings: Map<String, String>): Bundle {
        return mockk<Bundle>().also { bundle ->
            every { bundle.getString(any()) } answers {
                strings[firstArg()]
            }
            every { bundle.containsKey(any()) } answers {
                strings.containsKey(firstArg<String>())
            }
            every { bundle.getInt(any()) } returns 0
            every { bundle.getInt(any(), any()) } answers { secondArg() }
        }
    }

    private fun resolveShortcutPathCompat(extras: Bundle?, action: String?): String {
        return extras?.getString("path").orEmpty()
            .ifBlank { action.orEmpty() }
    }
}
