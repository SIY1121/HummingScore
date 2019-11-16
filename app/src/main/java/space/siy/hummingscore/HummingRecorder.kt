package space.siy.hummingscore

import io.reactivex.schedulers.Schedulers
import java.io.File

class HummingRecorder(val file: File, val bpm: Int = 120, val noteLengthResolution: Int = 16) {
    val sampleRate = 44100

    val oneFrameSampleCount = (sampleRate * 60 / bpm) / (noteLengthResolution / 4)
    val recorder = AudioRecorder(sampleRate, oneFrameSampleCount)
    val writer = WavWriter(sampleRate, file)

    fun start() = recorder.start().observeOn(Schedulers.computation()).map {
        writer.writeSample(it)
        PitchDetector.analyze(it)
    }

    fun stop() {
        recorder.stop()
        writer.flush()
    }
}
