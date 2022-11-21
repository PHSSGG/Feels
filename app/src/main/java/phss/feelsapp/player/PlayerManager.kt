package phss.feelsapp.player

import android.media.MediaPlayer
import phss.feelsapp.data.models.Playlist
import phss.feelsapp.data.models.Song

class PlayerManager : MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener {

    private val mediaPlayer = MediaPlayer().also {
        it.setOnCompletionListener(this)
        it.setOnPreparedListener(this)
    }

    private var songs: List<Song> = listOf()
    private var currentPlaying: Int = 0
    var playingFromPlaylist: Playlist? = null

    fun setupPlayer(songList: List<Song>, selected: Song, playlist: Playlist?) {
        songs = songList
        playingFromPlaylist = playlist

        playSong(selected)
    }

    fun getCurrentPlaying(): Song? {
        return songs.getOrNull(currentPlaying)
    }

    fun playSong(song: Song) {
        mediaPlayer.stop()
        mediaPlayer.reset()
        mediaPlayer.setDataSource(song.filePath)
        mediaPlayer.prepareAsync()

        getCurrentPlaying()?.isPlaying = false
        song.isPlaying = true

        currentPlaying = songs.indexOf(song)
    }

    fun playNext() {
        val nextSong = songs.getOrNull(currentPlaying + 1)
        if (nextSong == null) {
            songs.firstOrNull()?.apply { playSong(this) }
            return
        }

        playSong(nextSong)
    }

    fun resumePlayer() {
        mediaPlayer.start()
    }

    fun pausePlayer() {
        mediaPlayer.pause()
    }

    fun isPlaying(): Boolean {
        return mediaPlayer.isPlaying
    }

    override fun onCompletion(mediaPlayer: MediaPlayer) {
        playNext()
    }

    override fun onPrepared(mediaPlayer: MediaPlayer) {
        mediaPlayer.start()
    }

}