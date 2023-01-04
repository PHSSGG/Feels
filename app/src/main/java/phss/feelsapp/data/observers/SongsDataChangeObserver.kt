package phss.feelsapp.data.observers

import phss.feelsapp.data.models.Song

interface SongsDataChangeObserver {

    fun onSongAdded(song: Song)
    fun onSongDeleted(song: Song)

}