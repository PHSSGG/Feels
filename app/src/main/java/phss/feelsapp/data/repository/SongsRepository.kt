package phss.feelsapp.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
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
            recommendations.addAll(searchForSongsRemote(it, true).take(3))
        }

        return recommendations
    }

    suspend fun searchForSongsRemote(query: String, searchForPlaylist: Boolean = false): List<RemoteSong> {
        val result = if (searchForPlaylist) songsRemoteDataSource.retrieveSongsByPlaylist(query) else songsRemoteDataSource.retrieveSongsBySearch(query)

        return if (result is DataResult.Success) result.data.map { songItem ->
            RemoteSong(songItem).also {
                it.alreadyDownloaded = songsLocalDataSource.checkIfSongAlreadyExists(it.item.key)
                it.isFromPlaylist = searchForPlaylist
            }
        }
        else listOf()
    }

    fun loadAllSongs(): Flow<List<Song>> {
        return songsLocalDataSource.getAllSongs()
    }

    fun loadAllSongsWithoutFlow(): List<Song> {
        return songsLocalDataSource.getAllSongsWithoutFlow()
    }

    fun getRecentlyAdded(): Flow<List<Song>> {
        return songsLocalDataSource.getRecentlyAdded()
    }

    fun getRecentlyPlayed(limit: Int = 5): Flow<List<Song>> {
        return songsLocalDataSource.getRecentlyPlayed(limit)
    }

    fun getLocalSongByKey(songKey: String): Song? {
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