package phss.feelsapp.ui.search

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import koleton.api.hideSkeleton
import koleton.api.loadSkeleton
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import phss.feelsapp.R
import phss.feelsapp.data.models.RemoteSong
import phss.feelsapp.databinding.FragmentSearchBinding
import phss.feelsapp.download.observers.DownloadUpdateObserver
import phss.feelsapp.service.DownloaderService
import phss.feelsapp.ui.download.DownloadAdapterItemInteractListener
import phss.feelsapp.ui.download.adapter.DownloadSongItemAdapter
import phss.feelsapp.ui.download.viewmodel.DownloadViewModel

class SearchFragment : Fragment(), DownloadUpdateObserver {

    private val searchViewModel: SearchViewModel by inject()
    private val downloadViewModel: DownloadViewModel by inject()
    private val downloaderService: DownloaderService by inject()

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private var downloadAdapter: DownloadSongItemAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)

        setupSearchEditText()
        setupSearchResultView()
        setupSearchRows()

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        downloaderService.registerDownloadUpdateObserver(this::class, this)
    }

    override fun onPause() {
        super.onPause()
        downloaderService.unregisterDownloadUpdateObserver(this::class)
    }

    override fun onDownloadStart(song: RemoteSong) {
        requireActivity().runOnUiThread { downloadAdapter?.updateDownloading(song, song.downloading) }
    }

    override fun onDownloadFinish(song: RemoteSong, success: Boolean) {
        lifecycleScope.launch {
            delay(1000L)
            requireActivity().runOnUiThread { downloadAdapter?.updateDownloading(song, song.downloading) }
        }
    }

    override fun onDownloadProgressUpdate(song: RemoteSong, progress: Float) {
        requireActivity().runOnUiThread { downloadAdapter?.updateDownloadProgress(song, progress) }
    }

    private fun setupSearchResultView() {
        if (downloadAdapter == null) downloadAdapter = DownloadSongItemAdapter(arrayListOf(), setupSearchResultViewClickListener())

        binding.searchResultRecyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        binding.searchResultRecyclerView.adapter = downloadAdapter!!
    }

    private fun setupSearchResultViewClickListener(): DownloadAdapterItemInteractListener {
        return object : DownloadAdapterItemInteractListener {
            override fun onSongViewClick(song: RemoteSong) {
                song.playing = !song.playing
            }

            override fun onDownloadButtonClick(song: RemoteSong) {
                if (!song.downloading) downloadViewModel.downloadSong(song)
                else downloadViewModel.cancelSongDownload(song)
            }

            override fun onDeleteButtonClick(song: RemoteSong) {
                downloadViewModel.deleteSong(song)
                downloadAdapter?.updateSong(song)
            }
        }
    }

    private fun setupSearchRows() {
        binding.genre1.setOnClickListener { makeSearch("pop", true) }
        binding.genre2.setOnClickListener { makeSearch("hip hop", true) }
        binding.genre3.setOnClickListener { makeSearch("rock", true) }
        binding.genre4.setOnClickListener { makeSearch("blues", true) }
        binding.genre5.setOnClickListener { makeSearch("eletronic", true) }
        binding.genre6.setOnClickListener { makeSearch("reggae", true) }
        binding.genre7.setOnClickListener { makeSearch("country", true) }
        binding.genre8.setOnClickListener { makeSearch("funk", true) }
        binding.genre9.setOnClickListener { makeSearch("gospel", true) }
        binding.genre10.setOnClickListener { makeSearch("jazz", true) }
        binding.genre11.setOnClickListener { makeSearch("disco", true) }
        binding.genre12.setOnClickListener { makeSearch("classical", true) }
        binding.genre13.setOnClickListener { makeSearch("sertanejo", true) }
        binding.genre14.setOnClickListener { makeSearch("samba", true) }
        binding.genre15.setOnClickListener { makeSearch("pagode", true) }
        binding.genre16.setOnClickListener { makeSearch("kpop", true) }
        binding.genre17.setOnClickListener { makeSearch("j-pop", true) }
        binding.genre18.setOnClickListener { makeSearch("c-pop", true) }
    }

    private fun setupSearchEditText() {
        binding.searchSongEditText.setOnEditorActionListener { textView, actionId, _ ->
            var handled = false
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                handled = true

                binding.searchSongEditText.clearFocus()
                (requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(binding.root.windowToken, 0)

                makeSearch(textView.text.toString())
            }
            handled
        }
        binding.searchSongEditText.doOnTextChanged { text, _, _, _ ->
            if (text.isNullOrBlank()) {
                binding.searchResultRecyclerView.visibility = View.INVISIBLE
                binding.searchTableLayout.visibility = View.VISIBLE
            }
        }
    }

    private fun makeSearch(query: String, searchForPlaylist: Boolean = false) {
        binding.searchTableLayout.visibility = View.INVISIBLE
        binding.searchResultRecyclerView.visibility = View.VISIBLE
        binding.searchResultRecyclerView.loadSkeleton(R.layout.song_item_download_view) {
            itemCount(10)
        }

        searchViewModel.getSongs(query, searchForPlaylist) { songsList ->
            songsList.forEach {
                if (downloaderService.downloading.contains(it.item.key)) it.downloading = true
            }
            requireActivity().runOnUiThread {
                binding.searchResultRecyclerView.hideSkeleton()

                if (songsList.isEmpty()) {
                    binding.searchResultRecyclerView.visibility = View.INVISIBLE
                    binding.searchTableLayout.visibility = View.VISIBLE
                }
                else {
                    downloadAdapter?.updateList(songsList)
                    binding.searchResultRecyclerView.adapter = downloadAdapter
                }
            }
        }
    }

}