package space.siy.hummingscore.humming

import kotlin.math.max

object TempoDetector {
    val taptimes: MutableList<Long> = mutableListOf()
    fun tap(): Int {
        val last = taptimes.lastOrNull()
        if (last != null && System.currentTimeMillis() - last > 3000) {
            taptimes.clear()
        }
        taptimes.add(System.currentTimeMillis())
        if (taptimes.size <= 1) return 120

        val interval = mutableListOf<Long>()
        for (i in max(0, taptimes.size - 5) until taptimes.size - 1) {
            interval.add(taptimes[i + 1] - taptimes[i])
        }
        return (60f / (interval.average() / 1000f)).toInt()
    }
}
