package phss.feelsapp.data.repository

import phss.feelsapp.data.dao.UserDao
import phss.feelsapp.data.models.User

class UserRepository(
    private val userDao: UserDao
) {

    fun getUser(): User? {
        if (!userDao.checkIfHasUser()) return null
        return userDao.loadUser()
    }

    fun saveUser(user: User) {
        userDao.addUser(user)
    }

}