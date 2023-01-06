package phss.feelsapp.data.models

import phss.ytmusicwrapper.response.models.SongItem

class RemoteSong(
    val item: SongItem,
    var alreadyDownloaded: Boolean = false
) {

    // as playlists songs are mvs, the app will check for the field
    // to download the album song instead of the mv
    var isFromPlaylist = false
    var downloading: Boolean = false
    var playing: Boolean = false
    var downloadProgress = 0f

}