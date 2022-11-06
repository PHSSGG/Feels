package phss.feelsapp.data.models

import phss.ytmusicwrapper.response.models.SongItem

class RemoteSong(
    val item: SongItem,
    var alreadyDownloaded: Boolean = false
) {

    var downloading: Boolean = false
    var playing: Boolean = false
    var downloadProgress = 0f

}