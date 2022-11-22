package phss.feelsapp.ui.library.dialogs

import android.app.Dialog
import android.content.Context
import android.view.Window
import android.widget.Button
import com.google.android.material.textfield.TextInputEditText
import phss.feelsapp.R
import phss.feelsapp.ui.library.LibraryViewModel

class CreatePlaylistDialog(
    context: Context,
    private val libraryViewModel: LibraryViewModel,
) : Dialog(context) {

    fun openDialog() {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setCancelable(true)
        setContentView(R.layout.library_playlist_create_dialog)

        val playlistEditText: TextInputEditText = findViewById(R.id.playlistCreateDialogPlaylistEditText)
        val createButton: Button = findViewById(R.id.playlistCreateDialogCreateButton)
        val cancelButton: Button = findViewById(R.id.playlistCreateDialogCancelButton)

        createButton.setOnClickListener {
            val playlistName = playlistEditText.text
            if (playlistName.isNullOrBlank()) return@setOnClickListener

            libraryViewModel.createPlaylist(playlistName.toString())
            dismiss()
        }
        cancelButton.setOnClickListener { cancel() }

        show()
    }

}