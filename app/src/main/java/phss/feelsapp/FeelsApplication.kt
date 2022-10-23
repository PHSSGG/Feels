package phss.feelsapp

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.dsl.module
import phss.feelsapp.data.repository.SongsRepository
import phss.feelsapp.data.source.local.SongsLocalDataSource
import phss.feelsapp.data.source.remote.SongsRemoteDataSource
import phss.feelsapp.ui.home.HomeViewModel
import phss.ytmusicwrapper.YTMusicAPIWrapper

val appModule = module {
    single { SongsRepository(
        SongsLocalDataSource(),
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
            modules(appModule)
        }
    }

}