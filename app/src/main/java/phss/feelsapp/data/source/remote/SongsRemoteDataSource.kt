package phss.feelsapp.data.source.remote

import phss.feelsapp.constants.GENRE_PLAYLISTS
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

        return if (response != null && response.isSuccess && response.getOrNull() != null) {
            DataResult.Success(response.getOrNull()?.items!!)
        } else DataResult.Error(response?.exceptionOrNull() ?: IOException("Not data found with the provided search parameter"))
    }

    suspend fun retrieveSongsByPlaylist(genre: String): DataResult<List<SongItem>> {
        val response = wrapper.searchPlaylist(getPlaylistIdByGenre(genre))
        return if (response != null) DataResult.Success(response) else DataResult.Error(IOException("Not data found with the provided playlist id"))
    }

    private fun getPlaylistIdByGenre(genre: String): String {
        return GENRE_PLAYLISTS.getOrDefault(genre, "null")
    }

}