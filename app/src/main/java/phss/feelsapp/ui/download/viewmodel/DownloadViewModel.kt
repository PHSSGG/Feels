package phss.feelsapp.ui.download.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import phss.feelsapp.data.models.RemoteSong
import phss.feelsapp.data.repository.SongsRepository
import phss.feelsapp.service.DownloaderService
import phss.feelsapp.utils.getOnlySongName
import phss.feelsapp.utils.getSongArtists

class DownloadViewModel(
    private val downloaderService: DownloaderService,
    private val songsRepository: SongsRepository
) : ViewModel() {

    fun downloadSong(remoteSong: RemoteSong?) {
        if (remoteSong == null) return

        var songToDownload = remoteSong
        if (remoteSong.isFromPlaylist) {
            songToDownload = runBlocking(Dispatchers.IO) {
                songsRepository.searchForSongsRemote("${remoteSong.item.getSongArtists()} ${remoteSong.item.getOnlySongName()}").first()
            }
        }
        downloaderService.downloadSong(songToDownload)
    }

    fun cancelSongDownload(song: RemoteSong?) {
        if (song == null) return
        downloaderService.cancelSongDownload(song)
    }

    fun deleteSong(remoteSong: RemoteSong?) {
        if (remoteSong == null) return

        viewModelScope.launch(Dispatchers.IO) {
            val localSong = songsRepository.getLocalSongByKey(remoteSong.item.key)

            remoteSong.alreadyDownloaded = false
            songsRepository.deleteSong(localSong)
        }
    }

}