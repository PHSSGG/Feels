package phss.feelsapp.data.source.remote

import phss.feelsapp.data.DataResult
import phss.ytmusicwrapper.YTMusicAPIWrapper
import phss.ytmusicwrapper.request.SearchBody
import phss.ytmusicwrapper.response.models.SongItem
import java.io.IOException

class SongsRemoteDataSource(
    private val wrapper: YTMusicAPIWrapper
) {

    suspend fun retrieveSongsBySearch(search: String): DataResult<List<SongItem>> {
        val body = SearchBody(query = search)
        val response = wrapper.searchPage(body)

        return if (response.isSuccess && response.getOrNull() != null) {
            DataResult.Success(response.getOrNull()?.items!!)
        } else DataResult.Error(response.exceptionOrNull() ?: IOException("Not data found with the provided search parameter"))
    }

}