package io.music_assistant.client.widget

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import io.music_assistant.client.MainActivity
import io.music_assistant.client.R
import io.music_assistant.client.services.MediaNotificationData
import io.music_assistant.client.services.SharedMediaSessionManager
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

// Dark, calm palette lifted straight from ui/theme/Color.kt's dark scheme — the widget
// runs outside Compose's MaterialTheme, so these are copied rather than themed live. Keep
// in sync if the app's dark palette changes.
private val WidgetBackground = Color(0xFF1C2024) // surfaceContainerDark
private val WidgetOnBackground = Color(0xFFE0E2E9) // onSurfaceDark
private val WidgetOnBackgroundVariant = Color(0xFFC1C7CF) // onSurfaceVariantDark
private val WidgetControlTint = Color(0xFF8DCDFF) // primaryDark
private val WidgetFavoriteTint = Color(0xFFFFB1C8) // tertiaryDark — matches ui/theme/favoriteTint

private const val NARROW_WIDTH_THRESHOLD_DP = 250
private const val ART_SIZE_DP = 56
private const val CONTROL_ICON_SIZE_DP = 28
private const val FAVORITE_ICON_SIZE_DP = 22
private const val TAP_TARGET_PADDING_DP = 12

/** The widget deliberately commits to one dark look regardless of system theme (house
 * UI rule: dark mode always) — [ColorProvider] otherwise wants a separate night color. */
private fun fixedColor(color: Color) = ColorProvider(day = color, night = color)

/**
 * Home-screen now-playing widget. Renders whatever
 * [SharedMediaSessionManager.nowPlayingData] last held — the exact snapshot the media
 * notification is built from — so the two surfaces can never show conflicting text or
 * favorite state. See [NowPlayingWidgetUpdater] for what triggers a re-render.
 */
class NowPlayingWidget : GlanceAppWidget(), KoinComponent {
    // Exact (not a fixed set of declared sizes): the widget is resizable, and this keeps
    // LocalSize.current live across a drag-resize without listing every possible size.
    override val sizeMode: SizeMode = SizeMode.Exact

    private val sharedSession: SharedMediaSessionManager by inject()

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = sharedSession.nowPlayingData.value
        val art = sharedSession.notificationArt.value
            ?.takeIf { it.trackKey == data?.longItemId }
            ?.bitmap
        val strings = NowPlayingWidgetStrings.load()
        provideContent {
            NowPlayingWidgetContent(data, art, strings)
        }
    }
}

@Composable
private fun NowPlayingWidgetContent(
    data: MediaNotificationData?,
    art: Bitmap?,
    strings: NowPlayingWidgetStrings,
) {
    val context = LocalContext.current
    val openApp: Action = actionStartActivity(openPlayersIntent(context))
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(fixedColor(WidgetBackground))
            .cornerRadius(16.dp)
            .padding(12.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (data == null) {
            Box(
                modifier = GlanceModifier.fillMaxSize().clickable(openApp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = strings.nothingPlaying,
                    style = TextStyle(color = fixedColor(WidgetOnBackgroundVariant), fontSize = 14.sp),
                )
            }
        } else {
            NowPlayingRow(data, art, strings, openApp)
        }
    }
}

