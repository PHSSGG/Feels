package phss.feelsapp

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*
import org.junit.Before
import phss.feelsapp.data.dao.PlaylistDao
import phss.feelsapp.data.dao.SongDao
import phss.feelsapp.data.models.Playlist
import phss.feelsapp.data.models.PlaylistSong
import phss.feelsapp.data.models.Song
import phss.feelsapp.database.AppDatabase

@RunWith(AndroidJUnit4::class)
class DatabaseInstrumentedTest {

    lateinit var songDao: SongDao
    lateinit var playlistDao: PlaylistDao
    lateinit var db: AppDatabase

    @Before
    fun createDB() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        songDao = db.songDao()
        playlistDao = db.playlistDao()
    }

    @After
    fun close() {
        db.close()
    }

    @Test
    @Throws(Exception::class)
    fun writeSong() {
        songDao.addSong(Song(
            songId = 0,
            name = "test",
            artist = "aaa",
            album = "bb",
            duration = "0:00",
            key = "key",
            thumbnailPath = "path",
            filePath = "path"
        ))

        val song = songDao.loadSongByName("test")
        assertEquals("test", song.name)
    }

    @Test
    fun writePlaylist() {
        playlistDao.createPlaylist(Playlist(0, "playlisttest"))

        val playlist = playlistDao.loadPlaylistWithSongsByName("playlisttest")
        assertEquals("playlisttest", playlist.playlist.playlistName)
    }

    @Test
    fun writeSongInPlaylist() {
        val savedPlaylistId = playlistDao.createPlaylist(Playlist(0, "playlisttest"))
        songDao.addSong(Song(
            songId = 0,
            name = "test",
            artist = "aaa",
            album = "bb",
            duration = "0:00",
            key = "key",
            thumbnailPath = "path",
            filePath = "path"
        ))

        val song: Song = songDao.loadSongByName("test")

        playlistDao.addSongsToPlaylist(listOf(PlaylistSong(savedPlaylistId, song.songId)))

        val playlist = playlistDao.loadPlaylistWithSongsByName("playlisttest")
        assertEquals(1, playlist.songs.size)
    }

}