package phss.feelsapp.ui.recently

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.fragment.app.Fragment
import phss.feelsapp.data.models.Playlist
import phss.feelsapp.data.models.Song
import phss.feelsapp.player.observers.PlayerObserver
import phss.feelsapp.service.PlayerService
import phss.feelsapp.ui.songs.adapters.songs.SongsAdapterItemInteractListener

class RecentlyPlayer(
    val context: Context,
    val activity: Activity,
    val fragment: Fragment,
    var recentlyAdapter: RecentlyAdapter? = null,
) {

    var currentListOfSongs: List<Song> = ArrayList()
        set(value) {
            if (::playerService.isInitialized) {
                if (playerService.playerManager.playingFromPlaylist == playlist) {
                    val currentPlaying = playerService.playerManager.getCurrentPlaying()
                    value.forEach { it.isPlaying = currentPlaying?.key == it.key }
                }

            } else value.forEach { it.isPlaying = false }

            field = value
            recentlyAdapter?.updateList(currentListOfSongs)
        }
    val playlist = Playlist(Long.MIN_VALUE, "recently_played:${fragment::class.simpleName}")

    lateinit var playerService: PlayerService

    fun onResume() {
        setupPlayerObserver()
    }
    fun onPause() {
        playerService.playerManager.unregisterPlayerObserver(fragment::class)
    }

    private val playerServiceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            playerService.playerManager.unregisterPlayerObserver(fragment::class)
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as PlayerService.LocalBinder
            playerService = binder.getService()

            val currentPlaying = playerService.playerManager.getCurrentPlaying()
            if (currentPlaying != null) {
                setPlaying(currentPlaying, true)
                recentlyAdapter?.updateList(currentListOfSongs)
            }

            setupPlayerObserver()
        }
    }
    fun bindPlayerService() {
        val playerServiceIntent = Intent(context, PlayerService::class.java)
        activity.bindService(playerServiceIntent, playerServiceConnection, Context.BIND_AUTO_CREATE)
    }

    fun setupPlayerObserver() {
        if (!::playerService.isInitialized) return

        playerService.playerManager.registerPlayerObserver(fragment::class, object : PlayerObserver {
            override fun onPlay(song: Song, previous: Song?) {
                activity.runOnUiThread {
                    setPlaying(previous, false)
                    setPlaying(song, true)
                    recentlyAdapter?.updateList(currentListOfSongs)
                }
            }
            override fun onStop(song: Song?) {
                if (song == null) return
                activity.runOnUiThread {
                    setPlaying(song, false)
                    recentlyAdapter?.updateList(currentListOfSongs)
                }
            }

            override fun onShuffleStateChange(newSongsList: List<Song>) {}
        })
    }

    fun setupSongItemInteractListener(): SongsAdapterItemInteractListener {
        return object : SongsAdapterItemInteractListener {
            override fun onClick(song: Song) {
                val previousSong = playerService.playerManager.getCurrentPlaying()
                if (playerService.playerManager.isSamePlaylist(playlist) && previousSong != null && previousSong.key == song.key && song.isPlaying) {
                    if (playerService.playerManager.isPlaying()) playerService.playerManager.pausePlayer()
                    else playerService.playerManager.resumePlayer()
                    return
                }

                playerService.playerManager.setupPlayer(currentListOfSongs, song, playlist)

                setPlaying(previousSong, false)
                setPlaying(song, true)

                recentlyAdapter?.updateList(currentListOfSongs)
            }
            override fun onLongClick(song: Song) {}
        }
    }

    private fun setPlaying(song: Song?, playing: Boolean) {
        currentListOfSongs.find { selected -> selected.key == song?.key }?.run { isPlaying = playing }
    }

}