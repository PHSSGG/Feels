package phss.feelsapp.download.listeners

import phss.feelsapp.data.models.RemoteSong

interface DownloadUpdateListener {

    fun onDownloadStart(song: RemoteSong)
    fun onDownloadFinish(song: RemoteSong, success: Boolean)

    fun onDownloadProgressUpdate(song: RemoteSong, progress: Float)

}