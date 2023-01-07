package phss.feelsapp.ui.songs

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ImageButton
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import koleton.api.hideSkeleton
import koleton.api.loadSkeleton
import org.koin.android.ext.android.inject
import phss.feelsapp.R
import phss.feelsapp.data.models.Playlist
import phss.feelsapp.data.models.Song
import phss.feelsapp.databinding.FragmentSongsBinding
import phss.feelsapp.player.observers.PlayerObserver
import phss.feelsapp.service.PlayerService
import phss.feelsapp.ui.songs.adapters.songs.SongsAdapter
import phss.feelsapp.ui.songs.adapters.songs.SongsAdapterItemInteractListener
import phss.feelsapp.ui.songs.dialogs.PlaylistAddSongsDialog
import phss.feelsapp.ui.songs.dialogs.SongOptionsDialog

class SongsFragment : Fragment() {

    private val songsViewModel: SongsViewModel by inject()

    private var _binding: FragmentSongsBinding? = null
    private val binding get() = _binding!!

    private lateinit var playerService: PlayerService

    var currentListOfSongs: List<Song> = ArrayList()
        set(value) {
            if (::playerService.isInitialized) {
                val currentPlaying = playerService.playerManager.getCurrentPlaying()
                val playingFromPlaylist = playerService.playerManager.playingFromPlaylist

                if (playerService.playerManager.shuffle && playerService.playerManager.isSamePlaylist(playlist)) {
                    field = playerService.playerManager.getCurrentSongsList()

                    value.forEach { updatedSong ->
                        val fieldSong = field.find { it.key == updatedSong.key }
                        if (fieldSong != null) with(fieldSong) {
                            timesPlayed = updatedSong.timesPlayed
                            lastPlayed = updatedSong.lastPlayed
                            isPlaying = currentPlaying?.key == updatedSong.key && playingFromPlaylist?.playlistId == playlist?.playlistId
                        } else {
                            val newList = ArrayList(field)
                            newList.add(updatedSong)

                            field = newList
                        }
                    }

                    updateSongsAmount(field.size)
                    songsAdapter?.updateList(field)
                    return
                }

                value.forEach {
                    it.isPlaying = currentPlaying?.key == it.key && playingFromPlaylist?.playlistId == playlist?.playlistId
                }
            } else value.forEach { it.isPlaying = false }

            field = value
            updateSongsAmount(value.size)
            songsAdapter?.updateList(field)
        }
    var playlist: Playlist? = null
    var songsAdapter: SongsAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSongsBinding.inflate(inflater, container, false)

        binding.songsInfo.text = getString(R.string.songs_info).replace("{amount}", "0")

        songsAdapter = SongsAdapter(listOf(), setupSongItemInteractListener())
        setupSongsView()

        setFragmentResultListener("requestKey") { _, bundle ->
            val playlistId = bundle.getLong("playlistId", -1L)
            if (playlistId != -1L) songsViewModel.getPlaylistById(playlistId) {
                playlist = it
                requireActivity().runOnUiThread {
                    binding.songsLibraryText.text = playlist!!.playlistName

                    setupAddSongsButton()
                    updateSongsViewWithPlaylist()
                }
            }
            else {
                binding.songsAddButton.visibility = View.INVISIBLE
                updateSongsView()
            }

            bindPlayerService()
            setupPlayButton()
            setupShuffleButton()
            setupSearchEditText()
        }

