package phss.feelsapp.data.repository

import kotlinx.coroutines.flow.Flow
import phss.feelsapp.data.dao.PlaylistDao
import phss.feelsapp.data.observers.PlaylistsDataChangeObserver
import phss.feelsapp.data.models.Playlist
import phss.feelsapp.data.models.PlaylistWithSongs
import phss.feelsapp.data.models.Song
import phss.feelsapp.utils.toPlaylistSongReference
import kotlin.reflect.KClass

class PlaylistsRepository(
    private val playlistDao: PlaylistDao
) {

    private var playlistsDataChangeObservers = HashMap<KClass<*>, PlaylistsDataChangeObserver>()

    fun registerPlaylistsChangeDataObserver(clazz: KClass<*>, playlistsDataChangeObserver: PlaylistsDataChangeObserver) {
        playlistsDataChangeObservers[clazz] = playlistsDataChangeObserver
    }

    fun unregisterPlaylistsChangeDataObserver(clazz: KClass<*>) {
        playlistsDataChangeObservers.remove(clazz)
    }

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
        val playlistSongs = playlistDao.loadPlaylistsSongsByPlaylistId(playlist.playlistId)
        playlistDao.deletePlaylistSongs(playlistSongs)
        playlistDao.deletePlaylist(playlist)

        playlistsDataChangeObservers.values.forEach {
            it.onPlaylistDeleted(playlist)
        }
    }

    fun addSongToPlaylist(song: Song, playlist: Playlist) {
        addSongsToPlaylist(listOf(song), playlist)

        playlistsDataChangeObservers.values.forEach {
            it.onSongsAdded(playlist, listOf(song))
        }
    }

    fun addSongsToPlaylist(songs: List<Song>, playlist: Playlist) {
        playlistDao.addSongsToPlaylist(songs.map { it.toPlaylistSongReference(playlist) })

        playlistsDataChangeObservers.values.forEach {
            it.onSongsAdded(playlist, songs)
        }
    }

    fun removeSongFromPlaylist(song: Song, playlist: Playlist) {
        removeSongsFromPlaylist(listOf(song), playlist)

        playlistsDataChangeObservers.values.forEach {
            it.onSongsRemoved(playlist, listOf(song))
        }
    }

    fun removeSongsFromPlaylist(songs: List<Song>, playlist: Playlist) {
        playlistDao.removeSongsFromPlaylistByReferences(songs.map { it.toPlaylistSongReference(playlist) })

        playlistsDataChangeObservers.values.forEach {
            it.onSongsRemoved(playlist, songs)
        }
    }

}