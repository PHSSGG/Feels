package phss.feelsapp.view

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.Menu
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import com.dmitrymalkovich.android.ProgressFloatingActionButton
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.squareup.picasso.Picasso
import phss.feelsapp.R
import phss.feelsapp.data.models.Song
import phss.feelsapp.databinding.ActivityMainBinding
import phss.feelsapp.player.PlayerManager
import phss.feelsapp.player.listeners.PlayerStateChangeListener
import phss.feelsapp.service.PlayerService
import phss.feelsapp.ui.home.HomeFragment
import phss.feelsapp.ui.library.LibraryFragment
import phss.feelsapp.ui.search.SearchFragment
import phss.feelsapp.utils.CircleTransform
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    private lateinit var playerService: PlayerService

    private val playerServiceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            playerService.playerManager.unregisterPlayerStateChangeListener(this@MainActivity::class)
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as PlayerService.LocalBinder
            playerService = binder.getService()

            playerService.playerManager.registerPlayerStateChangeListener(this@MainActivity::class, setupPlayerStateChangeListener())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

        BottomSheetBehavior.from(findViewById(R.id.playerBottomSheet)).apply {
            peekHeight = maxHeight
            state = BottomSheetBehavior.STATE_HIDDEN

            setupBottomSheetButtons()
            setupBottomSheetSeekBar()
        }

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

    private fun setupPlayerStateChangeListener(): PlayerStateChangeListener {
        var isBottomSheetOpened = false
        var isBottomSheetClosing = false

        val bottomSheetCallback = object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onSlide(bottomSheetView: View, slideOffset: Float) {
                if (!isBottomSheetOpened && !isBottomSheetClosing) {
                    if (slideOffset == 1f) {
                        isBottomSheetOpened = true
                        BottomSheetBehavior.from(bottomSheetView).isDraggable = true
                    }
                    return
                }
                if (slideOffset <= 0.1f) {
                    isBottomSheetOpened = false
                    isBottomSheetClosing = true
                    BottomSheetBehavior.from(bottomSheetView).isDraggable = false
                    BottomSheetBehavior.from(bottomSheetView).state = BottomSheetBehavior.STATE_HIDDEN
                }
            }
            override fun onStateChanged(bottomSheet: View, newState: Int) {
            }
        }

        return object : PlayerStateChangeListener {
            override fun onPlaying(song: Song, duration: Int) {
                findViewById<ImageView>(R.id.playingSongFabPaused).visibility = View.GONE
                findViewById<ProgressFloatingActionButton>(R.id.playingSongFabHolder).visibility = View.VISIBLE

                Picasso.get().load(File(song.thumbnailPath)).transform(CircleTransform()).into(findViewById<FloatingActionButton>(R.id.playingSongFab))
                findViewById<ProgressBar>(R.id.playingSongFabProgress).apply {
                    progress = 0
                }

                val bottomSheet = BottomSheetBehavior.from(findViewById(R.id.playerBottomSheet))
                bottomSheet.addBottomSheetCallback(bottomSheetCallback)

                findViewById<ImageButton>(R.id.playerPauseResumeButton).setImageDrawable(AppCompatResources.getDrawable(baseContext, R.drawable.ic_pause))
                findViewById<ProgressFloatingActionButton>(R.id.playingSongFabHolder).setOnClickListener {
                    isBottomSheetClosing = false
                    bottomSheet.state = BottomSheetBehavior.STATE_EXPANDED
                }

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
                    progress = 0
                    max = playerService.playerManager.getSongDuration()
                }
                findViewById<TextView>(R.id.playerSeekBarSongDurationPosition).text = playerService.playerManager.getProgressString()
                findViewById<TextView>(R.id.playerSeekBarSongDuration).text = song.duration
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
                findViewById<ProgressFloatingActionButton>(R.id.playingSongFabHolder).visibility = View.GONE
                BottomSheetBehavior.from(findViewById(R.id.playerBottomSheet)).apply {
                    isBottomSheetClosing = true
                    state = BottomSheetBehavior.STATE_HIDDEN
                    removeBottomSheetCallback(bottomSheetCallback)
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

}