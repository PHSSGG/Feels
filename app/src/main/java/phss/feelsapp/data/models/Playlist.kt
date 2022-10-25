package phss.feelsapp.data.models

import androidx.room.*

@Entity
data class Playlist(
    @PrimaryKey(autoGenerate = true) val playlistId: Long,
    val playlistName: String
)

data class PlaylistWithSongs(
    @Embedded val playlist: Playlist,
    @Relation(
        parentColumn = "playlistId",
        entityColumn = "songId",
        entity = Song::class,
        associateBy = Junction(PlaylistSong::class)
    )
    val songs: List<Song>
)