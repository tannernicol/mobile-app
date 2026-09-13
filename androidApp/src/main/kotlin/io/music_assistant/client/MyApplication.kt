package io.music_assistant.client

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ApplicationInfo
import io.music_assistant.client.di.androidModule
import io.music_assistant.client.di.appModule
import io.music_assistant.client.di.cleanupSingletons
import io.music_assistant.client.di.initKoin
import io.music_assistant.client.logging.InMemoryLogWriter
import io.music_assistant.client.services.MediaNotificationManager
import io.music_assistant.client.services.SharedMediaSessionManager
import io.music_assistant.client.widget.NowPlayingWidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
import org.koin.core.context.stopKoin
import java.io.File

class MyApplication : Application() {
    // App-process-lifetime scope for work that must outlive any one service — see
    // NowPlayingWidgetUpdater's kdoc for why the widget update can't live in
    // MainMediaPlaybackService's own (service-lifetime) scope instead.
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        val debuggable = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        initKoin(androidModule(), appModule(), verboseLogging = debuggable) {
            androidContext(this@MyApplication)
        }
        createNotificationChannel(this)
        cleanupStaleLogFile()
        installCrashHandler()
        NowPlayingWidgetUpdater(this, GlobalContext.get().get<SharedMediaSessionManager>()).start(appScope)
    }

    private fun cleanupStaleLogFile() {
        File(cacheDir, "ma_client_logs.txt").delete()
    }

    private fun installCrashHandler() {
        val crashLogFile = File(cacheDir, "ma_crash_log.txt")
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                crashLogFile.delete()
                val text = InMemoryLogWriter.getLogText() +
                    "\n\n=== CRASH ===\n" + throwable.stackTraceToString()
                crashLogFile.writeText(text)
            } catch (_: Exception) { }
            previousHandler?.uncaughtException(thread, throwable)
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        // Clean up resources when app is terminated
        // Note: This is called on emulator/testing but not on real devices
        // However, having it ensures proper cleanup in tests
        cleanupSingletons()
        stopKoin()
    }

    private fun createNotificationChannel(context: Context) {
        val channel = NotificationChannel(
            MediaNotificationManager.CHANNEL_ID,
            "Media Playback",
            NotificationManager.IMPORTANCE_LOW,
        )
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }
}
