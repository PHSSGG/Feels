package phss.feelsapp.player

import android.media.AudioAttributes
import android.media.MediaPlayer
import kotlinx.coroutines.*
import phss.feelsapp.data.models.Playlist
import phss.feelsapp.data.models.Song
import phss.feelsapp.player.listeners.ObservablePlayerListener
import phss.feelsapp.player.listeners.PlayerStateChangeListener
import kotlin.reflect.KClass

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
    var playerStateChangeListeners = HashMap<KClass<*>, PlayerStateChangeListener>()
    var observablePlayerListener: ObservablePlayerListener? = null

    private var playingJob: Job? = null

    private var songs: List<Song> = listOf()
    private var currentPlaying: Int = 0
    var playingFromPlaylist: Playlist? = null

    private var isStopped = false

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
        return if (!isStopped()) songs.getOrNull(currentPlaying) else null
    }

    fun getPreviousSong(): Song? {
        return if (currentPlaying == 0) null else songs.getOrNull(currentPlaying - 1)
    }

    fun getNextSong(): Song? {
        if (shuffle) return songs.filterNot { alreadyPlayed.contains(it) }.shuffled().firstOrNull()
        return songs.getOrNull(currentPlaying + 1)
    }

    fun playSong(song: Song) {
        isStopped = false

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

        notifyPlayerStateChangeListenerResume(songs[currentPlaying])
    }

    fun pausePlayer() {
        mediaPlayer.pause()
        stopPlayingJob()

        notifyPlayerStateChangeListenerPause(songs[currentPlaying])
    }

    fun stopPlayer() {
        if (isStopped) return

        stopPlayingJob()

        mediaPlayer.stop()
        mediaPlayer.reset()
        alreadyPlayed.clear()

        getCurrentPlaying()?.isPlaying = false

        observablePlayerListener?.onStop(getCurrentPlaying()!!)
        notifyPlayerStateChangeListenerStop()

        isStopped = true
    }

    fun seekTo(position: Int) {
        mediaPlayer.seekTo(position)
    }

    fun backward5() {
        mediaPlayer.seekTo(getProgress() - 5000)
    }

    fun forward5() {
        mediaPlayer.seekTo(getProgress() + 5000)
    }

    fun isPlaying(): Boolean {
        return mediaPlayer.isPlaying
    }

    fun isStopped(): Boolean {
        return isStopped
    }

    override fun onCompletion(mediaPlayer: MediaPlayer) {
        if (!playNext()) stopPlayer()
    }

    override fun onPrepared(mediaPlayer: MediaPlayer) {
        mediaPlayer.start()

        notifyPlayerStateChangeListenerPlaying(
            songs[currentPlaying],
            mediaPlayer.duration
        )

        startPlayingJob()
    }

    fun registerPlayerStateChangeListener(clazz: KClass<*>, playerStateChangeListener: PlayerStateChangeListener) {
        playerStateChangeListeners[clazz] = playerStateChangeListener
    }

    fun unregisterPlayerStateChangeListener(clazz: KClass<*>) {
        playerStateChangeListeners.remove(clazz)
    }

    private fun notifyPlayerStateChangeListenerPlaying(song: Song, duration: Int) {
        playerStateChangeListeners.forEach {
            it.value.onPlaying(song, duration)
        }
    }

    private fun notifyPlayerStateChangeListenerTimeChange(song: Song, timePercent: Int) {
        playerStateChangeListeners.forEach {
            it.value.onTimeChange(song, timePercent)
        }
    }

    private fun notifyPlayerStateChangeListenerResume(song: Song) {
        playerStateChangeListeners.forEach {
            it.value.onResume(song)
        }
    }

    private fun notifyPlayerStateChangeListenerPause(song: Song) {
        playerStateChangeListeners.forEach {
            it.value.onPause(song)
        }
    }

    private fun notifyPlayerStateChangeListenerStop() {
        playerStateChangeListeners.forEach {
            it.value.onStop()
        }
    }

    private fun startPlayingJob() {
        if (playingJob != null) stopPlayingJob()

        playingJob = GlobalScope.launch(Dispatchers.IO) {
            while (isPlaying()) {
                notifyPlayerStateChangeListenerTimeChange(
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