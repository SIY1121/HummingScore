package space.siy.hummingscore

import io.reactivex.schedulers.Schedulers
import java.io.File
import kotlin.math.abs

class HummingRecorder(val file: File, val option: HummingOption) {
    val sampleRate = 44100

    val oneFrameSampleCount = (sampleRate * 60 / option.bpm) / (option.noteResolution / 4)
    val recorder = AudioRecorder(sampleRate, oneFrameSampleCount)
    val writer = WavWriter(sampleRate, file)

    data class Data(val note: Int, val samples: List<Byte>)

    val previewSampleRate = 2

    fun start() = recorder.start().observeOn(Schedulers.computation()).map { samples ->
        writer.writeSample(samples)
        val onePreviewSamples = samples.size / previewSampleRate
        Data(
            PitchDetector.analyze(samples),
            samples.toList().windowed(onePreviewSamples, onePreviewSamples).map { l ->
                ((l.map { abs(it.toFloat()) / Short.MAX_VALUE }.max()
                    ?: -1f) * Byte.MAX_VALUE).toByte()
            }
        )
    }

    fun stop() {
        recorder.stop()
        writer.flush()
    }
}
