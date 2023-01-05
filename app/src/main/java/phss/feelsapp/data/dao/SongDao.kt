package phss.feelsapp.data.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import phss.feelsapp.data.models.PlaylistWithSongs
import phss.feelsapp.data.models.Song
import phss.feelsapp.data.models.SongUpdateReference

@Dao
interface SongDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addSong(songEntity: Song): Long

    @Delete
    fun deleteSong(songEntity: Song)

    @Update(onConflict = OnConflictStrategy.REPLACE, entity = Song::class)
    fun updateSong(songUpdateReference: SongUpdateReference)

    @Transaction
    @Query("SELECT EXISTS(SELECT * FROM Song WHERE `key`=:songKey)")
    fun checkIfSongAlreadyExists(songKey: String): Boolean

    @Transaction
    @Query("SELECT * FROM Song")
    fun loadSongs(): Flow<List<Song>>

    @Transaction
    @Query("SELECT * FROM Song ORDER BY songId DESC LIMIT 5")
    fun loadRecentlyAdded(): Flow<List<Song>>

    @Transaction
    @Query("SELECT * FROM Song WHERE name=:songName")
    fun loadSongByName(songName: String): Song

    @Transaction
    @Query("SELECT * FROM Song WHERE `key`=:songKey")
    fun loadSongByKey(songKey: String): Song

}