package phss.feelsapp.data.observers

import phss.feelsapp.data.models.Playlist
import phss.feelsapp.data.models.Song

interface PlaylistsDataChangeObserver {

    fun onSongsAdded(playlist: Playlist, songs: List<Song>)
    fun onSongsRemoved(playlist: Playlist, songs: List<Song>)

    fun onPlaylistDeleted(playlist: Playlist)

}