@Composable
private fun NowPlayingRow(
    data: MediaNotificationData,
    art: Bitmap?,
    strings: NowPlayingWidgetStrings,
    openApp: Action,
) {
    val isNarrow = LocalSize.current.width < NARROW_WIDTH_THRESHOLD_DP.dp
    Row(
        modifier = GlanceModifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = GlanceModifier.size(ART_SIZE_DP.dp).cornerRadius(8.dp).clickable(openApp)) {
            if (art != null) {
                Image(
                    provider = ImageProvider(art),
                    contentDescription = data.name,
                    modifier = GlanceModifier.fillMaxSize(),
                )
            } else {
                Box(
                    modifier = GlanceModifier.fillMaxSize().background(fixedColor(WidgetOnBackgroundVariant)),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.baseline_library_music_24),
                        contentDescription = null,
                    )
                }
            }
        }
        Spacer(modifier = GlanceModifier.width(10.dp))
        Column(modifier = GlanceModifier.defaultWeight().clickable(openApp)) {
            Text(
                text = data.name?.takeIf { it.isNotBlank() } ?: strings.unknownTrack,
                maxLines = 1,
                style = TextStyle(
                    color = fixedColor(WidgetOnBackground),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )
            Text(
                text = subtitle(data, strings),
                maxLines = 1,
                style = TextStyle(color = fixedColor(WidgetOnBackgroundVariant), fontSize = 12.sp),
            )
        }
        Spacer(modifier = GlanceModifier.width(4.dp))
        FavoriteButton(data, strings)
        if (!isNarrow) {
            WidgetIconButton(
                icon = R.drawable.ic_widget_skip_previous_24,
                contentDescription = strings.previous,
                action = actionRunCallback<NowPlayingWidgetActionCallback>(
                    widgetActionParams(WidgetCommand.PREVIOUS),
                ),
            )
        }
        WidgetIconButton(
            icon = if (data.isPlaying) R.drawable.ic_widget_pause_24 else R.drawable.ic_widget_play_24,
            contentDescription = if (data.isPlaying) strings.pause else strings.play,
            action = actionRunCallback<NowPlayingWidgetActionCallback>(
                widgetActionParams(WidgetCommand.PLAY_PAUSE),
            ),
            tint = WidgetControlTint,
        )
        if (!isNarrow) {
            WidgetIconButton(
                icon = R.drawable.ic_widget_skip_next_24,
                contentDescription = strings.next,
                action = actionRunCallback<NowPlayingWidgetActionCallback>(
                    widgetActionParams(WidgetCommand.NEXT),
                ),
            )
        }
    }
}

/**
 * For radio, [MediaNotificationData.artist] is already the on-air song's artist — the
 * same string the notification shows, per the widget spec. The "(on <player>)" suffix
 * mirrors `SharedMediaSessionManager.artistMetadata`'s notification-facing formatting.
 */
internal fun subtitle(data: MediaNotificationData, strings: NowPlayingWidgetStrings): String {
    val artist = data.artist?.takeIf { it.isNotBlank() } ?: strings.unknownArtist
    return data.playerName?.let { "$artist ($it)" } ?: artist
}

@Composable
private fun FavoriteButton(data: MediaNotificationData, strings: NowPlayingWidgetStrings) {
    if (!data.isFavoritableTrack && !data.isFavoritableStream) {
        Spacer(modifier = GlanceModifier.size((FAVORITE_ICON_SIZE_DP + TAP_TARGET_PADDING_DP).dp))
        return
    }
    WidgetIconButton(
        icon = if (data.isFavorite) R.drawable.baseline_favorite_24 else R.drawable.baseline_favorite_border_24,
        contentDescription = strings.favorite,
        action = actionRunCallback<NowPlayingWidgetActionCallback>(widgetActionParams(WidgetCommand.FAVORITE)),
        tint = if (data.isFavorite) WidgetFavoriteTint else WidgetOnBackgroundVariant,
        sizeDp = FAVORITE_ICON_SIZE_DP,
    )
}

@Composable
private fun WidgetIconButton(
    icon: Int,
    contentDescription: String,
    action: Action,
    tint: Color = WidgetOnBackground,
    sizeDp: Int = CONTROL_ICON_SIZE_DP,
) {
    Box(
        modifier = GlanceModifier.size((sizeDp + TAP_TARGET_PADDING_DP).dp).clickable(action),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            provider = ImageProvider(icon),
            contentDescription = contentDescription,
            modifier = GlanceModifier.size(sizeDp.dp),
            colorFilter = ColorFilter.tint(fixedColor(tint)),
        )
    }
}

/**
 * Same deep link + activity flags `MediaNotificationManager.createNotification` uses
 * (`musicassistant://app/players`, FLAG_ACTIVITY_CLEAR_TOP|SINGLE_TOP), plus
 * FLAG_ACTIVITY_NEW_TASK — required here since the widget fires this Intent from outside
 * any Activity context.
 */
private fun openPlayersIntent(context: Context): Intent = Intent(context, MainActivity::class.java).apply {
    data = Uri.parse("musicassistant://app/players")
    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
        Intent.FLAG_ACTIVITY_CLEAR_TOP or
        Intent.FLAG_ACTIVITY_SINGLE_TOP
}
