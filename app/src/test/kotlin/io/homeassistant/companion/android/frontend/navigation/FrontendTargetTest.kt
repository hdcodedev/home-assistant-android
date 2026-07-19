package io.homeassistant.companion.android.frontend.navigation

import io.homeassistant.companion.android.frontend.navigation.FrontendTarget.Companion.toRawPath
import io.homeassistant.companion.android.settings.shortcuts.data.entities.ShortcutDestination
import io.homeassistant.companion.android.settings.shortcuts.toFrontendTarget
import io.homeassistant.companion.android.settings.shortcuts.toShortcutDestination
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class FrontendTargetTest {

    private val samples = listOf(
        FrontendTarget.Default,
        FrontendTarget.Path("/lovelace/0"),
        FrontendTarget.Path("/?more-info-entity-id=light.kitchen&foo=a b"),
        FrontendTarget.EntityMoreInfo("light.kitchen"),
    )

    @Test
    fun `Given legacy paths when fromRawPath then maps to the matching target`() {
        assertEquals(FrontendTarget.Default, FrontendTarget.fromRawPath(null))
        assertEquals(FrontendTarget.Path("/lovelace/0"), FrontendTarget.fromRawPath("/lovelace/0"))
        assertEquals(
            FrontendTarget.EntityMoreInfo("light.kitchen"),
            FrontendTarget.fromRawPath("entityId:light.kitchen"),
        )
    }

    @Test
    fun `Given any target when toLegacyPath then round-trips through fromRawPath`() {
        samples.forEach { target ->
            assertEquals(target, FrontendTarget.fromRawPath(target.toRawPath()))
        }
    }

    @Test
    fun `Given default target when toLegacyPath then returns null`() {
        assertNull(FrontendTarget.Default.toRawPath())
    }

    @Test
    fun `Given shortcut destinations when toFrontendTarget then matches frontend target`() {
        assertEquals(
            FrontendTarget.Path("/lovelace/home"),
            ShortcutDestination.Dashboard("/lovelace/home").toFrontendTarget(),
        )
        assertEquals(
            FrontendTarget.EntityMoreInfo("light.kitchen"),
            ShortcutDestination.Entity("light.kitchen").toFrontendTarget(),
        )
    }

    @Test
    fun `Given entity prefixed raw path when toShortcutDestination then returns Entity regardless of validity`() {
        assertEquals(
            ShortcutDestination.Entity("light.kitchen"),
            FrontendTarget.fromRawPath("entityId:light.kitchen").toShortcutDestination(),
        )
        assertEquals(
            ShortcutDestination.Entity("light"),
            FrontendTarget.fromRawPath("entityId:light").toShortcutDestination(),
        )
        assertEquals(
            ShortcutDestination.Entity(""),
            FrontendTarget.fromRawPath("entityId:").toShortcutDestination(),
        )
    }

    @Test
    fun `Given dashboard raw path when toShortcutDestination then returns Dashboard`() {
        assertEquals(
            ShortcutDestination.Dashboard("/lovelace/home"),
            FrontendTarget.fromRawPath("/lovelace/home").toShortcutDestination(),
        )
    }

    @Test
    fun `Given default target when toShortcutDestination then returns empty Dashboard`() {
        assertEquals(ShortcutDestination.Dashboard(""), FrontendTarget.Default.toShortcutDestination())
    }
}
