package phss.feelsapp.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class User(
    @PrimaryKey(autoGenerate = true) val userId: Int,
    val interests: List<String>,
)