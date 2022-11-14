package phss.feelsapp.utils

import phss.feelsapp.data.models.Playlist
import phss.feelsapp.data.models.PlaylistSongReference
import phss.feelsapp.data.models.Song

fun Song.toPlaylistSongReference(playlist: Playlist) = PlaylistSongReference(
    playlistId = playlist.playlistId,
    songId = this.songId
)