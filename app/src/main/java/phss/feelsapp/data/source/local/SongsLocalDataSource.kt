package phss.feelsapp.data.source.local

import kotlinx.coroutines.flow.Flow
import phss.feelsapp.data.dao.SongDao
import phss.feelsapp.data.models.Song
import phss.feelsapp.data.models.SongUpdateReference
import java.io.File

class SongsLocalDataSource(
    private val songDao: SongDao
) {

    fun checkIfSongAlreadyExists(songKey: String): Boolean {
        return songDao.checkIfSongAlreadyExists(songKey)
    }

    fun getAllSongs(): Flow<List<Song>> {
        return songDao.loadSongs()
    }

    fun getRecentlyAdded(): Flow<List<Song>> {
        return songDao.loadRecentlyAdded()
    }

    fun getRecentlyPlayed(limit: Int): Flow<List<Song>> {
        return songDao.loadRecentlyPlayed(limit)
    }

    fun getSongByKey(songKey: String): Song {
        return songDao.loadSongByKey(songKey)
    }

    fun updateSong(song: Song) {
        songDao.updateSong(SongUpdateReference(song.songId, song.timesPlayed, song.lastPlayed))
    }

    fun addSong(song: Song) {
        songDao.addSong(song)
    }

    fun deleteSong(song: Song) {
        songDao.deleteSong(song)

        File(song.filePath).run {
            if (exists()) delete()
        }
        File(song.thumbnailPath).run {
            if (exists()) delete()
        }
    }

}