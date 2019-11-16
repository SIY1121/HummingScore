package space.siy.hummingscore

import android.media.*
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.Log
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.abs

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        button.setOnClickListener {
            record()
        }
    }

    var recording = false
    lateinit var recorder: HummingRecorder
    private fun record() {
        if (recording) {
            recorder.stop()
            recording = false
            return
        }
        File(getExternalFilesDir(Environment.DIRECTORY_MUSIC)?.absolutePath + "/humming").mkdir()
        val file = File(getExternalFilesDir(Environment.DIRECTORY_MUSIC)?.absolutePath + "/humming/hoge.wav")
        recorder = HummingRecorder(file)
        recorder.start().observeOn(AndroidSchedulers.mainThread()).subscribe {
            progress_bar.progress = it
            textView.text = it.toNoteName()
            scoreView.addAndDraw(it)
        }
        recording = true
    }
}
