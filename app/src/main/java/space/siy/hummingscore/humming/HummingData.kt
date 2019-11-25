package space.siy.hummingscore.humming

import com.google.gson.Gson
import android.R.attr.data
import java.io.*


data class HummingData(
    var name: String,
    val option: HummingOption,
    val notes: MutableList<Int>,
    val previewSamples: MutableList<Byte>
)

fun HummingData.save(file: File) {
    val gson = Gson()
    val data = gson.toJson(this)
    val outputStreamWriter = OutputStreamWriter(FileOutputStream(file))
    outputStreamWriter.write(data)
    outputStreamWriter.close()
}
