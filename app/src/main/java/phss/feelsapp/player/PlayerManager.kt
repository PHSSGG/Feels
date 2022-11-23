package phss.feelsapp.player

import android.media.AudioAttributes
import android.media.MediaPlayer
import kotlinx.coroutines.*
import phss.feelsapp.data.models.Playlist
import phss.feelsapp.data.models.Song
import phss.feelsapp.player.listeners.ObservablePlayerListener
import phss.feelsapp.player.listeners.PlayerStateChangeListener

class PlayerManager : MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener {

    private val mediaPlayer = MediaPlayer().also {
        it.setOnCompletionListener(this)
        it.setOnPreparedListener(this)

        it.setAudioAttributes(
            AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        )
    }
    var playerStateChangeListener: PlayerStateChangeListener? = null
    var observablePlayerListener: ObservablePlayerListener? = null

    private var playingJob: Job? = null

    private var songs: List<Song> = listOf()
    private var currentPlaying: Int = 0
    var playingFromPlaylist: Playlist? = null

    var repeatType: RepeatType = RepeatType.NONE
    var shuffle: Boolean = false
    var alreadyPlayed = ArrayList<Song>()

    fun setupPlayer(songList: List<Song>, selected: Song, playlist: Playlist?) {
        alreadyPlayed.clear()
        songs = songList
        playingFromPlaylist = playlist

        playSong(selected)
    }

    fun getCurrentPlaying(): Song? {
        return songs.getOrNull(currentPlaying)
    }

    fun getPreviousSong(): Song? {
        return if (currentPlaying == 0) null else songs.getOrNull(currentPlaying - 1)
    }

    fun getNextSong(): Song? {
        if (shuffle) return songs.filterNot { alreadyPlayed.contains(it) }.shuffled().firstOrNull()
        return songs.getOrNull(currentPlaying + 1)
    }

    fun playSong(song: Song) {
        mediaPlayer.stop()
        mediaPlayer.reset()
        mediaPlayer.setDataSource(song.filePath)
        mediaPlayer.prepareAsync()

        getCurrentPlaying()?.isPlaying = false
        song.isPlaying = true

        observablePlayerListener?.onPlay(song, getCurrentPlaying())
        currentPlaying = songs.indexOf(song)

        alreadyPlayed.add(song)
    }

    fun playNext(forced: Boolean = false): Boolean {
        if (!forced && repeatType == RepeatType.REPEAT_SINGLE && getCurrentPlaying() != null) {
            playSong(getCurrentPlaying()!!)
            return true
        }

        val nextSong = getNextSong()
        if (nextSong == null) {
            if (forced) return false

            if (repeatType == RepeatType.REPEAT_LIST && songs.isNotEmpty()) {
                alreadyPlayed.clear()
                playSong(songs.first())
                return true
            }
            else stopPlayer()

            return false
        }

        playSong(nextSong)
        return true
    }

    fun playPrevious() {
        val previousSong = getPreviousSong() ?: return
        playSong(previousSong)
    }

    fun resumePlayer() {
        mediaPlayer.start()
        startPlayingJob()

        playerStateChangeListener?.onResume(songs[currentPlaying])
    }

    fun pausePlayer() {
        mediaPlayer.pause()
        stopPlayingJob()

        playerStateChangeListener?.onPause(songs[currentPlaying])
    }

    fun stopPlayer() {
        stopPlayingJob()

        mediaPlayer.stop()
        mediaPlayer.reset()
        alreadyPlayed.clear()

        observablePlayerListener?.onStop(songs[currentPlaying])
    }

    fun seekTo(position: Int) {
        mediaPlayer.seekTo(position)
    }

    fun isPlaying(): Boolean {
        return mediaPlayer.isPlaying
    }

    override fun onCompletion(mediaPlayer: MediaPlayer) {
        if (!playNext()) stopPlayer()
    }

    override fun onPrepared(mediaPlayer: MediaPlayer) {
        mediaPlayer.start()

        playerStateChangeListener?.onPlaying(
            songs[currentPlaying],
            mediaPlayer.duration
        )

        startPlayingJob()
    }

    private fun startPlayingJob() {
        if (playingJob != null) stopPlayingJob()

        playingJob = GlobalScope.launch(Dispatchers.IO) {
            while (isPlaying()) {
                playerStateChangeListener?.onTimeChange(
                    songs[currentPlaying],
                    getProgressPercentage(getProgress(), getSongDuration())
                )
                delay(1000)
            }
        }
    }

    private fun stopPlayingJob() {
        playingJob?.cancel()
        playingJob = null
    }

    private fun getProgressPercentage(currentPosition: Int, duration: Int): Int {
        val percentage: Double
        val current = (currentPosition / 1000).toLong()
        val total = (duration / 1000).toLong()

        percentage = current.toDouble() / total * 100

        return percentage.toInt()
    }

    fun getProgressString(): String {
        val millis = getProgress().toLong()

        val durationStringBuilder = StringBuilder()
        val hours = (millis / (1000 * 60 * 60)).toInt()
        val minutes = (millis % (1000 * 60 * 60) / (1000 * 60)).toInt()
        val seconds = (millis % (1000 * 60 * 60) % (1000 * 60) / 1000).toInt()

        durationStringBuilder
            .append(if (hours > 0) "${String.format("%02d", hours)}:" else "")
            .append("$minutes:")
            .append(String.format("%02d", seconds));

        return durationStringBuilder.toString()
    }

    fun getProgress(): Int {
        return mediaPlayer.currentPosition
    }

    fun getSongDuration(): Int {
        return mediaPlayer.duration
    }

    enum class RepeatType {
        REPEAT_LIST, REPEAT_SINGLE, NONE
    }

}