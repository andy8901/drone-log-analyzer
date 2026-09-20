package com.neosky.servicesupport.core.di

import android.content.Context
import androidx.room.Room
import com.neosky.servicesupport.data.local.AppDatabase
import com.neosky.servicesupport.data.local.dao.DroneDao
import com.neosky.servicesupport.data.local.dao.FlightLogDao
import com.neosky.servicesupport.data.local.dao.NotificationDao
import com.neosky.servicesupport.data.local.dao.TicketDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.DATABASE_NAME)
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideDroneDao(db: AppDatabase): DroneDao = db.droneDao()

    @Provides
    fun provideTicketDao(db: AppDatabase): TicketDao = db.ticketDao()

    @Provides
    fun provideFlightLogDao(db: AppDatabase): FlightLogDao = db.flightLogDao()

    @Provides
    fun provideNotificationDao(db: AppDatabase): NotificationDao = db.notificationDao()
}
