package phss.feelsapp.ui.library.adapters.playlists

import phss.feelsapp.data.models.Playlist

interface PlaylistAdapterItemInteractListener {

    fun onLongClick(playlist: Playlist)
    fun onClick(playlist: Playlist)

}