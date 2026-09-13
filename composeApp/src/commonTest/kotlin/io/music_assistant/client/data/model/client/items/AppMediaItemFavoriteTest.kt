package io.music_assistant.client.data.model.client.items

import io.music_assistant.client.data.model.client.testRadio
import io.music_assistant.client.data.model.client.testTrack
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * [isFavoritableTrack] is the single track-only heart-eligibility predicate shared by
 * the media notification ([io.music_assistant.client.services.MediaNotificationData]),
 * the media session's ACTION_TOGGLE_FAVORITE handler
 * (`SharedMediaSessionManager.createCallback`), the full player heart row
 * (`PlayerItems.FullPlayerItem`), and the home-screen widget — so all four surfaces
 * agree on when a queue item's OWN favorite flag is the one to show and toggle.
 *
 * Regression test for the bug reproduced on 2026-09-13 while radio station "Le
 * Mellotron" played: the full player used `canBeFavorited` alone (no `mediaType ==
 * TRACK` guard), so the STATION's own favorite flag rendered as a filled heart and a
 * tap called `toggleFavorite`, which UNFAVORITED THE STATION — the song-on-air branch
 * ([io.music_assistant.client.data.hasFavoritableStreamTrack] /
 * `MainDataSource.canFavoriteCurrentlyPlaying`, already covered by
 * `PlayerDataHasFavoritableStreamTrackTest` in NowPlayingChannelsTest.kt, including its
 * own schema-version-27 gate) was unreachable.
 */
class AppMediaItemIsFavoritableTrackTest {
    @Test
    fun favoritedTrackWithUriIsFavoritable() {
        val track = testTrack().copy(uri = "spotify://track/1", favorite = true)
        assertTrue(track.isFavoritableTrack)
    }

    @Test
    fun unfavoritedTrackWithUriIsFavoritable() {
        val track = testTrack().copy(uri = "spotify://track/1", favorite = false)
        assertTrue(track.isFavoritableTrack)
    }

    @Test
    fun trackWithoutUriIsNotFavoritable() {
        val track = testTrack().copy(uri = null, favorite = true)
        assertFalse(track.isFavoritableTrack)
    }

    @Test
    fun favoritedRadioStationIsNeverTrackFavoritable() {
        // The exact reported bug: a favorited station has a uri and favorite=true, but
        // its mediaType is RADIO, not TRACK — the station's own flag must never drive
        // this heart.
        val station = testRadio().copy(uri = "spotify://radio/1", favorite = true)
        assertFalse(station.isFavoritableTrack)
    }

    @Test
    fun unfavoritedRadioStationIsNotTrackFavoritable() {
        val station = testRadio().copy(uri = "spotify://radio/1", favorite = false)
        assertFalse(station.isFavoritableTrack)
    }
}
