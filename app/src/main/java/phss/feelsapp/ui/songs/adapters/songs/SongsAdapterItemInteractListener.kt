package phss.feelsapp.ui.songs.adapters.songs

import phss.feelsapp.data.models.Song

interface SongsAdapterItemInteractListener {

    fun onLongClick(song: Song)
    fun onClick(song: Song)

}