package io.music_assistant.client.widget

import io.music_assistant.client.services.MediaNotificationData

/**
 * Pure projection of [MediaNotificationData] for the home-screen widget. No Android
 * dependency beyond the input type's own fields, so [toWidgetModel] is unit-testable
 * without Robolectric — see NowPlayingWidgetModelTest.
 *
 * Deliberately reuses [MediaNotificationData] itself (the exact snapshot the
 * notification renders from — see `SharedMediaSessionManager.nowPlayingData`) rather than
 * re-deriving text or favorite state from `PlayerData`: the widget spec calls for the
 * "same strings the notification uses" and a heart that is "filled only when the
 * notification would show it filled", and sharing the type is what makes disagreement
 * between the two surfaces structurally impossible, not just unlikely.
 */
sealed interface NowPlayingWidgetModel {
    /** Nothing has acquired the media session (no active player) — a clear idle state. */
    data object Idle : NowPlayingWidgetModel

    data class NowPlaying(
        val title: String,
        val artist: String,
        val playerName: String?,
        val isPlaying: Boolean,
        val isFavorite: Boolean,
        val favoriteAvailable: Boolean,
        val imageUrl: String?,
        val trackKey: Long?,
    ) : NowPlayingWidgetModel
}

/**
 * Maps a [MediaNotificationData] snapshot (or null, the idle state) to the widget model.
 * [unknownTrack]/[unknownArtist] are the same localized fallbacks
 * [io.music_assistant.client.services.MediaSessionStrings] resolves for the notification
 * and media session, passed in so this mapping stays a pure, synchronous function.
 */
fun MediaNotificationData?.toWidgetModel(
    unknownTrack: String,
    unknownArtist: String,
): NowPlayingWidgetModel {
    val data = this ?: return NowPlayingWidgetModel.Idle
    return NowPlayingWidgetModel.NowPlaying(
        title = data.name?.takeIf { it.isNotBlank() } ?: unknownTrack,
        artist = data.artist?.takeIf { it.isNotBlank() } ?: unknownArtist,
        playerName = data.playerName,
        isPlaying = data.isPlaying,
        isFavorite = data.isFavorite,
        favoriteAvailable = data.isFavoritableTrack || data.isFavoritableStream,
        imageUrl = data.imageUrl,
        trackKey = data.longItemId,
    )
}
