package io.music_assistant.client.widget

import android.content.Context
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.action.ActionCallback
import io.music_assistant.client.services.SharedMediaSessionManager
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/** Widget button commands. FAVORITE dispatches the exact custom action id
 * `SharedMediaSessionManager.createCallback`'s `onCustomAction` already handles for the
 * notification and Android Auto — see that class's ACTION_TOGGLE_FAVORITE branch. */
internal enum class WidgetCommand { PLAY_PAUSE, PREVIOUS, NEXT, FAVORITE }

private val COMMAND_KEY = ActionParameters.Key<String>("now_playing_widget_command")

internal fun widgetActionParams(command: WidgetCommand) =
    actionParametersOf(COMMAND_KEY to command.name)

/**
 * Dispatches a widget button tap through a [MediaControllerCompat] built from the live
 * session's token ([SharedMediaSessionManager.sessionToken]) — the exact same
 * `MediaSessionCompat.Callback` the notification, Android Auto and hardware media
 * buttons already go through, so the widget can never behave differently from those
 * surfaces. A tap while nothing has acquired the session is a safe no-op: the widget
 * shows its idle layout then anyway, with no buttons to tap.
 */
internal class NowPlayingWidgetActionCallback : ActionCallback, KoinComponent {
    private val sharedSession: SharedMediaSessionManager by inject()

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val token = sharedSession.sessionToken() ?: return
        val controller = MediaControllerCompat(context, token)
        val command = parameters[COMMAND_KEY]
            ?.let { runCatching { WidgetCommand.valueOf(it) }.getOrNull() }
            ?: return
        when (command) {
            WidgetCommand.PLAY_PAUSE -> {
                if (controller.playbackState?.state == PlaybackStateCompat.STATE_PLAYING) {
                    controller.transportControls.pause()
                } else {
                    controller.transportControls.play()
                }
            }

            WidgetCommand.PREVIOUS -> controller.transportControls.skipToPrevious()
            WidgetCommand.NEXT -> controller.transportControls.skipToNext()
            WidgetCommand.FAVORITE ->
                controller.transportControls.sendCustomAction("ACTION_TOGGLE_FAVORITE", null)
        }
    }
}
