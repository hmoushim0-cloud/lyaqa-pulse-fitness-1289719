package sa.factory.lyaqapulsefitness1.engine

import kotlin.math.roundToInt

enum class TrainingZone(val arabicName: String, val lowerPct: Double, val upperPct: Double) {
    ZONE_1_RECOVERY("الاستشفاء النشط", 0.50, 0.60),
    ZONE_2_AEROBIC_FAT_BURN("حرق الدهون الهوائي", 0.60, 0.70),
    ZONE_3_TEMPO("الجهد الهوائي المتواصل", 0.70, 0.80),
    ZONE_4_ANAEROBIC_THRESHOLD("عتبة اللاكتات اللاهوائية", 0.80, 0.90),
    ZONE_5_VO2_MAX("الجهد الأقصى والسرعة القصوى", 0.90, 1.00)
}

data class CardiovascularProfile(
    val ageYears: Int = 28,
    val restingHrBpm: Int = 58,
    val bodyWeightKg: Double = 75.0,
    val isMale: Boolean = true
)

data class WorkoutMetrics(
    val currentHr: Int,
    val maxHr: Int,
    val currentZone: TrainingZone,
    val percentageHrMax: Double,
    val targetHrRange: Pair<Int, Int>,
    val estimatedVo2Max: Double,
    val totalKcalBurned: Double,
    val sessionElapsedMinutes: Double
)

class AerobicZoneAndVo2MaxEngine {

    fun calculateMaxHr(ageYears: Int): Int {
        // Tanaka formula: HRmax = 208 - (0.7 * age)
        return (208.0 - (0.7 * ageYears.toDouble())).roundToInt()
    }

    fun computeZone(currentHr: Int, profile: CardiovascularProfile): TrainingZone {
        val maxHr = calculateMaxHr(profile.ageYears)
        val hrr = maxHr - profile.restingHrBpm
        val intensity = if (hrr > 0) {
            ((currentHr - profile.restingHrBpm).toDouble() / hrr.toDouble()).coerceIn(0.0, 1.0)
        } else {
            (currentHr.toDouble() / maxHr.toDouble()).coerceIn(0.0, 1.0)
        }

        return when {
            intensity >= 0.90 -> TrainingZone.ZONE_5_VO2_MAX
            intensity >= 0.80 -> TrainingZone.ZONE_4_ANAEROBIC_THRESHOLD
            intensity >= 0.70 -> TrainingZone.ZONE_3_TEMPO
            intensity >= 0.60 -> TrainingZone.ZONE_2_AEROBIC_FAT_BURN
            else -> TrainingZone.ZONE_1_RECOVERY
        }
    }

    fun calculateVo2Max(profile: CardiovascularProfile): Double {
        // Uth-Sørensen-Overgaard-Pedersen formula: VO2max = 15.3 * (HRmax / HRrest)
        val maxHr = calculateMaxHr(profile.ageYears)
        val ratio = maxHr.toDouble() / profile.restingHrBpm.coerceAtLeast(40).toDouble()
        val vo2Max = 15.3 * ratio
        return (vo2Max * 10.0).roundToInt() / 10.0
    }

    fun calculateCaloricBurnRateKcalPerMin(currentHr: Int, profile: CardiovascularProfile): Double {
        // Keytel metabolic caloric expenditure formula
        val rate = if (profile.isMale) {
            (-55.0969 + (0.6309 * currentHr) + (0.1988 * profile.bodyWeightKg) + (0.2017 * profile.ageYears)) / 4.184
        } else {
            (-20.4022 + (0.4472 * currentHr) - (0.1263 * profile.bodyWeightKg) + (0.0740 * profile.ageYears)) / 4.184
        }
        return rate.coerceAtLeast(1.0)
    }

    fun evaluateWorkoutState(
        currentHr: Int,
        elapsedMinutes: Double,
        profile: CardiovascularProfile = CardiovascularProfile()
    ): WorkoutMetrics {
        val maxHr = calculateMaxHr(profile.ageYears)
        val zone = computeZone(currentHr, profile)
        val pctMax = ((currentHr.toDouble() / maxHr.toDouble()) * 100.0 * 10.0).roundToInt() / 10.0

        val hrr = maxHr - profile.restingHrBpm
        val lowHr = profile.restingHrBpm + (hrr * zone.lowerPct).roundToInt()
        val highHr = profile.restingHrBpm + (hrr * zone.upperPct).roundToInt()

        val vo2Max = calculateVo2Max(profile)
        val burnRate = calculateCaloricBurnRateKcalPerMin(currentHr, profile)
        val totalKcal = (burnRate * elapsedMinutes * 10.0).roundToInt() / 10.0

        return WorkoutMetrics(
            currentHr = currentHr,
            maxHr = maxHr,
            currentZone = zone,
            percentageHrMax = pctMax,
            targetHrRange = Pair(lowHr, highHr),
            estimatedVo2Max = vo2Max,
            totalKcalBurned = totalKcal,
            sessionElapsedMinutes = elapsedMinutes
        )
    }
}
