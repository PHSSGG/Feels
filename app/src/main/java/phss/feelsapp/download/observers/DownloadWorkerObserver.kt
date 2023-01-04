package phss.feelsapp.download.observers

import phss.feelsapp.data.models.RemoteSong

interface DownloadWorkerObserver {

    fun onProgressUpdate(song: RemoteSong, progress: Float)
    fun onFinish(song: RemoteSong, success: Boolean, error: Boolean)

}