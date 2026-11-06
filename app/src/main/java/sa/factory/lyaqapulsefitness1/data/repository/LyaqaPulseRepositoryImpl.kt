package sa.factory.lyaqapulsefitness1.data.repository

import sa.factory.lyaqapulsefitness1.domain.repository.LyaqaPulseRepository
import sa.factory.lyaqapulsefitness1.hardware.HardwareSensorDriver
import sa.factory.lyaqapulsefitness1.model.LyaqaPulseItem
import sa.factory.lyaqapulsefitness1.model.LyaqaPulseOrder
import sa.factory.lyaqapulsefitness1.model.LyaqaPulseTelemetry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class LyaqaPulseRepositoryImpl(
    private val sensorDriver: HardwareSensorDriver
) : LyaqaPulseRepository {

    private val _catalog = MutableStateFlow<List<LyaqaPulseItem>>(listOf(
        LyaqaPulseItem("1", "الباقة التنفيذية المعتمدة", "تكامل مباشر مع BLE Smart Scale GATT 0x181D + Heart Rate Strap", 180.0, "معتمد", "الخدمات الرئيسية"),
        LyaqaPulseItem("2", "خدمة المعالجة الفورية", "أداء أصيل بسرعة 60 إطار في الثانية", 95.0, "فوري", "المراقبة الحية"),
        LyaqaPulseItem("3", "باقة الصيانة والدعم السحابي", "تشفير كامل للبيانات المحلية ومزامنة آمنة", 250.0, "شامل", "التقارير الضريبية")
    ))
    override val catalogFlow: Flow<List<LyaqaPulseItem>> = _catalog.asStateFlow()

    private val _cart = MutableStateFlow<List<LyaqaPulseItem>>(emptyList())
    override val activeCart: StateFlow<List<LyaqaPulseItem>> = _cart.asStateFlow()

    override val telemetryFlow: Flow<LyaqaPulseTelemetry> = sensorDriver.observePeripheralStream()
        .map { frame ->
            LyaqaPulseTelemetry(
                peripheralName = frame.peripheralName,
                status = frame.connectionState,
                liveReading = frame.livePayload,
                batteryPct = 96,
                signalDbm = -54
            )
        }

    override fun addToCart(item: LyaqaPulseItem) {
        val current = _cart.value.toMutableList()
        current.add(item)
        _cart.value = current
    }

    override fun removeFromCart(item: LyaqaPulseItem) {
        val current = _cart.value.toMutableList()
        current.remove(item)
        _cart.value = current
    }

    override fun clearCart() {
        _cart.value = emptyList()
    }

    override suspend fun checkout(paymentMethod: String): LyaqaPulseOrder {
        val currentItems = _cart.value.toList()
        val subtotal = currentItems.sumOf { it.priceSar }
        val vat = subtotal * 0.15
        val total = subtotal + vat
        val order = LyaqaPulseOrder(
            orderId = "ORD-${System.currentTimeMillis() % 100000}",
            items = currentItems,
            subtotalSar = subtotal,
            vatSar = vat,
            totalSar = total,
            paymentMethod = paymentMethod
        )
        clearCart()
        return order
    }

    override suspend fun refreshCatalog() {
        // Offline-first sync logic
    }
}
