package io.music_assistant.client.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import co.touchlab.kermit.Logger
import io.music_assistant.client.services.SharedMediaSessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Pushes a widget re-render every time [SharedMediaSessionManager.nowPlayingData]
 * changes — the exact same trigger point `MainMediaPlaybackService` reposts the
 * notification from (`sharedSession.notificationArt.collect { ... }`).
 *
 * Deliberately NOT collected from inside `MainMediaPlaybackService`'s own coroutine
 * scope: that scope is cancelled in `onDestroy()` immediately after
 * `sharedSession.release()`, which is what resets `nowPlayingData` to null (idle) on
 * stop — a same-thread `cancel()` right after a plain `StateFlow` assignment races the
 * collector, so the widget could miss the final "went idle" emission and keep showing a
 * stale track. Started once for the app process instead ([start], called from
 * `MyApplication.onCreate`), so it outlives any one service and always sees the idle
 * transition.
 */
class NowPlayingWidgetUpdater(
    private val context: Context,
    private val sharedSession: SharedMediaSessionManager,
) {
    private val logger = Logger.withTag("NowPlayingWidgetUpdater")

    fun start(scope: CoroutineScope) {
        scope.launch {
            sharedSession.nowPlayingData.collect {
                // No-op when the widget isn't pinned — updateAll() is safe to call
                // either way, but skipping the (cheap) call avoids pointless work.
                if (GlanceAppWidgetManager(context).getGlanceIds(NowPlayingWidget::class.java).isNotEmpty()) {
                    runCatching { NowPlayingWidget().updateAll(context) }
                        .onFailure { logger.w(it) { "Widget update failed" } }
                }
            }
        }
    }
}
