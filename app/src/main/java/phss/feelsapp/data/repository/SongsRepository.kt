package phss.feelsapp.data.repository

import kotlinx.coroutines.flow.Flow
import phss.feelsapp.data.DataResult
import phss.feelsapp.data.dao.SongDao
import phss.feelsapp.data.models.RemoteSong
import phss.feelsapp.data.models.Song
import phss.feelsapp.data.source.local.SongsLocalDataSource
import phss.feelsapp.data.source.remote.SongsRemoteDataSource
import phss.ytmusicwrapper.response.models.SongItem

class SongsRepository(
    private val songsLocalDataSource: SongsLocalDataSource,
    private val songsRemoteDataSource: SongsRemoteDataSource
) {

    suspend fun retrieveRecommendationList(): List<RemoteSong> {
        // TODO: Get recommendations by user's recently activity

        val recommendations = ArrayList<RemoteSong>()
        recommendations.addAll(searchForSongsRemote("genre pop").take(3))
        recommendations.addAll(searchForSongsRemote("funk").take(3))
        recommendations.addAll(searchForSongsRemote("genre sertanejo").take(3))
        recommendations.addAll(searchForSongsRemote("kpop").take(3))
        recommendations.addAll(searchForSongsRemote("rock").take(3))

        return recommendations
    }

    suspend fun searchForSongsRemote(query: String): List<RemoteSong> {
        val result = songsRemoteDataSource.retrieveSongsBySearch(query)

        return if (result is DataResult.Success) result.data.map { songItem ->
            RemoteSong(songItem).also {
                it.alreadyDownloaded = songsLocalDataSource.checkIfSongAlreadyExists(it.item.key)
            }
        }
        else listOf()
    }

    fun loadAllSongs(): Flow<List<Song>> {
        return songsLocalDataSource.getAllSongs()
    }

    fun getRecentlyAdded(): Flow<List<Song>> {
        return songsLocalDataSource.getRecentlyAdded()
    }

    fun getLocalSongByKey(songKey: String): Song {
        return songsLocalDataSource.getSongByKey(songKey)
    }

    fun addSong(song: Song) {
        songsLocalDataSource.addSong(song)
    }

    fun deleteSong(song: Song?) {
        if (song == null) return
        songsLocalDataSource.deleteSong(song)
    }

}