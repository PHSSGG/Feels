package phss.feelsapp.ui.home

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.*
import phss.feelsapp.data.repository.SongsRepository
import phss.ytmusicwrapper.response.models.SongItem

class HomeViewModel(
    private val repository: SongsRepository
) : ViewModel() {

    fun getRecommendationsList(
        onFinished: (List<SongItem>) -> Unit
    ) {
        GlobalScope.async {
            onFinished(repository.retrieveRecommendationList())
        }
    }

}