package io.music_assistant.client.services

import io.music_assistant.client.ui.compose.common.action.PlayerAction
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Pins the direction/level -> [PlayerAction] mapping that
 * [SharedMediaSessionManager]'s remote `VolumeProviderCompat` dispatches into. The
 * provider itself is Android-framework-bound; this is the pure part of it.
 */
class RemoteVolumeRoutingTest {
    @Test
    fun `single player steps use the plain volume actions`() {
        assertEquals(
            PlayerAction.VolumeUp,
            remoteVolumeAdjustAction(direction = 1, isGroupBound = false),
        )
        assertEquals(
            PlayerAction.VolumeDown,
            remoteVolumeAdjustAction(direction = -1, isGroupBound = false),
        )
    }

    @Test
    fun `synced group steps use the group volume actions`() {
        assertEquals(
            PlayerAction.GroupVolumeUp,
            remoteVolumeAdjustAction(direction = 1, isGroupBound = true),
        )
        assertEquals(
            PlayerAction.GroupVolumeDown,
            remoteVolumeAdjustAction(direction = -1, isGroupBound = true),
        )
    }

    @Test
    fun `zero direction is not a step`() {
        assertNull(remoteVolumeAdjustAction(direction = 0, isGroupBound = false))
        assertNull(remoteVolumeAdjustAction(direction = 0, isGroupBound = true))
    }

    @Test
    fun `larger magnitudes still resolve to a single step`() {
        // VolumeProviderCompat only ever sends -1 0 1, but the mapping is a sign check,
        // not an equality check, so a larger magnitude still resolves sanely.
        assertEquals(
            PlayerAction.VolumeUp,
            remoteVolumeAdjustAction(direction = 5, isGroupBound = false),
        )
        assertEquals(
            PlayerAction.VolumeDown,
            remoteVolumeAdjustAction(direction = -5, isGroupBound = false),
        )
    }

    @Test
    fun `absolute set maps to the plain or group volume-set action`() {
        assertEquals(
            PlayerAction.VolumeSet(42.0),
            remoteVolumeSetAction(volume = 42, isGroupBound = false),
        )
        assertEquals(
            PlayerAction.GroupVolumeSet(42.0),
            remoteVolumeSetAction(volume = 42, isGroupBound = true),
        )
    }

    @Test
    fun `absolute set clamps to the published range`() {
        assertEquals(
            PlayerAction.VolumeSet(0.0),
            remoteVolumeSetAction(volume = -5, isGroupBound = false),
        )
        assertEquals(
            PlayerAction.VolumeSet(REMOTE_VOLUME_MAX.toDouble()),
            remoteVolumeSetAction(volume = 150, isGroupBound = false),
        )
    }
}
