package phss.feelsapp.data.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import phss.feelsapp.data.models.Playlist
import phss.feelsapp.data.models.PlaylistSong
import phss.feelsapp.data.models.PlaylistSongReference
import phss.feelsapp.data.models.PlaylistWithSongs

@Dao
interface PlaylistDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
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
    @Query("SELECT * FROM Playlist INNER JOIN PlaylistSong ON Playlist.playlistId=PlaylistSong.playlistId WHERE playlistName=:playlistName ORDER BY PlaylistSong.idOnPlaylist ASC")
    fun loadPlaylistWithSongsByName(playlistName: String): Flow<PlaylistWithSongs>

    @Transaction
    @Query("SELECT * FROM Playlist WHERE playlistId=:playlistId")
    fun loadPlaylistById(playlistId: Long): Playlist

    @Insert(onConflict = OnConflictStrategy.REPLACE, entity = PlaylistSong::class)
    fun addSongsToPlaylist(songs: List<PlaylistSongReference>)

    @Delete(entity = PlaylistSong::class)
    fun removeSongsFromPlaylist(references: List<PlaylistSongReference>)

}