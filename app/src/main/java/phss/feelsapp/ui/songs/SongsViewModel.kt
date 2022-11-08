package phss.feelsapp.ui.songs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import phss.feelsapp.data.models.Playlist
import phss.feelsapp.data.models.PlaylistWithSongs
import phss.feelsapp.data.models.Song
import phss.feelsapp.data.repository.PlaylistsRepository
import phss.feelsapp.data.repository.SongsRepository

class SongsViewModel(
    private val songsRepository: SongsRepository,
    private val playlistsRepository: PlaylistsRepository
) : ViewModel() {

    fun getPlaylistById(
        playlistId: Long,
        onFinished: (Playlist?) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            onFinished(playlistsRepository.getPlaylistById(playlistId))
        }
    }

    fun loadAllSongs(): Flow<List<Song>> {
        return songsRepository.loadAllSongs()
    }

    fun loadPlaylistWithSongs(playlist: Playlist): Flow<PlaylistWithSongs> {
        return playlistsRepository.getPlaylistSongs(playlist)
    }

    fun deleteSong(song: Song) {
        viewModelScope.launch(Dispatchers.IO) {
            songsRepository.deleteSong(song)
        }
    }

}