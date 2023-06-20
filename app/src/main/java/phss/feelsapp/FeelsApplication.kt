package phss.feelsapp

import android.app.Application
import androidx.room.Room
import com.yausername.youtubedl_android.YoutubeDL
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.dsl.module
import phss.feelsapp.data.repository.PlaylistsRepository
import phss.feelsapp.data.repository.SongsRepository
import phss.feelsapp.data.repository.UserRepository
import phss.feelsapp.data.source.local.SongsLocalDataSource
import phss.feelsapp.data.source.remote.SongsRemoteDataSource
import phss.feelsapp.database.AppDatabase
import phss.feelsapp.database.DatabaseMigrations
import phss.feelsapp.player.PlayerManager
import phss.feelsapp.service.DownloaderService
import phss.feelsapp.ui.download.viewmodel.DownloadViewModel
import phss.feelsapp.ui.home.HomeViewModel
import phss.feelsapp.ui.home.interests.SelectInterestsViewModel
import phss.feelsapp.ui.library.LibraryViewModel
import phss.feelsapp.ui.search.SearchViewModel
import phss.feelsapp.ui.songs.SongsViewModel
import phss.ytmusicwrapper.YTMusicAPIWrapper

val databaseModule = module {
    single { Room.databaseBuilder(
        androidApplication(),
        AppDatabase::class.java,
        "FeelsDatabase.db"
    ).addMigrations(DatabaseMigrations.MIGRATION_1_2).build() }

    single {
        val database = get<AppDatabase>()
        database.songDao()
    }
    single {
        val database = get<AppDatabase>()
        database.playlistDao()
    }
    single {
        val database = get<AppDatabase>()
        database.userDao()
    }
}

val appModule = module {
    single { SongsRepository(
        SongsLocalDataSource(get()),
        SongsRemoteDataSource(YTMusicAPIWrapper(""))
    ) }
    single { PlaylistsRepository(get()) }
    single { UserRepository(get()) }
    single { DownloaderService(get(), get()) }
    single { PlayerManager() }

    viewModel { HomeViewModel(get(), get()) }
    viewModel { SelectInterestsViewModel(get()) }
    viewModel { SearchViewModel(get()) }
    viewModel { DownloadViewModel(get(), get()) }
    viewModel { LibraryViewModel(get(), get()) }
    viewModel { SongsViewModel(get(), get()) }
}

class FeelsApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger()
            androidContext(this@FeelsApplication)
            modules(databaseModule, appModule)
        }

        YoutubeDL.getInstance().init(applicationContext)

        Thread {
            try {
                YoutubeDL.getInstance().updateYoutubeDL(this)
            } catch (ignored: Exception) {}
        }.start()
    }

}