package phss.feelsapp

import android.app.Application
import androidx.room.Room
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.dsl.module
import phss.feelsapp.data.repository.SongsRepository
import phss.feelsapp.data.source.remote.SongsRemoteDataSource
import phss.feelsapp.database.AppDatabase
import phss.feelsapp.ui.home.HomeViewModel
import phss.ytmusicwrapper.YTMusicAPIWrapper

val databaseModule = module {
    single { Room.databaseBuilder(
        androidApplication(),
        AppDatabase::class.java,
        "FeelsDatabase.db"
    ).build() }

    single {
        val database = get<AppDatabase>()
        database.songDao()
    }
    single {
        val database = get<AppDatabase>()
        database.playlistDao()
    }
}

val appModule = module {
    single { SongsRepository(
        get(),
        SongsRemoteDataSource(YTMusicAPIWrapper(""))
    ) }

    viewModel { HomeViewModel(get()) }
}

class FeelsApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger()
            androidContext(this@FeelsApplication)
            modules(databaseModule, appModule)
        }
    }

}