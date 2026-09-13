package io.music_assistant.client.widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/**
 * System entry point (registered in AndroidManifest.xml) for placement, resize, and
 * OS-triggered updates. Content is otherwise pushed reactively by
 * [NowPlayingWidgetUpdater], not by this class — see its kdoc for why a plain
 * GlanceAppWidgetReceiver's own `onUpdate` isn't enough for a "live" widget.
 */
class NowPlayingWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NowPlayingWidget()
}
