package phss.feelsapp.download.observers

import phss.feelsapp.data.models.RemoteSong

interface DownloadUpdateObserver {

    fun onDownloadStart(song: RemoteSong)
    fun onDownloadFinish(song: RemoteSong, success: Boolean)

    fun onDownloadProgressUpdate(song: RemoteSong, progress: Float)

}