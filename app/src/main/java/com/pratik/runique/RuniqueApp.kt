package com.pratik.runique

import android.app.Application
import com.pratik.auth.di.authDataModule
import com.pratik.auth.presentation.di.authViewModelModule
import com.pratik.core.data.di.coreDataModule
import com.pratik.core.database.di.databaseModule
import com.pratik.run.data.di.runDataModule
import com.pratik.run.location.di.locationModule
import com.pratik.run.network.di.networkModule
import com.pratik.run.presentation.di.runPresentationModule
import com.pratik.runique.di.appModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.workmanager.koin.workManagerFactory
import org.koin.core.context.startKoin
import timber.log.Timber

class RunningTrackerApp: Application() {

    val applicationScope = CoroutineScope(SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        if(BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        startKoin {
            androidLogger()
            androidContext(this@RunningTrackerApp)
            workManagerFactory()
            modules(
                authDataModule,
                authViewModelModule,
                appModule,
                coreDataModule,
                runPresentationModule,
                locationModule,
                databaseModule,
                networkModule,
                runDataModule
            )
        }
    }
}