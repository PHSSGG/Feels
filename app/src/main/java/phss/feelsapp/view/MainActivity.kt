package phss.feelsapp.view

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RoundRectShape
import android.os.Bundle
import android.os.IBinder
import android.view.Menu
import android.view.View
import android.widget.*
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import com.dmitrymalkovich.android.ProgressFloatingActionButton
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.jagrosh.jlyrics.LyricsClient
import com.squareup.picasso.Picasso
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import phss.feelsapp.R
import phss.feelsapp.data.models.Song
import phss.feelsapp.data.repository.SongsRepository
import phss.feelsapp.databinding.ActivityMainBinding
import phss.feelsapp.player.PlayerManager
import phss.feelsapp.player.observers.PlayerStateChangeObserver
import phss.feelsapp.service.DownloaderService
import phss.feelsapp.service.PlayerService
import phss.feelsapp.ui.download.drawer.DownloadsDrawerFragment
import phss.feelsapp.ui.home.HomeFragment
import phss.feelsapp.ui.library.LibraryFragment
import phss.feelsapp.ui.search.SearchFragment
import phss.feelsapp.utils.CircleTransform
import phss.feelsapp.utils.TopSheetBehavior
import phss.musixmatchwrapper.MusixMatch
import java.io.File


class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    private lateinit var playerService: PlayerService

    private val songsRepository: SongsRepository by inject()
    private val downloaderService: DownloaderService by inject()

    private val playerServiceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            playerService.playerManager.unregisterPlayerStateChangeObserver(this@MainActivity::class)
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as PlayerService.LocalBinder
            playerService = binder.getService()

            playerService.playerManager.registerPlayerStateChangeObserver(this@MainActivity::class, setupPlayerStateChangeListener())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        GlobalScope.launch {
            val bugged = ArrayList<Song>()
            songsRepository.loadAllSongsWithoutFlow().filter { song ->
                song.artist.equals(
                    "null",
                    true
                ) || song.artist.contains("info(") || song.artist.contains("Info(")
            }.forEach { song ->
                bugged.add(song)
            }

            delay(10000)
            bugged.forEach {
                songsRepository.searchForSongsRemote("${it.key}", false).firstOrNull { remoteSong ->
                    remoteSong.item.key.equals(it.key)
                }?.run {
                    if (it.name == this.item.info!!.name) {
                        songsRepository.deleteSong(it)
                        downloaderService.downloadSong(this)
                    }
                }
            }
        }

        val playerServiceIntent = Intent(this, PlayerService::class.java)
        bindService(playerServiceIntent, playerServiceConnection, Context.BIND_AUTO_CREATE)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val drawerLayout: DrawerLayout = binding.drawerLayout
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home, R.id.nav_search, R.id.nav_library, R.id.nav_songs
            ), drawerLayout
        )

        TopSheetBehavior.from(findViewById(R.id.playerBottomSheet)).apply {
            //peekHeight = maxHeight
            state = TopSheetBehavior.STATE_HIDDEN

            setupBottomSheetButtons()
            setupBottomSheetSeekBar()
        }

        setupDownloadsDrawer()
        setupPlayerFloatingActionButton()
        setupBottomNavigationButtons()
    }

    private fun setupDownloadsDrawer() {
        val downloadsDrawer = binding.downloadsDrawerLayout
        val actionBarDrawerToggle = ActionBarDrawerToggle(this, downloadsDrawer, null, R.string.downloading_title, R.string.downloading_title)
        downloadsDrawer.addDrawerListener(actionBarDrawerToggle)
        actionBarDrawerToggle.syncState()

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.downloadsDrawerFrameLayout, DownloadsDrawerFragment(), "Downloads").commit();

        findViewById<ConstraintLayout>(R.id.downloadingIndicator).visibility = View.GONE
    }

    private fun setupPlayerFloatingActionButton() {
        findViewById<ProgressFloatingActionButton>(R.id.playingSongFabHolder).setOnLongClickListener { view ->
            changePosition(view, 500)
            true
        }
    }

    private fun changePosition(view: View, transitionDuration: Long, isUpdateView: Boolean = false) {
        val layoutParams = view.layoutParams as ConstraintLayout.LayoutParams

        if (layoutParams.startToStart == ConstraintLayout.LayoutParams.UNSET) {
            if (isUpdateView) return

            layoutParams.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            layoutParams.endToEnd = ConstraintLayout.LayoutParams.UNSET
        } else {
            layoutParams.startToStart = ConstraintLayout.LayoutParams.UNSET
            layoutParams.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
        }

        val transition = AutoTransition()
        transition.duration = transitionDuration
        TransitionManager.beginDelayedTransition(view.parent as ConstraintLayout, transition)

        view.layoutParams = layoutParams
        view.requestLayout()
    }

    private fun setupPlayerStateChangeListener(): PlayerStateChangeObserver {
        var isBottomSheetOpened = false
        var isBottomSheetClosing = false

        val bottomSheetCallback = object : TopSheetBehavior.TopSheetCallback() {
            override fun onSlide(bottomSheetView: View, slideOffset: Float) {
                if (!isBottomSheetOpened && !isBottomSheetClosing) {
                    if (slideOffset == 1f) {
                        isBottomSheetOpened = true
                        //BottomSheetBehavior.from(bottomSheetView).isDraggable = true
                    }
                    return
                }
                if (slideOffset <= 0.1f) {
                    isBottomSheetOpened = false
                    isBottomSheetClosing = true
                    //BottomSheetBehavior.from(bottomSheetView).isDraggable = false
                    TopSheetBehavior.from(bottomSheetView).state = TopSheetBehavior.STATE_HIDDEN
                }
            }
            override fun onStateChanged(bottomSheet: View, newState: Int) {
            }
        }

        return object : PlayerStateChangeObserver {
            override fun onPlaying(song: Song, duration: Int, progress: Int) {
                runOnUiThread {
                    findViewById<ImageView>(R.id.playingSongFabPaused).run {
                        visibility = if (!playerService.playerManager.isPlaying()) View.VISIBLE
                        else View.GONE
                    }
                    findViewById<ProgressBar>(R.id.playingSongFabProgress).progress = if (progress < 0) 0 else progress
                    findViewById<ProgressFloatingActionButton>(R.id.playingSongFabHolder).visibility = View.VISIBLE

                    lifecycleScope.launch {
                        delay(500)
                        changePosition(findViewById<ProgressFloatingActionButton>(R.id.playingSongFabHolder), 200, true)
                    }

                    Picasso.get().load(File(song.thumbnailPath)).transform(CircleTransform()).into(findViewById<FloatingActionButton>(R.id.playingSongFab))

                    val bottomSheet = TopSheetBehavior.from(findViewById(R.id.playerBottomSheet))
                    bottomSheet.setTopSheetCallback(bottomSheetCallback)

                    findViewById<ImageButton>(R.id.playerPauseResumeButton).setImageDrawable(if (playerService.playerManager.isPlaying()) AppCompatResources.getDrawable(baseContext, R.drawable.ic_pause) else AppCompatResources.getDrawable(baseContext, R.drawable.ic_play))
                    findViewById<ProgressFloatingActionButton>(R.id.playingSongFabHolder).setOnClickListener {
                        if (bottomSheet.state == TopSheetBehavior.STATE_EXPANDED) {
                            bottomSheet.state = TopSheetBehavior.STATE_HIDDEN
                        } else {
                            isBottomSheetClosing = false
                            bottomSheet.state = TopSheetBehavior.STATE_EXPANDED
                        }
                    }
                    updateShuffleButtonStyle()

                    setupBottomSheetImage(song, findViewById(R.id.playerCurrentPlayingThumb))
                    setupBottomSheetImage(playerService.playerManager.getNextSong(), findViewById(R.id.playerNextPlayingThumb))
                    setupBottomSheetImage(playerService.playerManager.getPreviousSong(), findViewById(R.id.playerPreviousPlayingThumb))

                    findViewById<TextView>(R.id.playerCurrentPlayingTitle).apply {
                        text = song.name

                        setHorizontallyScrolling(true)
                        isSelected = true
                    }
                    findViewById<TextView>(R.id.playerCurrentPlayingArtist).apply {
                        text = song.artist

                        setHorizontallyScrolling(true)
                        isSelected = true
                    }
                    findViewById<SeekBar>(R.id.playerSeekBar).apply {
                        this.progress = progress
                        max = playerService.playerManager.getSongDuration()
                    }
                    findViewById<TextView>(R.id.playerSeekBarSongDurationPosition).text = playerService.playerManager.getProgressString()
                    findViewById<TextView>(R.id.playerSeekBarSongDuration).text = song.duration
                }

                lifecycleScope.launch(Dispatchers.IO) {
                    var lyrics = "Lyrics not found"

                    val musixMatchLyrics = MusixMatch().searchLyrics(song.artist, song.name).get()
                    if (musixMatchLyrics != null) {
                        lyrics = musixMatchLyrics.content
                    } else {
                        val foundLyric = LyricsClient().getLyrics("${song.artist} ${song.name}").get()
                        if (foundLyric != null) {
                            lyrics = foundLyric.content
                        }
                    }

                    runOnUiThread {
                        findViewById<TextView>(R.id.lyricText).text = lyrics
                    }
                }
            }

            override fun onTimeChange(song: Song, timePercent: Int) {
                runOnUiThread {
                    findViewById<ProgressBar>(R.id.playingSongFabProgress)?.progress = timePercent
                    findViewById<SeekBar>(R.id.playerSeekBar).progress = playerService.playerManager.getProgress()
                    findViewById<TextView>(R.id.playerSeekBarSongDurationPosition).text = playerService.playerManager.getProgressString()
                }
            }

            override fun onResume(song: Song) {
                findViewById<ImageButton>(R.id.playerPauseResumeButton).setImageDrawable(AppCompatResources.getDrawable(baseContext, R.drawable.ic_pause))
                findViewById<ImageView>(R.id.playingSongFabPaused).visibility = View.GONE
            }

            override fun onPause(song: Song) {
                findViewById<ImageButton>(R.id.playerPauseResumeButton).setImageDrawable(AppCompatResources.getDrawable(baseContext, R.drawable.ic_play))
                findViewById<ImageView>(R.id.playingSongFabPaused).visibility = View.VISIBLE
            }

            override fun onStop() {
                runOnUiThread {
                    findViewById<ProgressFloatingActionButton>(R.id.playingSongFabHolder).visibility = View.GONE
                    findViewById<ImageView>(R.id.playingSongFabPaused).visibility = View.GONE

                    TopSheetBehavior.from(findViewById(R.id.playerBottomSheet)).apply {
                        isBottomSheetClosing = true
                        state = TopSheetBehavior.STATE_HIDDEN
                        setTopSheetCallback(null)
                        //removeBottomSheetCallback(bottomSheetCallback)
                    }
                }
            }
        }
    }

    private fun setupBottomSheetImage(song: Song?, imageView: ImageView) {
        if (song == null) imageView.visibility = View.GONE
        else {
            imageView.visibility = View.VISIBLE
            Picasso.get().load(File(song.thumbnailPath)).into(imageView)
        }
    }

    private fun setupBottomSheetButtons() {
        findViewById<ImageButton>(R.id.playerRepeatButton).setOnClickListener {
            playerService.playerManager.repeatType = when (playerService.playerManager.repeatType) {
                PlayerManager.RepeatType.NONE -> PlayerManager.RepeatType.REPEAT_LIST
                PlayerManager.RepeatType.REPEAT_LIST -> PlayerManager.RepeatType.REPEAT_SINGLE
                else -> PlayerManager.RepeatType.NONE
            }
            updateRepeatButtonStyle()
        }
        findViewById<ImageButton>(R.id.playerShuffleButton).setOnClickListener {
            playerService.playerManager.shuffle = !playerService.playerManager.shuffle
            updateShuffleButtonStyle()
        }
        findViewById<ImageButton>(R.id.playerPauseResumeButton).setOnClickListener {
            if (playerService.playerManager.isPlaying()) playerService.playerManager.pausePlayer()
            else playerService.playerManager.resumePlayer()
        }
        findViewById<ImageButton>(R.id.playerPreviousButton).setOnClickListener {
            playerService.playerManager.playPrevious()
        }
        findViewById<ImageButton>(R.id.playerNextButton).setOnClickListener {
            playerService.playerManager.playNext(true)
        }
        findViewById<ImageButton>(R.id.playerBackwardButton).setOnClickListener {
            playerService.playerManager.backward5()
        }
        findViewById<ImageButton>(R.id.playerForwardButton).setOnClickListener {
            playerService.playerManager.forward5()
        }
        findViewById<ImageButton>(R.id.closePlayerViewButton).setOnClickListener {
            handlePlayerViewCloseButtonClick()
        }
        findViewById<ConstraintLayout>(R.id.closePlayerViewButtonView).setOnClickListener {
            handlePlayerViewCloseButtonClick()
        }
        findViewById<ImageButton>(R.id.lyricsButton).setOnClickListener {
            findViewById<ConstraintLayout>(R.id.normalPlayerView).visibility = View.INVISIBLE
            findViewById<ConstraintLayout>(R.id.lyricsPlayerView).visibility = View.VISIBLE
        }
    }

    private fun handlePlayerViewCloseButtonClick() {
        val normalPlayerView = findViewById<ConstraintLayout>(R.id.normalPlayerView)
        if (normalPlayerView.visibility == View.INVISIBLE) {
            findViewById<ConstraintLayout>(R.id.lyricsPlayerView).visibility = View.GONE
            normalPlayerView.visibility = View.VISIBLE
        } else {
            TopSheetBehavior.from(findViewById(R.id.playerBottomSheet)).state = TopSheetBehavior.STATE_HIDDEN
        }
    }

    private fun setupBottomSheetSeekBar() {
        findViewById<SeekBar>(R.id.playerSeekBar).setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) playerService.playerManager.seekTo(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })
    }

    private fun updateRepeatButtonStyle() {
        val repeatButton: ImageButton = findViewById(R.id.playerRepeatButton)
        when (playerService.playerManager.repeatType) {
            PlayerManager.RepeatType.NONE -> {
                repeatButton.setImageDrawable(AppCompatResources.getDrawable(baseContext, R.drawable.ic_repeat_list))
                repeatButton.setColorFilter(ContextCompat.getColor(baseContext, R.color.gray))
            }
            PlayerManager.RepeatType.REPEAT_LIST -> {
                repeatButton.setImageDrawable(AppCompatResources.getDrawable(baseContext, R.drawable.ic_repeat_list))
                repeatButton.setColorFilter(ContextCompat.getColor(baseContext, R.color.green))
            }
            PlayerManager.RepeatType.REPEAT_SINGLE -> {
                repeatButton.setImageDrawable(AppCompatResources.getDrawable(baseContext, R.drawable.ic_repeat_single))
                repeatButton.setColorFilter(ContextCompat.getColor(baseContext, R.color.green))
            }
        }
    }

    private fun updateShuffleButtonStyle() {
        val shuffleButton: ImageButton = findViewById(R.id.playerShuffleButton)
        if (playerService.playerManager.shuffle) shuffleButton.setColorFilter(ContextCompat.getColor(baseContext, R.color.green))
        else shuffleButton.setColorFilter(ContextCompat.getColor(baseContext, R.color.gray))
    }

    private fun setupBottomNavigationButtons() {
        findViewById<BottomNavigationView>(R.id.bottom_navigation).setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.homeButton -> {
                    val transaction = supportFragmentManager.beginTransaction()
                    transaction.replace(R.id.nav_host_fragment_content, HomeFragment())
                    transaction.commit()
                    true
                }
                R.id.searchButton -> {
                    val transaction = supportFragmentManager.beginTransaction()
                    transaction.replace(R.id.nav_host_fragment_content, SearchFragment())
                    transaction.commit()
                    true
                }
                R.id.libraryButton -> {
                    val transaction = supportFragmentManager.beginTransaction()
                    transaction.replace(R.id.nav_host_fragment_content, LibraryFragment())
                    transaction.commit()
                    true
                }
                else -> false
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

}