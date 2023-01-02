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
import phss.feelsapp.download.listeners.DownloadUpdateListener
import phss.feelsapp.service.DownloaderService
import phss.feelsapp.ui.download.DownloadAdapterItemInteractListener
import phss.feelsapp.ui.download.adapter.DownloadSongItemAdapter
import phss.feelsapp.ui.download.viewmodel.DownloadViewModel

class SearchFragment : Fragment() {

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
        setupObservableDownloadUpdateListener()
    }

    override fun onPause() {
        super.onPause()
        downloaderService.unregisterDownloadUpdateListener(this::class)
    }

    private fun setupSearchResultView() {
        if (downloadAdapter == null) downloadAdapter = DownloadSongItemAdapter(listOf(), setupSearchResultViewClickListener())

        binding.searchResultRecyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        binding.searchResultRecyclerView.adapter = downloadAdapter!!
    }

    private fun setupObservableDownloadUpdateListener() {
        downloaderService.registerDownloadUpdateListener(this::class, object : DownloadUpdateListener {
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
        })
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
        // some genres are starting with the "genre" word because of search problems
        binding.genre1.setOnClickListener { makeSearch("genre pop") }
        binding.genre2.setOnClickListener { makeSearch("genre hip hop") }
        binding.genre3.setOnClickListener { makeSearch("rock") }
        binding.genre4.setOnClickListener { makeSearch("blues") }
        binding.genre5.setOnClickListener { makeSearch("genre eletronic") }
        binding.genre6.setOnClickListener { makeSearch("reggae") }
        binding.genre7.setOnClickListener { makeSearch("country") }
        binding.genre8.setOnClickListener { makeSearch("funk") }
        binding.genre9.setOnClickListener { makeSearch("gospel") }
        binding.genre10.setOnClickListener { makeSearch("jazz") }
        binding.genre11.setOnClickListener { makeSearch("genre disco") }
        binding.genre12.setOnClickListener { makeSearch("classical") }
        binding.genre13.setOnClickListener { makeSearch("genre sertanejo") }
        binding.genre14.setOnClickListener { makeSearch("samba") }
        binding.genre15.setOnClickListener { makeSearch("pagode") }
        binding.genre16.setOnClickListener { makeSearch("kpop") }
        binding.genre17.setOnClickListener { makeSearch("j-pop") }
        binding.genre18.setOnClickListener { makeSearch("c-pop") }
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

    private fun makeSearch(query: String) {
        binding.searchTableLayout.visibility = View.INVISIBLE
        binding.searchResultRecyclerView.visibility = View.VISIBLE
        binding.searchResultRecyclerView.loadSkeleton(R.layout.song_item_download_view) {
            itemCount(10)
        }

        searchViewModel.getSongs(query) { songsList ->
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