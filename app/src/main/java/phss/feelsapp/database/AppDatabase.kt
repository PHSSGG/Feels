package phss.feelsapp.database

import androidx.room.Database
import androidx.room.RoomDatabase
import phss.feelsapp.data.dao.PlaylistDao
import phss.feelsapp.data.dao.SongDao
import phss.feelsapp.data.models.Playlist
import phss.feelsapp.data.models.PlaylistSong
import phss.feelsapp.data.models.Song

@Database(
    entities = [Song::class, PlaylistSong::class, Playlist::class],
    version = 1
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun playlistDao(): PlaylistDao
    abstract fun songDao(): SongDao

}