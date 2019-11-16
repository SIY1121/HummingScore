package space.siy.hummingscore

import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import java.io.File
import kotlin.math.abs

class HummingRecorder(val file: File, val option: HummingOption) {
    val sampleRate = 44100

    val oneFrameSampleCount = (sampleRate * 60 / option.bpm) / (option.noteResolution / 4)
    val recorder = AudioRecorder(sampleRate, oneFrameSampleCount / option.previewWaveSampleRate)
    val writer = WavWriter(sampleRate, file)

    data class Data(val tone: Int, val samples: List<Byte>)

    val tones = MutableList<Int>(0) { _ -> 0 }

    lateinit var sampleObservable: Observable<ShortArray>
    lateinit var notesObservable: Observable<Int>
    lateinit var previewSampleObservable: Observable<Byte>
    fun start() {
        sampleObservable = recorder.start().observeOn(Schedulers.computation())
        sampleObservable.subscribe {
            writer.writeSample(it)
        }
        notesObservable = sampleObservable
            .buffer(option.previewWaveSampleRate)
            .map { _samples ->
                val sampleCount = _samples.sumBy { it.size }
                var samples = ShortArray(0)
                val i = 0
                _samples.forEach {
                    samples += it
                }
                val tone = PitchDetector.analyze(samples)
                synchronized(tones) {
                    tones.add(tone)
                }
                tone
            }
        previewSampleObservable = sampleObservable.map { samples ->
            ((samples.map { it.toFloat() }.max() ?: 0f) / Short.MAX_VALUE * 256).toByte()
        }
    }

    fun stop() {
        recorder.stop()
        writer.flush()
    }
}
