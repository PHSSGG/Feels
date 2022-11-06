package phss.feelsapp.ui.home

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.*
import phss.feelsapp.data.models.RemoteSong
import phss.feelsapp.data.repository.SongsRepository

class HomeViewModel(
    private val repository: SongsRepository
) : ViewModel() {

    fun getRecommendationsList(
        onFinished: (List<RemoteSong>) -> Unit
    ) {
        GlobalScope.async {
            onFinished(repository.retrieveRecommendationList())
        }
    }

}