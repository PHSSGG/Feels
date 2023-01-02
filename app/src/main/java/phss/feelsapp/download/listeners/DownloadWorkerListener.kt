package phss.feelsapp.download.listeners

import phss.feelsapp.data.models.RemoteSong

interface DownloadWorkerListener {

    fun onProgressUpdate(song: RemoteSong, progress: Float)
    fun onFinish(song: RemoteSong, success: Boolean, error: Boolean)

}