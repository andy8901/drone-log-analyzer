package com.neosky.servicesupport.core.di

import com.neosky.servicesupport.data.repository.AuthRepositoryImpl
import com.neosky.servicesupport.data.repository.CustomerRepositoryImpl
import com.neosky.servicesupport.data.repository.DocumentRepositoryImpl
import com.neosky.servicesupport.data.repository.DroneRepositoryImpl
import com.neosky.servicesupport.data.repository.FlightRepositoryImpl
import com.neosky.servicesupport.data.repository.InvoiceRepositoryImpl
import com.neosky.servicesupport.data.repository.MaintenanceRepositoryImpl
import com.neosky.servicesupport.data.repository.NotificationRepositoryImpl
import com.neosky.servicesupport.data.repository.SearchRepositoryImpl
import com.neosky.servicesupport.data.repository.ServiceHistoryRepositoryImpl
import com.neosky.servicesupport.data.repository.TicketRepositoryImpl
import com.neosky.servicesupport.data.repository.WarrantyRepositoryImpl
import com.neosky.servicesupport.domain.repository.AuthRepository
import com.neosky.servicesupport.domain.repository.CustomerRepository
import com.neosky.servicesupport.domain.repository.DocumentRepository
import com.neosky.servicesupport.domain.repository.DroneRepository
import com.neosky.servicesupport.domain.repository.FlightRepository
import com.neosky.servicesupport.domain.repository.InvoiceRepository
import com.neosky.servicesupport.domain.repository.MaintenanceRepository
import com.neosky.servicesupport.domain.repository.NotificationRepository
import com.neosky.servicesupport.domain.repository.SearchRepository
import com.neosky.servicesupport.domain.repository.ServiceHistoryRepository
import com.neosky.servicesupport.domain.repository.TicketRepository
import com.neosky.servicesupport.domain.repository.WarrantyRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindCustomerRepository(impl: CustomerRepositoryImpl): CustomerRepository

    @Binds
    @Singleton
    abstract fun bindDroneRepository(impl: DroneRepositoryImpl): DroneRepository

    @Binds
    @Singleton
    abstract fun bindTicketRepository(impl: TicketRepositoryImpl): TicketRepository

    @Binds
    @Singleton
    abstract fun bindFlightRepository(impl: FlightRepositoryImpl): FlightRepository

    @Binds
    @Singleton
    abstract fun bindWarrantyRepository(impl: WarrantyRepositoryImpl): WarrantyRepository

    @Binds
    @Singleton
    abstract fun bindMaintenanceRepository(impl: MaintenanceRepositoryImpl): MaintenanceRepository

    @Binds
    @Singleton
    abstract fun bindInvoiceRepository(impl: InvoiceRepositoryImpl): InvoiceRepository

    @Binds
    @Singleton
    abstract fun bindServiceHistoryRepository(impl: ServiceHistoryRepositoryImpl): ServiceHistoryRepository

    @Binds
    @Singleton
    abstract fun bindDocumentRepository(impl: DocumentRepositoryImpl): DocumentRepository

    @Binds
    @Singleton
    abstract fun bindNotificationRepository(impl: NotificationRepositoryImpl): NotificationRepository

    @Binds
    @Singleton
    abstract fun bindSearchRepository(impl: SearchRepositoryImpl): SearchRepository
}
