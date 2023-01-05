package phss.feelsapp.ui.home.interests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import phss.feelsapp.data.models.User
import phss.feelsapp.data.repository.UserRepository

class SelectInterestsViewModel(
    private val userRepository: UserRepository
) : ViewModel() {

    fun getUser(onFinished: User?.() -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            onFinished(userRepository.getUser())
        }
    }

    fun createNewUser(interests: List<String>, onCreated: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.saveUser(User(0, interests))
            onCreated()
        }
    }

}