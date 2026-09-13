package io.music_assistant.client.widget

import musicassistantclient.composeapp.generated.resources.Res
import musicassistantclient.composeapp.generated.resources.action_next
import musicassistantclient.composeapp.generated.resources.action_pause
import musicassistantclient.composeapp.generated.resources.action_play
import musicassistantclient.composeapp.generated.resources.action_previous
import musicassistantclient.composeapp.generated.resources.cd_favorite
import musicassistantclient.composeapp.generated.resources.media_unknown_artist
import musicassistantclient.composeapp.generated.resources.media_unknown_track
import musicassistantclient.composeapp.generated.resources.players_nothing
import org.jetbrains.compose.resources.getString

/**
 * Localized labels for the widget, resolved from the same Compose Multiplatform string
 * catalog the notification and full player use — mirrors
 * [io.music_assistant.client.services.MediaSessionStrings]. `players_nothing` ("Nothing
 * playing") is the exact string the in-app player already shows for an empty queue.
 */
internal class NowPlayingWidgetStrings(
    val unknownTrack: String,
    val unknownArtist: String,
    val nothingPlaying: String,
    val play: String,
    val pause: String,
    val next: String,
    val previous: String,
    val favorite: String,
) {
    companion object {
        suspend fun load(): NowPlayingWidgetStrings = NowPlayingWidgetStrings(
            unknownTrack = getString(Res.string.media_unknown_track),
            unknownArtist = getString(Res.string.media_unknown_artist),
            nothingPlaying = getString(Res.string.players_nothing),
            play = getString(Res.string.action_play),
            pause = getString(Res.string.action_pause),
            next = getString(Res.string.action_next),
            previous = getString(Res.string.action_previous),
            favorite = getString(Res.string.cd_favorite),
        )
    }
}
