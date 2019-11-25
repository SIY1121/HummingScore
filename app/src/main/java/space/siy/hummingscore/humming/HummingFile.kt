package space.siy.hummingscore.humming

import com.google.gson.Gson
import io.reactivex.Observable
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream

class HummingFile(val path: String) : HummingSource {
    override var hummingData = HummingData("", HummingOption(), arrayListOf(), arrayListOf())
    val file = File(path)

    init {
        val bufferedReader: BufferedReader = file.bufferedReader()
        val inputString = bufferedReader.use { it.readText() }
        val gson = Gson()
        hummingData = gson.fromJson<HummingData>(inputString, HummingData::class.java)
    }

    override fun start() {
    }

    override fun stop() {
    }

    override fun rename(name: String) {
        hummingData.name = name
        hummingData.save(file)
    }


    override val notes: MutableList<Int>
        get() = hummingData.notes
    override val previewSamples: MutableList<Byte>
        get() = hummingData.previewSamples
    override var notesObservable: Observable<Int>
        get() = Observable.fromIterable(hummingData.notes)
        set(value) {}
    override var previewSampleObservable: Observable<Byte>
        get() = Observable.fromIterable(hummingData.previewSamples)
        set(value) {}
}
