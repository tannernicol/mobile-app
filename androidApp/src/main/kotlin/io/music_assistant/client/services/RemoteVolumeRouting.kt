package io.music_assistant.client.services

import io.music_assistant.client.ui.compose.common.action.PlayerAction

/**
 * Personal-fork addition (upstream declined this in mobile-app#767 "Use physical volume
 * keys to change volume on remote outputs", closed NOT_PLANNED — PR #697 shows a toast
 * instead). Volume range published to the OS via `VolumeProviderCompat`, matching the
 * in-app slider's 0-100 range ([io.music_assistant.client.ui.compose.home.players.VolumeSliderBox]).
 */
internal const val REMOTE_VOLUME_MAX = 100

/**
 * Maps a `VolumeProviderCompat.onAdjustVolume` direction to the app's existing volume
 * actions, group-aware exactly like the in-app slider's step buttons
 * ([io.music_assistant.client.ui.compose.home.players.PlayersPager]'s `isGroupForGesture`).
 * Null for direction 0 — Android never sends it, but the callback signature allows it.
 */
internal fun remoteVolumeAdjustAction(direction: Int, isGroupBound: Boolean): PlayerAction? =
    when {
        direction > 0 -> if (isGroupBound) PlayerAction.GroupVolumeUp else PlayerAction.VolumeUp
        direction < 0 -> {
            if (isGroupBound) PlayerAction.GroupVolumeDown else PlayerAction.VolumeDown
        }

        else -> null
    }

/**
 * Maps a `VolumeProviderCompat.onSetVolumeTo` absolute level to the app's existing
 * volume-set action, clamped to [REMOTE_VOLUME_MAX] and group-aware.
 */
internal fun remoteVolumeSetAction(volume: Int, isGroupBound: Boolean): PlayerAction {
    val level = volume.coerceIn(0, REMOTE_VOLUME_MAX).toDouble()
    return if (isGroupBound) PlayerAction.GroupVolumeSet(level) else PlayerAction.VolumeSet(level)
}
