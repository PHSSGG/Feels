package phss.feelsapp.ui.search

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import phss.feelsapp.data.repository.SongsRepository
import phss.ytmusicwrapper.response.models.SongItem

class SearchViewModel(
    private val songsRepository: SongsRepository
) : ViewModel() {

    fun getSongs(query: String, onFinished: (List<SongItem>) -> Unit) {
        if (query == "") {
            onFinished(listOf())
            return
        }
        GlobalScope.async {
            onFinished(songsRepository.searchForSongsRemote(query))
        }
    }

}