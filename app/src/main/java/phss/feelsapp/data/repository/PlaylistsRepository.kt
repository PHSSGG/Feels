package phss.feelsapp.data.repository

import kotlinx.coroutines.flow.Flow
import phss.feelsapp.data.dao.PlaylistDao
import phss.feelsapp.data.models.Playlist
import phss.feelsapp.data.models.PlaylistWithSongs
import phss.feelsapp.data.models.Song
import phss.feelsapp.utils.toPlaylistSongReference

class PlaylistsRepository(
    private val playlistDao: PlaylistDao
) {

    fun getAllPlaylists(): Flow<List<Playlist>> {
        return playlistDao.loadPlaylists()
    }

    fun getPlaylistSongs(playlist: Playlist): Flow<PlaylistWithSongs> {
        return playlistDao.loadPlaylistWithSongsByName(playlist.playlistName)
    }

    fun getPlaylistById(playlistId: Long): Playlist? {
        if (!playlistDao.checkPlaylistExistsById(playlistId)) return null
        return playlistDao.loadPlaylistById(playlistId)
    }

    fun createPlaylist(playlist: Playlist) {
        playlistDao.createPlaylist(playlist)
    }

    fun deletePlaylist(playlist: Playlist) {
        playlistDao.deletePlaylist(playlist)
    }

    fun addSongToPlaylist(song: Song, playlist: Playlist) {
        addSongsToPlaylist(listOf(song), playlist)
    }

    fun addSongsToPlaylist(songs: List<Song>, playlist: Playlist) {
        playlistDao.addSongsToPlaylist(songs.map { it.toPlaylistSongReference(playlist) })
    }

    fun removeSongFromPlaylist(song: Song, playlist: Playlist) {
        removeSongsFromPlaylist(listOf(song), playlist)
    }

    fun removeSongsFromPlaylist(songs: List<Song>, playlist: Playlist) {
        playlistDao.removeSongsFromPlaylist(songs.map { it.toPlaylistSongReference(playlist) })
    }

}