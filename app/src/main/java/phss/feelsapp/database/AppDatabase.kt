package phss.feelsapp.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import phss.feelsapp.data.dao.PlaylistDao
import phss.feelsapp.data.dao.SongDao
import phss.feelsapp.data.dao.UserDao
import phss.feelsapp.data.models.Playlist
import phss.feelsapp.data.models.PlaylistSong
import phss.feelsapp.data.models.Song
import phss.feelsapp.data.models.User
import phss.feelsapp.database.converter.Converters

@Database(
    entities = [User::class, Song::class, PlaylistSong::class, Playlist::class],
    version = 2
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun playlistDao(): PlaylistDao
    abstract fun songDao(): SongDao
    abstract fun userDao(): UserDao

}

class DatabaseMigrations() {

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE User (`userId` INTEGER PRIMARY KEY NOT NULL, `interests` TEXT NOT NULL);")
                database.execSQL("CREATE TABLE Song_new (songId INTEGER PRIMARY KEY NOT NULL, name TEXT NOT NULL, artist TEXT NOT NULL, album TEXT NOT NULL, duration TEXT NOT NULL, `key` TEXT NOT NULL, isPlaying INTEGER NOT NULL, timesPlayed INTEGER NOT NULL DEFAULT 0, lastPlayed INTEGER DEFAULT null, thumbnailPath TEXT NOT NULL, filePath TEXT NOT NULL);");
                database.execSQL("INSERT INTO Song_new (songId, name, artist, album, duration, `key`, isPlaying, timesPlayed, lastPlayed, thumbnailPath, filePath) SELECT songId, name, artist, album, duration, `key`, isPlaying, 0, null, thumbnailPath, filePath FROM Song;");
                database.execSQL("DROP TABLE Song");
                database.execSQL("ALTER TABLE Song_new RENAME TO Song");
            }
        }
    }

}