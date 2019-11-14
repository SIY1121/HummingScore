package space.siy.hummingscore

import be.tarsos.dsp.pitch.McLeodPitchMethod
import kotlin.math.pow

object PitchDetector {

    val levels = (0..88).map { i ->
        (27.5 * (2.0.pow(1 / 12.0)).pow(i)).toInt()
    }

    fun analyze(arr: ShortArray) = McLeodPitchMethod(44100f, arr.size).run {
        getPitch(arr.map { it / Short.MAX_VALUE.toFloat() }.toFloatArray()).let { res ->
            (0..87).forEach {i ->
                if (levels[i] <= res.pitch && res.pitch  < levels[i + 1])
                    return@let i
            }
            return@let 0
        }
    }
}

fun Int.toNoteName(): String {
    val a = this % 12
    val b = this / 12
    val name = when (a) {
        0 -> "A"
        1 -> "B♭"
        2 -> "B"
        3 -> "C"
        4 -> "C#"
        5 -> "D"
        6 -> "E♭"
        7 -> "E"
        8 -> "F"
        9 -> "F#"
        10 -> "G"
        11 -> "G#"
        else -> ""
    }
    return name + b
}
