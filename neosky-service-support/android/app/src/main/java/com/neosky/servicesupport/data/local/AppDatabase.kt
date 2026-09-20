package com.neosky.servicesupport.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.neosky.servicesupport.data.local.dao.DroneDao
import com.neosky.servicesupport.data.local.dao.FlightLogDao
import com.neosky.servicesupport.data.local.dao.NotificationDao
import com.neosky.servicesupport.data.local.dao.TicketDao
import com.neosky.servicesupport.data.local.entity.DroneEntity
import com.neosky.servicesupport.data.local.entity.FlightLogEntity
import com.neosky.servicesupport.data.local.entity.NotificationEntity
import com.neosky.servicesupport.data.local.entity.TicketEntity

@Database(
    entities = [
        DroneEntity::class,
        TicketEntity::class,
        FlightLogEntity::class,
        NotificationEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun droneDao(): DroneDao
    abstract fun ticketDao(): TicketDao
    abstract fun flightLogDao(): FlightLogDao
    abstract fun notificationDao(): NotificationDao

    companion object {
        const val DATABASE_NAME = "neosky_service_support.db"
    }
}
