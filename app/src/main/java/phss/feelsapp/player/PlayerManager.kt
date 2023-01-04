package phss.feelsapp.player

import android.media.AudioAttributes
import android.media.MediaPlayer
import kotlinx.coroutines.*
import phss.feelsapp.data.models.Playlist
import phss.feelsapp.data.models.Song
import phss.feelsapp.player.observers.PlayerObserver
import phss.feelsapp.player.observers.PlayerStateChangeObserver
import java.time.Instant
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
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
    private var playerStateChangeObservers = HashMap<KClass<*>, PlayerStateChangeObserver>()
    private var playerObservers = HashMap<KClass<*>, PlayerObserver>()

    private var playingJob: Job? = null

    private var songs: List<Song> = arrayListOf()
    private var shuffledSongs: List<Song> = arrayListOf()

    private val currentSongs: List<Song> get() = if (shuffle) shuffledSongs else songs

    private var currentPlaying: Int = 0
    var playingFromPlaylist: Playlist? = null

    private var isStopped = true

    var repeatType: RepeatType = RepeatType.NONE

    var shuffle: Boolean = false
        set(value) {
            if (value) {
                shuffledSongs = currentSongs.shuffled()
                currentPlaying = if (getCurrentPlaying() != null) shuffledSongs.indexOf(getCurrentPlaying()) else 0
            } else {
                val currentPlayingOnShuffle = currentSongs.find { it.songId == getCurrentPlaying()?.songId }
                field = false
                currentPlaying = if (currentPlayingOnShuffle != null) currentSongs.indexOf(currentPlayingOnShuffle) else 0

                shuffledSongs = listOf()
            }

            field = value

            if (getCurrentPlaying() != null) playerStateChangeObservers.values.forEach {
                    it.onPlaying(getCurrentPlaying()!!, getSongDuration(), getProgress())
                }
            playerObservers.values.forEach { it.onShuffleStateChange(currentSongs) }
        }

    fun setupPlayer(songList: List<Song>, selected: Song, playlist: Playlist?) {
        songs = songList
        playingFromPlaylist = playlist

        playSong(selected)
    }

    fun getCurrentPlaying(): Song? {
        return if (!isStopped()) currentSongs.getOrNull(currentPlaying) else null
    }

    fun getPreviousSong(): Song? {
        return if (!shuffle && currentPlaying == 0) null else currentSongs.getOrNull(currentPlaying - 1)
    }

    fun getNextSong(): Song? {
        return currentSongs.getOrNull(currentPlaying + 1)
    }

    private fun playSong(song: Song) {
        isStopped = false

        mediaPlayer.stop()
        mediaPlayer.reset()
        mediaPlayer.setDataSource(song.filePath)
        mediaPlayer.prepareAsync()

        getCurrentPlaying()?.isPlaying = false
        song.isPlaying = true

        playerObservers.values.forEach { it.onPlay(song, getCurrentPlaying()) }
        currentPlaying = songs.indexOf(song)
    }

    fun playNext(forced: Boolean = false): Boolean {
        if (!forced && repeatType == RepeatType.REPEAT_SINGLE && getCurrentPlaying() != null) {
            playSong(getCurrentPlaying()!!)
            return true
        }

        val nextSong = getNextSong()
        if (nextSong == null) {
            if (forced) return false

            if (repeatType == RepeatType.REPEAT_LIST && currentSongs.isNotEmpty()) {
                playSong(currentSongs.first())
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

        notifyPlayerStateChangeResume(currentSongs[currentPlaying])
    }

    fun pausePlayer() {
        mediaPlayer.pause()
        stopPlayingJob()

        notifyPlayerStateChangePause(currentSongs[currentPlaying])
    }

    fun stopPlayer() {
        if (isStopped) return

        stopPlayingJob()

        mediaPlayer.stop()
        mediaPlayer.reset()

        getCurrentPlaying()?.isPlaying = false

        playerObservers.values.forEach { it.onStop(getCurrentPlaying()!!) }
        notifyPlayerStateChangeStop()

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

        notifyPlayerStateChangePlaying(
            currentSongs[currentPlaying],
            mediaPlayer.duration
        )

        startPlayingJob()
    }

    fun registerPlayerStateChangeObserver(clazz: KClass<*>, playerStateChangeObserver: PlayerStateChangeObserver) {
        playerStateChangeObservers[clazz] = playerStateChangeObserver
    }

    fun unregisterPlayerStateChangeObserver(clazz: KClass<*>) {
        playerStateChangeObservers.remove(clazz)
    }

    fun registerPlayerObserver(clazz: KClass<*>, playerObserver: PlayerObserver) {
        playerObservers[clazz] = playerObserver
    }

    fun unregisterPlayerObserver(clazz: KClass<*>) {
        playerObservers.remove(clazz)
    }

    private fun notifyPlayerStateChangePlaying(song: Song, duration: Int) {
        playerStateChangeObservers.forEach {
            it.value.onPlaying(song, duration)
        }
    }

    private fun notifyPlayerStateChangeTimeChange(song: Song, timePercent: Int) {
        playerStateChangeObservers.forEach {
            it.value.onTimeChange(song, timePercent)
        }
    }

    private fun notifyPlayerStateChangeResume(song: Song) {
        playerStateChangeObservers.forEach {
            it.value.onResume(song)
        }
    }

    private fun notifyPlayerStateChangePause(song: Song) {
        playerStateChangeObservers.forEach {
            it.value.onPause(song)
        }
    }

    private fun notifyPlayerStateChangeStop() {
        playerStateChangeObservers.forEach {
            it.value.onStop()
        }
    }

    private fun startPlayingJob() {
        if (playingJob != null) stopPlayingJob()

        playingJob = GlobalScope.launch(Dispatchers.IO) {
            while (isPlaying()) {
                notifyPlayerStateChangeTimeChange(
                    currentSongs[currentPlaying],
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