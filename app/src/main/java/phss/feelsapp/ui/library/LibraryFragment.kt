package phss.feelsapp.ui.library

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.coroutineScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.textfield.TextInputEditText
import koleton.api.hideSkeleton
import koleton.api.loadSkeleton
import org.koin.android.ext.android.inject
import phss.feelsapp.R
import phss.feelsapp.data.models.Playlist
import phss.feelsapp.databinding.FragmentLibraryBinding
import phss.feelsapp.ui.library.adapters.playlists.PlaylistAdapterItemInteractListener
import phss.feelsapp.ui.library.adapters.playlists.PlaylistsAdapter
import phss.feelsapp.ui.recently.RecentlyAdapter
import phss.feelsapp.ui.songs.SongsFragment

class LibraryFragment : Fragment() {

    private val libraryViewModel: LibraryViewModel by inject()

    private var _binding: FragmentLibraryBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLibraryBinding.inflate(inflater, container, false)

        setupPlaylistsView()
        setupRecentlyAddedView()
        setupCreatePlaylistButton()
        setupAllSongsView()

        return binding.root
    }

    private fun setupPlaylistsView() {
        binding.playlistsRecyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        updatePlaylistsView()
    }

    private fun updatePlaylistsView() {
        val playlistsAdapter = PlaylistsAdapter(listOf(), setupPlaylistItemInteractListener())
        binding.playlistsRecyclerView.adapter = playlistsAdapter

        lifecycle.coroutineScope.launchWhenStarted {
            libraryViewModel.getPlaylists().collect {
                playlistsAdapter.updateList(it)
            }
        }
    }

    private fun setupPlaylistItemInteractListener(): PlaylistAdapterItemInteractListener {
        return object : PlaylistAdapterItemInteractListener {
            override fun onClick(playlist: Playlist) {
                setFragmentResult("requestKey", bundleOf("playlistId" to playlist.playlistId))
                openSongsFragment()
            }

            override fun onLongClick(playlist: Playlist) {
                libraryViewModel.deletePlaylist(playlist)
            }
        }
    }

    private fun setupRecentlyAddedView() {
        binding.libraryRecentlyRecyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        updateRecentlyAddedView()
    }

    private fun updateRecentlyAddedView() {
        val recentlyAdapter = RecentlyAdapter(listOf())
        binding.libraryRecentlyRecyclerView.adapter = recentlyAdapter

        binding.libraryRecentlyRecyclerView.loadSkeleton(R.layout.recently_song_view) {
            itemCount(3)
        }

        lifecycle.coroutineScope.launchWhenStarted {
            libraryViewModel.getRecentlyAddedSongs().collect { songsList ->
                binding.libraryRecentlyRecyclerView.hideSkeleton()

                if (songsList.isEmpty()) binding.recentlyAddedNothingToShow.visibility = View.VISIBLE
                else {
                    binding.recentlyAddedNothingToShow.visibility = View.GONE
                    recentlyAdapter.updateList(songsList)
                }
            }
        }
    }

    private fun setupAllSongsView() {
        binding.libraryAllSongsView.setOnClickListener {
            setFragmentResult("requestKey", bundleOf("playlistId" to -1L))
            openSongsFragment()
        }
    }

    private fun openSongsFragment() {
        val transaction = activity?.supportFragmentManager?.beginTransaction() ?: return
        transaction.replace(R.id.nav_host_fragment_content, SongsFragment())
        transaction.commit()
    }

    private fun setupCreatePlaylistButton() {
        binding.playlistCreateButton.setOnClickListener {
            val dialog = Dialog(requireContext()).also {
                it.requestWindowFeature(Window.FEATURE_NO_TITLE)
                it.setCancelable(true)
                it.setContentView(R.layout.library_playlist_create_dialog)
            }

            val playlistEditText: TextInputEditText = dialog.findViewById(R.id.playlistCreateDialogPlaylistEditText)
            val createButton: Button = dialog.findViewById(R.id.playlistCreateDialogCreateButton)
            val cancelButton: Button = dialog.findViewById(R.id.playlistCreateDialogCancelButton)

            createButton.setOnClickListener {
                val playlistName = playlistEditText.text
                if (playlistName.isNullOrBlank()) return@setOnClickListener

                libraryViewModel.createPlaylist(playlistName.toString())
                dialog.dismiss()
            }
            cancelButton.setOnClickListener { dialog.cancel() }

            dialog.show()
        }
    }

}