package phss.feelsapp.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import org.koin.android.ext.android.inject
import phss.feelsapp.data.models.Song
import phss.feelsapp.player.PlayerManager
import phss.feelsapp.player.observers.PlayerStateChangeObserver
import phss.feelsapp.player.notification.PlayerNotificationManager

class PlayerService : Service() {

    private var isRunning = false

    private val binder = LocalBinder()

    val playerManager: PlayerManager by inject()
    val playerNotificationManager = PlayerNotificationManager(this)

    override fun onBind(intent: Intent): IBinder {
        playerManager.registerPlayerStateChangeObserver(this::class, setupPlayerStateChangeObserver())
        playerManager.registerPlayerObserver(this::class, setupPlayerObserver())
        registerReceiver(playerNotificationManager, playerNotificationManager.getReceiverFilter())
        return binder
    }

    override fun onUnbind(intent: Intent): Boolean {
        playerManager.stopPlayer()
        playerManager.unregisterPlayerStateChangeObserver(this::class)
        playerManager.unregisterPlayerObserver(this::class)

        playlistsRepository.unregisterPlaylistsChangeDataObserver(this::class)
        return true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        isRunning = true
        return START_NOT_STICKY
    }

    private fun setupPlayerStateChangeObserver() = object : PlayerStateChangeObserver {
        override fun onPlaying(song: Song, duration: Int, progress: Int) { updateNotification() }
        override fun onTimeChange(song: Song, timePercent: Int) { updateNotification() }
        override fun onResume(song: Song) { updateNotification() }
        override fun onPause(song: Song) { updateNotification() }

        override fun onStop() {
            stopForeground(playerNotificationManager.NOTIFICATION_ID)
        }
    }

    private fun updateNotification() {
        startForeground(playerNotificationManager.NOTIFICATION_ID, playerNotificationManager.buildNotification())
    }

    inner class LocalBinder : Binder() {
        fun getService() = this@PlayerService
    }

}