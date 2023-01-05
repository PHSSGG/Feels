package phss.feelsapp.data.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity
data class Song(
    @PrimaryKey(autoGenerate = true) val songId: Long,
    val name: String,
    val artist: String,
    val album: String,
    val duration: String,
    val key: String,
    @ColumnInfo(defaultValue = "0") var timesPlayed: Long,
    @ColumnInfo(defaultValue = "null") var lastPlayed: Date?,
    val thumbnailPath: String,
    val filePath: String
) {

    var isPlaying = false

}

@Entity
data class PlaylistSong(
    @PrimaryKey(autoGenerate = true) val idOnPlaylist: Long,
    val playlistId: Long,
    @ColumnInfo(index = true) val songId: Long
)

class SongUpdateReference(
    val songId: Long,
    var timesPlayed: Long,
    var lastPlayed: Date?,
)

class PlaylistSongReference(
    val playlistId: Long,
    val songId: Long
)