        return binding.root
    }

    override fun onPause() {
        super.onPause()
        playerService.playerManager.unregisterPlayerObserver(this::class)
    }

    override fun onResume() {
        super.onResume()
        // update playing info
        currentListOfSongs = currentListOfSongs
        setupPlayerObserver()
    }

    private val playerServiceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            playerService.playerManager.unregisterPlayerObserver(this::class)
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as PlayerService.LocalBinder
            playerService = binder.getService()

            val currentPlaying = playerService.playerManager.getCurrentPlaying()
            if (currentPlaying != null) songsAdapter?.updateItems(currentPlaying)

            setupPlayerObserver()
        }
    }
    private fun bindPlayerService() {
        val playerServiceIntent = Intent(requireContext(), PlayerService::class.java)
        requireActivity().bindService(playerServiceIntent, playerServiceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun setupPlayerObserver() {
        if (!::playerService.isInitialized) return

        updateShuffleButton(binding.songsShuffleButton)
        playerService.playerManager.registerPlayerObserver(this::class, object : PlayerObserver {
            override fun onPlay(song: Song, previous: Song?) {
                requireActivity().runOnUiThread { songsAdapter?.updateItems(song, previous) }
            }
            override fun onStop(song: Song?) {
                if (song == null) return
                requireActivity().runOnUiThread { songsAdapter?.updateItems(song) }
            }

            override fun onShuffleStateChange(newSongsList: List<Song>) {
                if (!playerService.playerManager.isSamePlaylist(playlist)) return

                currentListOfSongs = newSongsList
                requireActivity().runOnUiThread {
                    updateShuffleButton(binding.songsShuffleButton)
                }
            }
        })
    }

    private fun playCurrentSongs() {
        val song = currentListOfSongs.getOrNull(0) ?: return
        val previousSong = playerService.playerManager.getCurrentPlaying()

        playerService.playerManager.setupPlayer(currentListOfSongs, song, playlist)
        songsAdapter?.updateItems(song, previousSong)
    }

    private fun setupPlayButton() = binding.songsPlayButton.setOnClickListener {
        playCurrentSongs()
    }

    private fun setupShuffleButton() = binding.songsShuffleButton.setOnClickListener {
        if (currentListOfSongs.isEmpty()) return@setOnClickListener

        if (!playerService.playerManager.isSamePlaylist(playlist) || (!playerService.playerManager.isPlaying() && !playerService.playerManager.shuffle)) {
            playerService.playerManager.shuffleAndPlay = true
            playCurrentSongs()
        } else playerService.playerManager.shuffle = !playerService.playerManager.shuffle

        updateShuffleButton(it as ImageButton)
    }

    private fun updateShuffleButton(shuffleButton: ImageButton) {
        if (playerService.playerManager.shuffle && playerService.playerManager.isSamePlaylist(playlist)) shuffleButton.setColorFilter(ContextCompat.getColor(requireContext(), R.color.green))
        else shuffleButton.setColorFilter(ContextCompat.getColor(requireContext(), R.color.gray))
    }

    private fun setupSearchEditText() {
        binding.songsLibrarySearch.setOnEditorActionListener { textView, actionId, _ ->
            var handled = false
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                handled = true

                binding.songsLibrarySearch.clearFocus()
                (requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(binding.root.windowToken, 0)

                songsAdapter?.filter?.filter(textView.text.toString())
            }
            handled
        }
        binding.songsLibrarySearch.doOnTextChanged { text, _, _, _ ->
            if (text.isNullOrBlank()) songsAdapter?.filter?.filter(null)
            else songsAdapter?.filter?.filter(text.toString())
        }
    }

    private fun updateSongsAmount(amount: Int) {
        binding.songsInfo.text = getString(R.string.songs_info).replace("{amount}", "$amount")
    }

    private fun setupSongsView() {
        binding.songsRecyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        binding.songsRecyclerView.adapter = songsAdapter

        binding.songsRecyclerView.loadSkeleton(R.layout.library_songs_view) {
            itemCount(10)
        }
    }

    private fun setupSongItemInteractListener(): SongsAdapterItemInteractListener {
        return object : SongsAdapterItemInteractListener {
            override fun onClick(song: Song) {
                val previousSong = playerService.playerManager.getCurrentPlaying()
                if (playerService.playerManager.isSamePlaylist(playlist) && previousSong != null && previousSong.key == song.key && song.isPlaying) {
                    if (playerService.playerManager.isPlaying()) playerService.playerManager.pausePlayer()
                    else playerService.playerManager.resumePlayer()
                    return
                }

                playerService.playerManager.setupPlayer(currentListOfSongs, song, playlist)
                songsAdapter?.updateItems(song, previousSong)
            }
            override fun onLongClick(song: Song) {
                SongOptionsDialog(
                    requireContext(),
                    lifecycleScope,
                    songsViewModel,
                    song, playlist
                ).openDialog()
            }
        }
    }

    private fun setupAddSongsButton() {
        binding.songsAddButton.setOnClickListener {
            PlaylistAddSongsDialog(
                requireContext(),
                lifecycleScope,
                songsViewModel,
                playlist, currentListOfSongs
            ).openDialog()
        }
    }

    private fun updateSongsView() {
        lifecycle.coroutineScope.launchWhenStarted {
            songsViewModel.loadAllSongs().collect {
                currentListOfSongs = it
                binding.songsRecyclerView.hideSkeleton()
            }
        }
    }

    private fun updateSongsViewWithPlaylist() {
        if (playlist == null) return

        lifecycle.coroutineScope.launchWhenStarted {
            songsViewModel.loadPlaylistWithSongs(playlist!!).collect {
                binding.songsRecyclerView.hideSkeleton()

                if (it != null && it.songs != null) {
                    currentListOfSongs = it.songs
                }
            }
        }
    }

}