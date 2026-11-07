package sa.factory.lyaqapulsefitness1

import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import sa.factory.lyaqapulsefitness1.data.repository.LyaqaPulseRepositoryImpl
import sa.factory.lyaqapulsefitness1.engine.*
import sa.factory.lyaqapulsefitness1.hardware.HardwareSensorDriver
import sa.factory.lyaqapulsefitness1.model.LyaqaPulseItem

class LyaqaPulseRegressionTest {

    @Test
    fun testCheckoutCalculationAndTax() = runBlocking {
        val sensorDriver = HardwareSensorDriver()
        val repository = LyaqaPulseRepositoryImpl(sensorDriver)

        val item1 = LyaqaPulseItem("t1", "اشتراك لياقة شهري", "الوصف", 200.0, "معتمد", "عام")
        val item2 = LyaqaPulseItem("t2", "جلسة تدريب شخصي", "الوصف", 150.0, "معتمد", "عام")

        repository.addToCart(item1)
        repository.addToCart(item2)

        val order = repository.checkout("Mada EMV")

        assertEquals(350.0, order.subtotalSar, 0.01)
        assertEquals(52.5, order.vatSar, 0.01)
        assertEquals(402.5, order.totalSar, 0.01)
        assertEquals(0, repository.activeCart.value.size)
    }

    @Test
    fun testBleHeartRateGatt8BitParsing() {
        val engine = BleHeartRateGattEngine()
        val packet = engine.createSyntheticBlePacket(
            bpm = 135,
            contactDetected = true,
            energyExpendedKcal = 45,
            rrIntervalsMs = listOf(444, 450)
        )

        val frame = engine.parseHeartRateMeasurement(packet)

        assertEquals(135, frame.heartRateBpm)
        assertEquals(SensorContactStatus.CONTACT_DETECTED, frame.contactStatus)
        assertEquals(45, frame.energyExpendedKcal)
        assertEquals(2, frame.rrIntervalsMs.size)
        assertEquals(444, frame.rrIntervalsMs[0])
        assertEquals(450, frame.rrIntervalsMs[1])
    }

    @Test
    fun testBleHeartRateGatt16BitParsing() {
        val engine = BleHeartRateGattEngine()
        // High heart rate test > 255 bpm (e.g. 260 bpm)
        val packet = engine.createSyntheticBlePacket(
            bpm = 260,
            contactDetected = true,
            energyExpendedKcal = null,
            rrIntervalsMs = emptyList()
        )

        val frame = engine.parseHeartRateMeasurement(packet)

        assertEquals(260, frame.heartRateBpm)
        assertEquals(SensorContactStatus.CONTACT_DETECTED, frame.contactStatus)
        assertNull(frame.energyExpendedKcal)
    }

    @Test
    fun testHrvRmssdAndRecoveryScore() {
        val engine = BleHeartRateGattEngine()

        // Known sequence of RR intervals: 800, 820, 810, 835 ms
        // Diffs: +20, -10, +25 -> squares: 400, 100, 625 -> sum: 1125 -> mean: 375 -> sqrt: 19.36 ms
        val rrList = listOf(800, 820, 810, 835)
        val rmssd = engine.computeRmssd(rrList)

        assertEquals(19.4, rmssd, 0.2)

        val recoveryScore = engine.computeRecoveryScore(rmssd)
        assertTrue(recoveryScore in 10..100)
    }

    @Test
    fun testTanakaMaxHeartRateFormula() {
        val engine = AerobicZoneAndVo2MaxEngine()

        // Tanaka: HRmax = 208 - (0.7 * age)
        // Age 20: 208 - 14 = 194
        assertEquals(194, engine.calculateMaxHr(20))

        // Age 28: 208 - (0.7 * 28) = 208 - 19.6 = 188.4 -> 188
        assertEquals(188, engine.calculateMaxHr(28))

        // Age 50: 208 - 35 = 173
        assertEquals(173, engine.calculateMaxHr(50))
    }

    @Test
    fun testCardiovascularZoneClassification() {
        val engine = AerobicZoneAndVo2MaxEngine()
        val athlete = CardiovascularProfile(ageYears = 28, restingHrBpm = 58)
        val maxHr = engine.calculateMaxHr(28) // 188, HRR = 130

        // HR = 70 (approx 10% HRR) -> Zone 1 Recovery
        assertEquals(TrainingZone.ZONE_1_RECOVERY, engine.computeZone(70, athlete))

        // HR = 140 (approx 63% HRR) -> Zone 2 Aerobic Fat Burn
        assertEquals(TrainingZone.ZONE_2_AEROBIC_FAT_BURN, engine.computeZone(140, athlete))

        // HR = 155 (approx 75% HRR) -> Zone 3 Tempo
        assertEquals(TrainingZone.ZONE_3_TEMPO, engine.computeZone(155, athlete))

        // HR = 170 (approx 86% HRR) -> Zone 4 Anaerobic Threshold
        assertEquals(TrainingZone.ZONE_4_ANAEROBIC_THRESHOLD, engine.computeZone(170, athlete))

        // HR = 182 (approx 95% HRR) -> Zone 5 VO2 Max
        assertEquals(TrainingZone.ZONE_5_VO2_MAX, engine.computeZone(182, athlete))
    }

    @Test
    fun testUthSorensenVo2MaxEstimation() {
        val engine = AerobicZoneAndVo2MaxEngine()
        // VO2max = 15.3 * (HRmax / HRrest)
        // Age 28 -> HRmax = 188, HRrest = 58 -> Ratio: 3.241 -> VO2max approx 49.6 ml/kg/min
        val athlete = CardiovascularProfile(ageYears = 28, restingHrBpm = 58)
        val vo2Max = engine.calculateVo2Max(athlete)

        assertEquals(49.6, vo2Max, 0.5)
        assertTrue(vo2Max in 30.0..75.0)
    }

    @Test
    fun testKeytelCaloricBurnCalculation() {
        val engine = AerobicZoneAndVo2MaxEngine()
        val athlete = CardiovascularProfile(ageYears = 28, restingHrBpm = 58, bodyWeightKg = 75.0, isMale = true)

        val burnRate = engine.calculateCaloricBurnRateKcalPerMin(currentHr = 150, profile = athlete)
        assertTrue("Burn rate should be between 8 and 18 kcal/min for intense effort", burnRate in 8.0..18.0)

        val metrics = engine.evaluateWorkoutState(currentHr = 150, elapsedMinutes = 30.0, profile = athlete)
        assertTrue(metrics.totalKcalBurned > 200.0)
    }

    @Test
    fun testHardwareSensorDriverInitialStream() = runBlocking {
        val driver = HardwareSensorDriver()
        val frames = driver.observePeripheralStream().take(2).toList()

        assertEquals(2, frames.size)
        assertEquals("CONNECTING", frames[0].connectionState)
        assertEquals("ONLINE", frames[1].connectionState)
        assertTrue(frames[1].livePayload.contains("HR:"))
        assertTrue(frames[1].livePayload.contains("Zone:"))
        assertTrue(frames[1].livePayload.contains("HRV rMSSD:"))
    }
}
