package phss.feelsapp.data.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import phss.feelsapp.data.models.Playlist
import phss.feelsapp.data.models.PlaylistSong
import phss.feelsapp.data.models.PlaylistWithSongs
import phss.feelsapp.data.models.Song

@Dao
interface PlaylistDao {

    @Insert
    fun createPlaylist(playlistEntity: Playlist): Long

    @Delete
    fun deletePlaylist(playlistEntity: Playlist)

    @Query("SELECT EXISTS(SELECT * FROM Playlist WHERE playlistId=:playlistId)")
    fun checkPlaylistExistsById(playlistId: Long): Boolean

    @Query("SELECT EXISTS(SELECT * FROM Playlist WHERE playlistName=:playlistName)")
    fun checkPlaylistExistsByName(playlistName: String): Boolean

    @Transaction
    @Query("SELECT * FROM Playlist")
    fun loadPlaylists(): Flow<List<Playlist>>

    @Transaction
    @Query("SELECT * FROM Playlist WHERE playlistName=:playlistName")
    fun loadPlaylistWithSongsByName(playlistName: String): Flow<PlaylistWithSongs>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addSongsToPlaylist(songs: List<PlaylistSong>)

    @Delete
    fun removeSongsFromPlaylist(songs: List<Song>)

}