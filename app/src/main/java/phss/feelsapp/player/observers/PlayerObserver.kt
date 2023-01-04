package phss.feelsapp.player.observers

import phss.feelsapp.data.models.Song

interface PlayerObserver {

    fun onPlay(song: Song, previous: Song? = null)
    fun onStop(song: Song)

    fun onShuffleStateChange(newSongsList: List<Song>)

}