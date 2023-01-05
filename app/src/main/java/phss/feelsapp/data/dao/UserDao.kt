package phss.feelsapp.data.dao

import androidx.room.*
import phss.feelsapp.data.models.User

@Dao
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addUser(user: User)

    @Transaction
    @Query("SELECT EXISTS(SELECT * FROM User)")
    fun checkIfHasUser(): Boolean

    @Transaction
    @Query("SELECT * FROM User")
    fun loadUser(): User

}