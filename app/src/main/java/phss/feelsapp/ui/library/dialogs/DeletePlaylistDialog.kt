package phss.feelsapp.ui.library.dialogs

import android.app.Dialog
import android.content.Context
import android.view.Window
import android.widget.Button
import android.widget.TextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import phss.feelsapp.R
import phss.feelsapp.data.models.Playlist
import phss.feelsapp.ui.library.LibraryViewModel

class DeletePlaylistDialog(
    context: Context,
    private val scope: CoroutineScope,
    private val libraryViewModel: LibraryViewModel,
    private val playlist: Playlist,
) : Dialog(context) {

    fun openDialog() {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setCancelable(true)
        setContentView(R.layout.playlist_delete_confirm_dialog)

        val playlistDeleteDialogInfo: TextView = findViewById(R.id.playlistDeleteDialogInfoName)
        scope.launch {
            libraryViewModel.getPlaylistWithSongs(playlist).collectLatest {
                val amountOfSongs = if (it != null && it.songs != null) it.songs.size else 0
                playlistDeleteDialogInfo.text = playlistDeleteDialogInfo.text.toString().replace("{playlist}", playlist.playlistName).replace("{amount}", "$amountOfSongs")
            }
        }

        findViewById<Button>(R.id.playlistDeleteDialogCancelButton).setOnClickListener {
            dismiss()
        }
        findViewById<Button>(R.id.playlistDeleteDialogDeleteButton).setOnClickListener {
            libraryViewModel.deletePlaylist(playlist)
            dismiss()
        }

        show()
    }

}