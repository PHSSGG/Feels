package phss.feelsapp.data.repository

import kotlinx.coroutines.flow.Flow
import phss.feelsapp.data.DataResult
import phss.feelsapp.data.observers.SongsDataChangeObserver
import phss.feelsapp.data.models.RemoteSong
import phss.feelsapp.data.models.Song
import phss.feelsapp.data.models.User
import phss.feelsapp.data.source.local.SongsLocalDataSource
import phss.feelsapp.data.source.remote.SongsRemoteDataSource
import kotlin.reflect.KClass

class SongsRepository(
    private val songsLocalDataSource: SongsLocalDataSource,
    private val songsRemoteDataSource: SongsRemoteDataSource
) {

    private var songsDataChangeObservers = HashMap<KClass<*>, SongsDataChangeObserver>()

    fun registerSongsChangeDataObserver(clazz: KClass<*>, songsDataChangeObserver: SongsDataChangeObserver) {
        songsDataChangeObservers[clazz] = songsDataChangeObserver
    }

    fun unregisterSongsChangeDataObserver(clazz: KClass<*>) {
        songsDataChangeObservers.remove(clazz)
    }

    suspend fun retrieveRecommendationList(user: User): List<RemoteSong> {
        val recommendations = ArrayList<RemoteSong>()
        user.interests.forEach {
            recommendations.addAll(searchForSongsRemote(it).take(3))
        }

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

    fun updateSong(song: Song) {
        if (!songsLocalDataSource.checkIfSongAlreadyExists(song.key)) return
        songsLocalDataSource.updateSong(song)
    }

    fun addSong(song: Song) {
        songsLocalDataSource.addSong(song)

        songsDataChangeObservers.values.forEach {
            it.onSongAdded(song)
        }
    }

    fun deleteSong(song: Song?) {
        if (song == null) return
        songsLocalDataSource.deleteSong(song)

        songsDataChangeObservers.values.forEach {
            it.onSongDeleted(song)
        }
    }

}