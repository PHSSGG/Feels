package phss.feelsapp.player

import android.media.AudioAttributes
import android.media.MediaPlayer
import kotlinx.coroutines.*
import phss.feelsapp.data.models.Playlist
import phss.feelsapp.data.models.Song
import phss.feelsapp.player.observers.PlayerObserver
import phss.feelsapp.player.observers.PlayerStateChangeObserver
import phss.feelsapp.service.PlayerService
import java.time.Instant
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.reflect.KClass

class PlayerManager : MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener {

    private val mediaPlayer = MediaPlayer().also {
        it.setOnCompletionListener(this)
        it.setOnPreparedListener(this)
        it.setOnErrorListener(this)

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
    private var isChangingSong = false
    private var retryAfterError = false

    var repeatType: RepeatType = RepeatType.NONE

    var shuffle: Boolean = false
        set(value) {
            shuffledSongs = listOf()

            if (value) {
                if (field) field = false

                shuffledSongs = currentSongs.shuffled()
                currentPlaying = if (getCurrentPlaying() != null) shuffledSongs.indexOf(getCurrentPlaying()) else 0
            } else {
                val currentPlayingOnShuffle = currentSongs.find { it.key == getCurrentPlaying()?.key }
                field = false
                currentPlaying = if (currentPlayingOnShuffle != null) currentSongs.indexOf(currentPlayingOnShuffle) else 0
            }

            field = value

            if (field && currentSongs.isEmpty()) return
            if (getCurrentPlaying() != null) notifyPlayerStateChangePlaying(getCurrentPlaying()!!, getSongDuration(), getProgress())
            notifyPlayerObserverShuffleStateChange()
        }
    var shuffleAndPlay = false

    fun setupPlayer(songList: List<Song>, selected: Song, playlist: Playlist?) {
        // prevent original songs list changing to a shuffled one
        if (isSamePlaylist(playlist) && currentSongs.isNotEmpty() && !shuffleAndPlay) {
            // currentSongs.find { it.key == selected.key } ?: selected
            // this is needed due to a bug when playing the last song of a list that is already being played
            playSong(currentSongs.find { it.key == selected.key } ?: selected)
            return
        }

        if (!isStopped) stopPlayer()

        if (shuffle && !isSamePlaylist(playlist)) shuffle = false
        songs = songList
        playingFromPlaylist = playlist

        if (shuffleAndPlay) {
            shuffle = true
            shuffleAndPlay = false

            playSong(currentSongs.first())
        } else playSong(selected)
    }

    fun isSamePlaylist(playlist: Playlist?) = playingFromPlaylist?.playlistId == playlist?.playlistId

    fun getCurrentPlaying(): Song? {
        return if (!isStopped()) currentSongs.getOrNull(currentPlaying) else null
    }

    fun getCurrentSongsList() = currentSongs

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

        val previousPlaying = getCurrentPlaying()
        previousPlaying?.isPlaying = false
        song.isPlaying = true
        song.timesPlayed += 1
        song.lastPlayed = Date.from(Instant.now())

        playerObservers.values.forEach { it.onPlay(song, previousPlaying) }
        currentPlaying = currentSongs.indexOf(song)
    }

    fun playNext(forced: Boolean = false): Boolean {
        if (isChangingSong) return false
        isChangingSong = true

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
        if (getProgress() / 1000 <= 2) {
            if (isChangingSong) return
            isChangingSong = true

            val previousSong = getPreviousSong() ?: return
            playSong(previousSong)
        } else seekTo(0)
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

        playerObservers.values.forEach { it.onStop(getCurrentPlaying()) }
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
        return getCurrentPlaying() != null && mediaPlayer.isPlaying
    }

    fun isStopped(): Boolean {
        return isStopped
    }

    fun addSongs(songList: List<Song>) {
        val newList = ArrayList(songs)
        newList.addAll(songList)
        songs = newList
        if (shuffledSongs.isNotEmpty()) {
            val newShuffledSongsList = ArrayList(shuffledSongs)
            newShuffledSongsList.addAll(songList)
            shuffledSongs = newShuffledSongsList
        }

        if (getCurrentPlaying() != null) playerStateChangeObservers.values.forEach {
            it.onPlaying(getCurrentPlaying()!!, getSongDuration(), getProgress())
        }
    }

    fun removeSongs(songList: List<Song>) {
        val playingSong = getCurrentPlaying()
        val playingPosition = currentPlaying

        val newList = ArrayList(songs)
        newList.removeAll { current -> songList.find { it.key == current.key } != null }
        songs = newList
        if (shuffledSongs.isNotEmpty()) {
            val newShuffledSongsList = ArrayList(shuffledSongs)
            newShuffledSongsList.removeAll { current -> songList.find { it.key == current.key } != null }
            shuffledSongs = newShuffledSongsList
        }

        if (songList.find { it.key == playingSong?.key } != null) {
            if (playingSong != null && currentSongs.getOrNull(playingPosition) != null) {
                playingSong.isPlaying = false
                playSong(currentSongs[playingPosition])
            } else stopPlayer()
        } else if (playingSong != null) currentPlaying = currentSongs.indexOf(playingSong)

        if (getCurrentPlaying() != null) playerStateChangeObservers.values.forEach {
            it.onPlaying(getCurrentPlaying()!!, getSongDuration(), getProgress())
        }
    }

    override fun onCompletion(mediaPlayer: MediaPlayer) {
        if (!playNext()) stopPlayer()
    }

    override fun onPrepared(mediaPlayer: MediaPlayer) {
        mediaPlayer.start()
        startPlayingJob()
    }

    override fun onError(mediaPlayer: MediaPlayer, error: Int, state: Int): Boolean {
        // prevent playing the same song twice
        if (retryAfterError) return true

        if (error == -38 && getCurrentPlaying() != null) {
            retryAfterError = true
            playSong(getCurrentPlaying()!!)
        }
        return true
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

    private fun notifyPlayerObserverShuffleStateChange() {
        playerObservers[PlayerService::class]?.onShuffleStateChange(currentSongs)
        playerObservers.filterNot { it.key == PlayerService::class }.values.forEach { it.onShuffleStateChange(currentSongs) }
    }

    private fun notifyPlayerStateChangePlaying(song: Song, duration: Int, progress: Int = 0) {
        playerStateChangeObservers.forEach {
            it.value.onPlaying(song, duration, progress)
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
            while (!isPlaying()) delay(200)

            isChangingSong = false

            withContext(Dispatchers.Main) {
                notifyPlayerStateChangePlaying(
                    currentSongs[currentPlaying],
                    mediaPlayer.duration,
                    getProgress()
                )
            }

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