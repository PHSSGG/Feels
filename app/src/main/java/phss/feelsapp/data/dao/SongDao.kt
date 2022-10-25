package phss.feelsapp.data.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import phss.feelsapp.data.models.PlaylistWithSongs
import phss.feelsapp.data.models.Song

@Dao
interface SongDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addSong(songEntity: Song): Long

    @Delete
    fun deleteSong(songEntity: Song)

    @Transaction
    @Query("SELECT * FROM Song")
    fun loadSongs(): Flow<List<Song>>

    @Transaction
    @Query("SELECT * FROM Song WHERE name=:songName")
    fun loadSongByName(songName: String): Song

}