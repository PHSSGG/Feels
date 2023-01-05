package phss.feelsapp.ui.home

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import phss.feelsapp.data.models.RemoteSong
import phss.feelsapp.data.models.Song
import phss.feelsapp.data.repository.SongsRepository
import phss.feelsapp.data.repository.UserRepository

class HomeViewModel(
    private val songsRepository: SongsRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    fun getRecommendationsList(
        onFinished: (List<RemoteSong>) -> Unit
    ) {
        GlobalScope.async {
            val user = userRepository.getUser()
            if (user == null) {
                onFinished(listOf())
                return@async
            } else onFinished(songsRepository.retrieveRecommendationList(user))
        }
    }

    fun getRecentlyPlayedList(): Flow<List<Song>> {
        return songsRepository.getRecentlyPlayed()
    }

}