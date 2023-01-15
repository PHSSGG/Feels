package phss.feelsapp.ui.library

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import koleton.api.hideSkeleton
import koleton.api.loadSkeleton
import org.koin.android.ext.android.inject
import phss.feelsapp.R
import phss.feelsapp.data.models.Playlist
import phss.feelsapp.data.models.Song
import phss.feelsapp.databinding.FragmentLibraryBinding
import phss.feelsapp.ui.library.adapters.playlists.PlaylistAdapterItemInteractListener
import phss.feelsapp.ui.library.adapters.playlists.PlaylistsAdapter
import phss.feelsapp.ui.library.dialogs.CreatePlaylistDialog
import phss.feelsapp.ui.library.dialogs.DeletePlaylistDialog
import phss.feelsapp.ui.recently.RecentlyAdapter
import phss.feelsapp.ui.recently.RecentlyPlayer
import phss.feelsapp.ui.songs.SongsFragment
import phss.feelsapp.ui.songs.adapters.songs.SongsAdapterItemInteractListener

class LibraryFragment : Fragment() {

    private val libraryViewModel: LibraryViewModel by inject()
    private lateinit var recentlyPlayer: RecentlyPlayer

    private var _binding: FragmentLibraryBinding? = null
    private val binding get() = _binding!!

    private var recentlyAdapter: RecentlyAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLibraryBinding.inflate(inflater, container, false)

        recentlyPlayer = RecentlyPlayer(requireContext(), requireActivity(), this)
        recentlyPlayer.bindPlayerService()

        setupPlaylistsView()
        setupRecentlyAddedView()
        setupCreatePlaylistButton()
        setupAllSongsView()

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        recentlyPlayer.onResume()
    }

    override fun onPause() {
        super.onPause()
        recentlyPlayer.onPause()
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
                DeletePlaylistDialog(
                    requireContext(),
                    lifecycleScope,
                    libraryViewModel,
                    playlist
                ).openDialog()
            }
        }
    }

    private fun setupRecentlyAddedView() {
        binding.libraryRecentlyRecyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        recentlyAdapter = RecentlyAdapter(listOf(), recentlyPlayer.setupSongItemInteractListener())
        recentlyPlayer.recentlyAdapter = recentlyAdapter

        updateRecentlyAddedView()
    }

    private fun updateRecentlyAddedView() {
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
                    recentlyPlayer.currentListOfSongs = songsList
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
            CreatePlaylistDialog(requireContext(), libraryViewModel).openDialog()
        }
    }

}