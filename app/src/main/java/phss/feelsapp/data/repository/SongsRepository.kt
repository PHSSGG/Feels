package phss.feelsapp.data.repository

import android.util.Log
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import phss.feelsapp.data.DataResult
import phss.feelsapp.data.source.local.SongsLocalDataSource
import phss.feelsapp.data.source.remote.SongsRemoteDataSource
import phss.ytmusicwrapper.response.models.SongItem

class SongsRepository(
    private val songsLocalDataSource: SongsLocalDataSource,
    private val songsRemoteDataSource: SongsRemoteDataSource
) {

    suspend fun retrieveRecommendationList(): List<SongItem> {
        // TODO: Get recommendations by user's recently activity

        val recommendations = ArrayList<SongItem>()
        recommendations.addAll(searchForSongsRemote("pop genre").take(3))
        recommendations.addAll(searchForSongsRemote("funk").take(3))
        recommendations.addAll(searchForSongsRemote("sertanejo").take(3))
        recommendations.addAll(searchForSongsRemote("kpop").take(3))
        recommendations.addAll(searchForSongsRemote("rock").take(3))

        return recommendations
    }

    suspend fun searchForSongsRemote(query: String): List<SongItem> {
        val result = songsRemoteDataSource.retrieveSongsBySearch(query)

        return if (result is DataResult.Success) result.data
        else listOf()
    }

}