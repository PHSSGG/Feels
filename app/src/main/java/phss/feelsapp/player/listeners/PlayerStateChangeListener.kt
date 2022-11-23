package phss.feelsapp.player.listeners

import phss.feelsapp.data.models.Song

interface PlayerStateChangeListener {

    fun onPlaying(song: Song, duration: Int)
    fun onTimeChange(song: Song, timePercent: Int)

    fun onResume(song: Song)
    fun onPause(song: Song)
    fun onStop()

}