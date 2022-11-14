package phss.feelsapp.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import phss.feelsapp.data.models.Playlist
import phss.feelsapp.data.models.PlaylistWithSongs
import phss.feelsapp.data.models.Song
import phss.feelsapp.data.repository.PlaylistsRepository
import phss.feelsapp.data.repository.SongsRepository

class LibraryViewModel(
    private val playlistsRepository: PlaylistsRepository,
    private val songsRepository: SongsRepository
) : ViewModel() {

    fun getPlaylists(): Flow<List<Playlist>> {
        return playlistsRepository.getAllPlaylists()
    }

    fun getPlaylistWithSongs(playlist: Playlist): Flow<PlaylistWithSongs> {
        return playlistsRepository.getPlaylistSongs(playlist)
    }

    fun createPlaylist(newPlaylistName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val playlistModel = Playlist(
                playlistId = 0,
                playlistName = newPlaylistName
            )
            playlistsRepository.createPlaylist(playlistModel)
        }
    }

    fun deletePlaylist(playlist: Playlist) {
        viewModelScope.launch(Dispatchers.IO) {
            playlistsRepository.deletePlaylist(playlist)
        }
    }

    fun getRecentlyAddedSongs(): Flow<List<Song>> {
        return songsRepository.getRecentlyAdded()
    }

}