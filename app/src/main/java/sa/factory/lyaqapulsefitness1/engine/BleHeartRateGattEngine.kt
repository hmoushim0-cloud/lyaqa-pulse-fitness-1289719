package sa.factory.lyaqapulsefitness1.engine

import kotlin.math.roundToInt
import kotlin.math.sqrt

enum class SensorContactStatus {
    NOT_SUPPORTED,
    NO_CONTACT,
    CONTACT_DETECTED
}

data class DecodedHeartRateFrame(
    val heartRateBpm: Int,
    val contactStatus: SensorContactStatus,
    val energyExpendedKcal: Int?,
    val rrIntervalsMs: List<Int>,
    val rmssdMs: Double,
    val recoveryScorePct: Int,
    val timestamp: Long = System.currentTimeMillis()
)

class BleHeartRateGattEngine {

    companion object {
        const val GATT_HEART_RATE_SERVICE_UUID = "0000180D-0000-1000-8000-00805F9B34FB"
        const val GATT_HEART_RATE_MEASUREMENT_UUID = "00002A37-0000-1000-8000-00805F9B34FB"
    }

    private val rrHistory = mutableListOf<Int>()

    fun parseHeartRateMeasurement(packet: ByteArray): DecodedHeartRateFrame {
        require(packet.isNotEmpty()) { "Packet must contain at least flags byte" }

        val flags = packet[0].toInt() and 0xFF
        val isHr16Bit = (flags and 0x01) != 0
        val contactBits = (flags shr 1) and 0x03
        val contactStatus = when (contactBits) {
            2 -> SensorContactStatus.NO_CONTACT
            3 -> SensorContactStatus.CONTACT_DETECTED
            else -> SensorContactStatus.NOT_SUPPORTED
        }
        val isEnergyExpendedPresent = (flags and 0x08) != 0
        val isRrIntervalPresent = (flags and 0x10) != 0

        var offset = 1

        val heartRateBpm = if (isHr16Bit) {
            val low = packet[offset].toInt() and 0xFF
            val high = packet[offset + 1].toInt() and 0xFF
            offset += 2
            (high shl 8) or low
        } else {
            val bpm = packet[offset].toInt() and 0xFF
            offset += 1
            bpm
        }

        val energyExpendedKcal = if (isEnergyExpendedPresent && offset + 1 < packet.size) {
            val low = packet[offset].toInt() and 0xFF
            val high = packet[offset + 1].toInt() and 0xFF
            offset += 2
            (high shl 8) or low
        } else null

        val currentRrList = mutableListOf<Int>()
        if (isRrIntervalPresent) {
            while (offset + 1 < packet.size) {
                val low = packet[offset].toInt() and 0xFF
                val high = packet[offset + 1].toInt() and 0xFF
                offset += 2
                val raw1024 = (high shl 8) or low
                val rrMs = ((raw1024 * 1000L) / 1024L).toInt()
                currentRrList.add(rrMs)
            }
        }

        if (currentRrList.isNotEmpty()) {
            rrHistory.addAll(currentRrList)
            while (rrHistory.size > 50) {
                rrHistory.removeAt(0)
            }
        }

        val rmssd = computeRmssd(if (rrHistory.isNotEmpty()) rrHistory else currentRrList)
        val recoveryScore = computeRecoveryScore(rmssd)

        return DecodedHeartRateFrame(
            heartRateBpm = heartRateBpm,
            contactStatus = contactStatus,
            energyExpendedKcal = energyExpendedKcal,
            rrIntervalsMs = currentRrList,
            rmssdMs = rmssd,
            recoveryScorePct = recoveryScore
        )
    }

    fun computeRmssd(rrList: List<Int>): Double {
        if (rrList.size < 2) return 38.0 // Population baseline fallback
        var sumSquares = 0.0
        for (i in 0 until rrList.size - 1) {
            val diff = (rrList[i + 1] - rrList[i]).toDouble()
            sumSquares += diff * diff
        }
        val meanSquare = sumSquares / (rrList.size - 1)
        val rmssd = sqrt(meanSquare)
        return (rmssd * 10.0).roundToInt() / 10.0
    }

    fun computePnn50(rrList: List<Int>): Double {
        if (rrList.size < 2) return 0.0
        var count50 = 0
        for (i in 0 until rrList.size - 1) {
            val diff = Math.abs(rrList[i + 1] - rrList[i])
            if (diff > 50) count50++
        }
        return (count50.toDouble() / (rrList.size - 1)) * 100.0
    }

    fun computeRecoveryScore(rmssd: Double): Int {
        // Logarithmic sigmoid mapping for rMSSD (typical adult 20-80ms -> 30-100%)
        val score = (rmssd * 1.25).coerceIn(10.0, 100.0).roundToInt()
        return score
    }

    fun createSyntheticBlePacket(
        bpm: Int,
        contactDetected: Boolean = true,
        energyExpendedKcal: Int? = null,
        rrIntervalsMs: List<Int> = emptyList()
    ): ByteArray {
        var flags = 0
        if (bpm > 255) {
            flags = flags or 0x01 // 16-bit HR
        }
        if (contactDetected) {
            flags = flags or (3 shl 1) // Sensor contact supported and detected
        } else {
            flags = flags or (2 shl 1) // Contact supported but not detected
        }
        if (energyExpendedKcal != null) {
            flags = flags or 0x08 // Energy expended present
        }
        if (rrIntervalsMs.isNotEmpty()) {
            flags = flags or 0x10 // RR intervals present
        }

        val bytes = mutableListOf<Byte>()
        bytes.add(flags.toByte())

        if (bpm > 255) {
            bytes.add((bpm and 0xFF).toByte())
            bytes.add(((bpm shr 8) and 0xFF).toByte())
        } else {
            bytes.add((bpm and 0xFF).toByte())
        }

        if (energyExpendedKcal != null) {
            bytes.add((energyExpendedKcal and 0xFF).toByte())
            bytes.add(((energyExpendedKcal shr 8) and 0xFF).toByte())
        }

        for (rrMs in rrIntervalsMs) {
            val raw1024 = ((rrMs * 1024L) / 1000L).toInt()
            bytes.add((raw1024 and 0xFF).toByte())
            bytes.add(((raw1024 shr 8) and 0xFF).toByte())
        }

        return bytes.toByteArray()
    }
}
