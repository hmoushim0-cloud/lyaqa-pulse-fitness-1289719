package sa.factory.lyaqapulsefitness1.hardware

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import sa.factory.lyaqapulsefitness1.engine.*
import kotlin.random.Random

data class HardwareTelemetryFrame(
    val peripheralName: String,
    val connectionState: String,
    val livePayload: String,
    val timestamp: Long = System.currentTimeMillis()
)

class HardwareSensorDriver {
    val peripheral = "Bluetooth BLE Heart Rate Strap (GATT 0x180D)"
    val gattEngine = BleHeartRateGattEngine()
    val zoneEngine = AerobicZoneAndVo2MaxEngine()

    fun observePeripheralStream(): Flow<HardwareTelemetryFrame> = flow {
        emit(HardwareTelemetryFrame(peripheral, "CONNECTING", "BLE GATT 0x180D Heart Rate Service Handshake initializing..."))
        delay(200)

        // Seed initial rest calibration
        val athlete = CardiovascularProfile(ageYears = 26, restingHrBpm = 54, bodyWeightKg = 73.0, isMale = true)
        val initialPacket = gattEngine.createSyntheticBlePacket(
            bpm = 62,
            contactDetected = true,
            energyExpendedKcal = 12,
            rrIntervalsMs = listOf(968, 972, 960)
        )
        val initialDecoded = gattEngine.parseHeartRateMeasurement(initialPacket)
        val initialWorkout = zoneEngine.evaluateWorkoutState(initialDecoded.heartRateBpm, elapsedMinutes = 1.0, athlete)

        val initPayload = "[ONLINE] HR: " + initialDecoded.heartRateBpm + " BPM | " +
                "Zone: " + initialWorkout.currentZone.name + " (" + initialWorkout.percentageHrMax + "%) | " +
                "HRV rMSSD: " + initialDecoded.rmssdMs + "ms | Recovery: " + initialDecoded.recoveryScorePct + "%"
        emit(HardwareTelemetryFrame(peripheral, "ONLINE", initPayload))

        var frameCounter = 1
        var sessionMinutes = 1.0

        while (true) {
            delay(1500)
            frameCounter++
            sessionMinutes += 0.25
            val jitter = Random.nextInt(1, 8)

            // Dynamic progression through aerobic and tempo workout phases
            val targetBpm = when {
                frameCounter < 6 -> Random.nextInt(115, 130) // Zone 2 Aerobic
                frameCounter < 12 -> Random.nextInt(135, 155) // Zone 3 Tempo
                frameCounter < 18 -> Random.nextInt(160, 175) // Zone 4 Anaerobic
                else -> Random.nextInt(120, 140) // Cool down
            }

            val rrBase = (60000 / targetBpm)
            val rrNoise1 = rrBase + Random.nextInt(-35, 35)
            val rrNoise2 = rrBase + Random.nextInt(-45, 45)

            val blePacket = gattEngine.createSyntheticBlePacket(
                bpm = targetBpm,
                contactDetected = true,
                energyExpendedKcal = (sessionMinutes * 11.2).toInt(),
                rrIntervalsMs = listOf(rrNoise1, rrNoise2)
            )

            val decoded = gattEngine.parseHeartRateMeasurement(blePacket)
            val workout = zoneEngine.evaluateWorkoutState(decoded.heartRateBpm, sessionMinutes, athlete)

            val liveStatus = "[ACTIVE_STREAMING] Frame #" + frameCounter + " (+" + jitter + "ms) | " +
                    "HR: " + decoded.heartRateBpm + " BPM (" + workout.currentZone.name + ") | " +
                    "Burn: " + String.format("%.1f", workout.totalKcalBurned) + " kcal | " +
                    "VO2max: " + workout.estimatedVo2Max + " | HRV: " + decoded.rmssdMs + "ms"

            emit(HardwareTelemetryFrame(peripheral, "ACTIVE_STREAMING", liveStatus))
        }
    }
}
