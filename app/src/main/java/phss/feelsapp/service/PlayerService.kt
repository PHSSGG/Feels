package phss.feelsapp.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import phss.feelsapp.data.observers.PlaylistsDataChangeObserver
import phss.feelsapp.data.observers.SongsDataChangeObserver
import phss.feelsapp.data.models.Playlist
import phss.feelsapp.data.models.Song
import phss.feelsapp.data.repository.PlaylistsRepository
import phss.feelsapp.data.repository.SongsRepository
import phss.feelsapp.player.PlayerManager
import phss.feelsapp.player.observers.PlayerStateChangeObserver
import phss.feelsapp.player.notification.PlayerNotificationManager
import phss.feelsapp.player.observers.PlayerObserver

class PlayerService : Service(), PlaylistsDataChangeObserver, SongsDataChangeObserver {

    private var isRunning = false

    private val binder = LocalBinder()

    val playerManager: PlayerManager by inject()
    val playerNotificationManager = PlayerNotificationManager(this)

    private val playlistsRepository: PlaylistsRepository by inject()
    private val songsRepository: SongsRepository by inject()

    override fun onBind(intent: Intent): IBinder {
        playerManager.registerPlayerStateChangeObserver(this::class, setupPlayerStateChangeObserver())
        playerManager.registerPlayerObserver(this::class, setupPlayerObserver())
        registerReceiver(playerNotificationManager, playerNotificationManager.getReceiverFilter())

        playlistsRepository.registerPlaylistsChangeDataObserver(this::class, this)
        songsRepository.registerSongsChangeDataObserver(this::class, this)

        return binder
    }

    override fun onUnbind(intent: Intent): Boolean {
        if (playerNotificationManager.isMediaSessionInitialized) {
            playerNotificationManager.mediaSession.isActive = false
            playerNotificationManager.mediaSession.release()
        }

        unregisterReceiver(playerNotificationManager)

        playerManager.stopPlayer()
        playerManager.unregisterPlayerStateChangeObserver(this::class)
        playerManager.unregisterPlayerObserver(this::class)

        playlistsRepository.unregisterPlaylistsChangeDataObserver(this::class)
        songsRepository.unregisterSongsChangeDataObserver(this::class)

        return true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        isRunning = true
        return START_NOT_STICKY
    }

    override fun onSongsAdded(playlist: Playlist, songs: List<Song>) {
        if (!playerManager.isStopped() && playerManager.playingFromPlaylist == playlist) playerManager.addSongs(songs)
    }
    override fun onSongsRemoved(playlist: Playlist, songs: List<Song>) {
        if (!playerManager.isStopped() && playerManager.playingFromPlaylist == playlist) playerManager.removeSongs(songs)
    }
    override fun onPlaylistDeleted(playlist: Playlist) {
        if (!playerManager.isStopped() && playerManager.playingFromPlaylist == playlist) playerManager.stopPlayer()
    }

    override fun onSongAdded(song: Song) {
        if (!playerManager.isStopped() && playerManager.playingFromPlaylist == null) playerManager.addSongs(listOf(song))
    }
    override fun onSongDeleted(song: Song) {
        if (!playerManager.isStopped() && playerManager.playingFromPlaylist == null) playerManager.removeSongs(listOf(song))
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

    private fun setupPlayerObserver() = object : PlayerObserver {
        override fun onPlay(song: Song, previous: Song?) {
            CoroutineScope(Dispatchers.IO).launch {
                songsRepository.updateSong(song)
                previous?.let { songsRepository.updateSong(it) }
            }
        }
        override fun onStop(song: Song?) {
            if (song == null) return
            CoroutineScope(Dispatchers.IO).launch {
                songsRepository.updateSong(song)
            }
        }

        override fun onShuffleStateChange(newSongsList: List<Song>) {}
    }

    private fun updateNotification() {
        if (playerManager.isStopped()) return
        startForeground(playerNotificationManager.NOTIFICATION_ID, playerNotificationManager.buildNotification())
    }

    inner class LocalBinder : Binder() {
        fun getService() = this@PlayerService
    }

}