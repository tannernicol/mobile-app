package io.music_assistant.client.widget

import io.music_assistant.client.data.model.client.RepeatMode
import io.music_assistant.client.services.MediaNotificationData
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * [toWidgetModel] must never disagree with what the notification shows — see
 * NowPlayingWidgetModel.kt's kdoc. These pin the mapping directly against
 * [MediaNotificationData], the exact snapshot both surfaces render from.
 */
class NowPlayingWidgetModelTest {
    private val unknownTrack = "Unknown Track"
    private val unknownArtist = "Unknown Artist"

    @Test
    fun `null data maps to idle`() {
        assertEquals(NowPlayingWidgetModel.Idle, null.toWidgetModel(unknownTrack, unknownArtist))
    }

    @Test
    fun `favorited track shows a filled heart`() {
        val model = data(isFavoritableTrack = true, isFavorite = true)
            .toWidgetModel(unknownTrack, unknownArtist)
        val nowPlaying = assertIs<NowPlayingWidgetModel.NowPlaying>(model)
        assertTrue(nowPlaying.favoriteAvailable)
        assertTrue(nowPlaying.isFavorite)
    }

    @Test
    fun `unfavorited track shows an outline heart`() {
        val model = data(isFavoritableTrack = true, isFavorite = false)
            .toWidgetModel(unknownTrack, unknownArtist)
        val nowPlaying = assertIs<NowPlayingWidgetModel.NowPlaying>(model)
        assertTrue(nowPlaying.favoriteAvailable)
        assertFalse(nowPlaying.isFavorite)
    }

    @Test
    fun `favoritable radio stream is always available but never filled`() {
        // Mirrors MediaNotificationData.from: a radio stream's own favorite flag belongs
        // to the station, so isFavorite is always false for the stream case even though
        // favoriteAvailable is true (see MediaNotificationData.kt's isFavorite comment).
        val model = data(isFavoritableTrack = false, isFavoritableStream = true, isFavorite = false)
            .toWidgetModel(unknownTrack, unknownArtist)
        val nowPlaying = assertIs<NowPlayingWidgetModel.NowPlaying>(model)
        assertTrue(nowPlaying.favoriteAvailable)
        assertFalse(nowPlaying.isFavorite)
    }

    @Test
    fun `radio without an on-air song has no favorite affordance`() {
        val model = data(isFavoritableTrack = false, isFavoritableStream = false)
            .toWidgetModel(unknownTrack, unknownArtist)
        val nowPlaying = assertIs<NowPlayingWidgetModel.NowPlaying>(model)
        assertFalse(nowPlaying.favoriteAvailable)
    }

    @Test
    fun `blank or missing name and artist fall back to the localized unknowns`() {
        val blank = data(name = " ", artist = null).toWidgetModel(unknownTrack, unknownArtist)
        val nowPlaying = assertIs<NowPlayingWidgetModel.NowPlaying>(blank)
        assertEquals(unknownTrack, nowPlaying.title)
        assertEquals(unknownArtist, nowPlaying.artist)
    }

    @Test
    fun `real name and artist pass through unchanged`() {
        val model = data(name = "About Farewell", artist = "Alela Diane")
            .toWidgetModel(unknownTrack, unknownArtist)
        val nowPlaying = assertIs<NowPlayingWidgetModel.NowPlaying>(model)
        assertEquals("About Farewell", nowPlaying.title)
        assertEquals("Alela Diane", nowPlaying.artist)
    }

    @Test
    fun `player name and playing state and track key pass through`() {
        val model = data(playerName = "Kitchen", isPlaying = false, longItemId = 42L)
            .toWidgetModel(unknownTrack, unknownArtist)
        val nowPlaying = assertIs<NowPlayingWidgetModel.NowPlaying>(model)
        assertEquals("Kitchen", nowPlaying.playerName)
        assertFalse(nowPlaying.isPlaying)
        assertEquals(42L, nowPlaying.trackKey)
    }

    private fun data(
        name: String? = "Song",
        artist: String? = "Artist",
        playerName: String? = null,
        isPlaying: Boolean = true,
        isFavoritableTrack: Boolean = false,
        isFavoritableStream: Boolean = false,
        isFavorite: Boolean = false,
        longItemId: Long? = null,
    ) = MediaNotificationData(
        multiplePlayers = false,
        longItemId = longItemId,
        name = name,
        artist = artist,
        album = null,
        repeatMode = RepeatMode.OFF,
        shuffleEnabled = false,
        isLongFormContent = false,
        isFavoritableTrack = isFavoritableTrack,
        isFavoritableStream = isFavoritableStream,
        isFavorite = isFavorite,
        isPlaying = isPlaying,
        imageUrl = null,
        chapterName = null,
        elapsedTime = null,
        elapsedUpdateTimeMs = null,
        playerName = playerName,
        duration = null,
    )
}

/**
 * [subtitle] renders "the same strings the notification uses" for both track and radio
 * (the widget spec's explicit requirement) — it never re-derives radio-specific text,
 * it just formats whatever [MediaNotificationData.artist] already holds (the on-air
 * song's artist for radio, populated upstream by
 * `io.music_assistant.client.data.NowPlayingChannels`'s stream-metadata overlay).
 */
class NowPlayingSubtitleTest {
    private val strings = NowPlayingWidgetStringsFixture.build()

    @Test
    fun `artist alone with no player suffix`() {
        assertEquals("Alela Diane", subtitle(data(artist = "Alela Diane"), strings))
    }

    @Test
    fun `player suffix appended when present, single or multi player alike`() {
        assertEquals(
            "Alela Diane (Kitchen)",
            subtitle(data(artist = "Alela Diane", playerName = "Kitchen"), strings),
        )
    }

    @Test
    fun `blank artist falls back to the localized unknown artist`() {
        assertEquals(strings.unknownArtist, subtitle(data(artist = " "), strings))
    }

    @Test
    fun `radio on-air artist passes through exactly like a track's`() {
        // Radio's "artist" here is the on-air song's artist, already resolved upstream —
        // subtitle() treats it identically to a track, per the widget spec.
        assertEquals(
            "Big Thief",
            subtitle(data(name = "Vampire Empire", artist = "Big Thief"), strings),
        )
    }

    private fun data(artist: String?, name: String? = "Song", playerName: String? = null) =
        MediaNotificationData(
            multiplePlayers = false,
            longItemId = null,
            name = name,
            artist = artist,
            album = null,
            repeatMode = RepeatMode.OFF,
            shuffleEnabled = false,
            isLongFormContent = false,
            isFavoritableTrack = false,
            isFavoritableStream = false,
            isFavorite = false,
            isPlaying = true,
            imageUrl = null,
            chapterName = null,
            elapsedTime = null,
            elapsedUpdateTimeMs = null,
            playerName = playerName,
            duration = null,
        )
}

/** Builds [NowPlayingWidgetStrings] without the suspend Compose-resource loader, for
 * tests that only need its plain fields. */
internal object NowPlayingWidgetStringsFixture {
    fun build() = NowPlayingWidgetStrings(
        unknownTrack = "Unknown Track",
        unknownArtist = "Unknown Artist",
        nothingPlaying = "Nothing playing",
        play = "Play",
        pause = "Pause",
        next = "Next",
        previous = "Previous",
        favorite = "Favorite",
    )
}
