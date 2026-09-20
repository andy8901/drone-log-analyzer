package com.neosky.servicesupport.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.neosky.servicesupport.data.local.entity.TicketEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TicketDao {
    @Query("SELECT * FROM tickets ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<TicketEntity>>

    @Query("SELECT * FROM tickets WHERE id = :ticketId")
    fun observeById(ticketId: String): Flow<TicketEntity?>

    @Upsert
    suspend fun upsertAll(tickets: List<TicketEntity>)

    @Upsert
    suspend fun upsert(ticket: TicketEntity)

    @Query("DELETE FROM tickets")
    suspend fun clearAll()
}
