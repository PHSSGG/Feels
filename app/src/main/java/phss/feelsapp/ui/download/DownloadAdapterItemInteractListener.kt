package phss.feelsapp.ui.download

import phss.feelsapp.data.models.RemoteSong

interface DownloadAdapterItemInteractListener {

    fun onDownloadButtonClick(song: RemoteSong)
    fun onSongViewClick(song: RemoteSong)
    fun onDeleteButtonClick(song: RemoteSong)

}