package sa.factory.lyaqapulsefitness1.di

import sa.factory.lyaqapulsefitness1.data.repository.LyaqaPulseRepositoryImpl
import sa.factory.lyaqapulsefitness1.domain.repository.LyaqaPulseRepository
import sa.factory.lyaqapulsefitness1.domain.usecase.*
import sa.factory.lyaqapulsefitness1.hardware.HardwareSensorDriver
import sa.factory.lyaqapulsefitness1.ui.viewmodel.LyaqaPulseViewModel

class DiContainer {
    val sensorDriver = HardwareSensorDriver()
    val repository: LyaqaPulseRepository = LyaqaPulseRepositoryImpl(sensorDriver)

    val getCatalogUseCase = GetCatalogUseCase(repository)
    val manageCartUseCase = ManageCartUseCase(repository)
    val processCheckoutUseCase = ProcessCheckoutUseCase(repository)
    val observeHardwareTelemetryUseCase = ObserveHardwareTelemetryUseCase(repository)

    fun provideViewModel(): LyaqaPulseViewModel = LyaqaPulseViewModel(
        getCatalogUseCase,
        manageCartUseCase,
        processCheckoutUseCase,
        observeHardwareTelemetryUseCase
    )
}
