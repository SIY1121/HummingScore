package space.siy.hummingscore.humming

import be.tarsos.dsp.pitch.McLeodPitchMethod
import kotlin.math.abs
import kotlin.math.pow

/**
 * McLeod Pitch Method を用いてピッチを解析する
 */
object PitchDetector {

    /** 音程と周波数の対応付けを格納するリスト */
    val levels = (0..88).map { i ->
        (27.5 * (2.0.pow(1 / 12.0)).pow(i)).toInt()
    }

    fun analyze(arr: ShortArray) = McLeodPitchMethod(44100f, arr.size).run {
        getPitch(arr.map { it / Short.MAX_VALUE.toFloat() }.toFloatArray()).let { res ->
            var dis = abs(res.pitch - levels[0])
            (1..87).forEach { i ->
                if (dis < abs(res.pitch - levels[i]))
                    return@let i - 1
                dis = abs(res.pitch - levels[i])
            }
            return@let 0
        }
    }
}

val degrees = arrayOf("A", "B♭", "B", "C", "C#", "D", "E♭", "E", "F", "F#", "G", "G#")

/** 鍵盤番号を名前に変換 */
fun Int.toNoteName(): String {
    val degree = this % 12
    val octaveNumber = (this - 3) / 12
    return degrees[degree] + octaveNumber
}
