package phss.feelsapp.player.listeners

import phss.feelsapp.data.models.Song

interface ObservablePlayerListener {

    fun onPlay(song: Song, previous: Song? = null)
    fun onStop(song: Song)

}