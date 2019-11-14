package space.siy.hummingscore

import java.io.File

class HummingRecorder(val file: File) {
    val recorder = AudioRecorder(44100)
    val writer = WavWriter(44100, file)

    fun start() = recorder.start().map {
        writer.writeSample(it)
        PitchDetector.analyze(it)
    }

    fun stop() {
        recorder.stop()
    }
}
