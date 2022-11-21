package phss.feelsapp.view

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.Menu
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import com.google.android.material.bottomnavigation.BottomNavigationView
import phss.feelsapp.R
import phss.feelsapp.databinding.ActivityMainBinding
import phss.feelsapp.service.PlayerService
import phss.feelsapp.ui.home.HomeFragment
import phss.feelsapp.ui.library.LibraryFragment
import phss.feelsapp.ui.search.SearchFragment

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    private lateinit var playerService: PlayerService

    private val playerServiceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            // TODO: Unbind service stuff
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            // TODO: Bind service stuff

            val binder = service as PlayerService.LocalBinder
            playerService = binder.getService()
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