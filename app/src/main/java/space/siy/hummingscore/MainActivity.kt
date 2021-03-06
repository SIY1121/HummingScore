package space.siy.hummingscore

import android.Manifest
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.NavUtils
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.activity_main.*
import permissions.dispatcher.NeedsPermission
import permissions.dispatcher.RuntimePermissions
import space.siy.hummingscore.humming.*
import space.siy.hummingscore.midi.MidiDevice
import space.siy.hummingscore.midi.MidiPlayer
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


@RuntimePermissions
class MainActivity : AppCompatActivity() {
    var hummingOption = HummingOption(120, 16, 1)

    lateinit var recorder: HummingSource
    val player = HummingPlayer(hummingOption)

    var midiDevice: MidiDevice? = null
    var midiPlayer: MidiPlayer? = null

    var recording = false
    var hasData = false
    var now = ""
    var jsonPath = ""
    var path = ""

    var lastTouchedX = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        title = "新規Humming"

        actionBar?.setDisplayShowHomeEnabled(true)

        button_play.isEnabled = false
        button_edit.isEnabled = false

        jsonPath = intent.getStringExtra("path") ?: ""
        if (jsonPath.isNotBlank()) {
            button_record.isEnabled = false
            button_record.alpha = 0.5f
            button_play.alpha = 1f
            button_play.isEnabled = true
            button_edit.alpha = 1f
            button_edit.isEnabled = true
            path = jsonPath.replace(".json", ".wav")
            recorder = HummingFile(jsonPath)
            title = recorder.hummingData.name
            hummingOption = recorder.hummingData.option
            setting_wrapper.visibility = View.GONE
            observe()
            stopRecord()
        } else {
            clearNoteButtonColor()
            note_button_16.compoundDrawablesRelative[0].colorFilter =
                PorterDuffColorFilter(resources.getColor(R.color.colorPrimaryDark), PorterDuff.Mode.SRC_IN)
            hummingOption.noteResolution = 16
        }

        /** 録音ボタン */
        button_record.setOnClickListener {
            if (!recording)
                recordWithPermissionCheck()
            else
                stopRecord()
        }

        /** 再生ボタン */
        button_play.setOnClickListener {
            togglePlayer()
        }

        /** midi接続ボタン */
        button_midi.setOnClickListener {
            showMidiMenu()
        }

        button_edit.setOnClickListener {
            showEditDialog()
        }

        tempo_button.setOnClickListener {
            hummingOption.bpm = TempoDetector.tap()
            tempo_text_view.text = "${hummingOption.bpm} BPM"
        }

        note_button_4.setOnClickListener {
            clearNoteButtonColor()
            note_button_4.compoundDrawablesRelative[0].colorFilter =
            PorterDuffColorFilter(resources.getColor(R.color.colorPrimaryDark), PorterDuff.Mode.SRC_IN)
            hummingOption.noteResolution = 4
        }
        note_button_8.setOnClickListener {
            clearNoteButtonColor()
            note_button_8.compoundDrawablesRelative[0].colorFilter =
                PorterDuffColorFilter(resources.getColor(R.color.colorPrimaryDark), PorterDuff.Mode.SRC_IN)
            hummingOption.noteResolution = 8
        }
        note_button_16.setOnClickListener {
            clearNoteButtonColor()
            note_button_16.compoundDrawablesRelative[0].colorFilter =
                PorterDuffColorFilter(resources.getColor(R.color.colorPrimaryDark), PorterDuff.Mode.SRC_IN)
            hummingOption.noteResolution = 16
        }
        note_button_32.setOnClickListener {
            clearNoteButtonColor()
            note_button_32.compoundDrawablesRelative[0].colorFilter =
                PorterDuffColorFilter(resources.getColor(R.color.colorPrimaryDark), PorterDuff.Mode.SRC_IN)
            hummingOption.noteResolution = 32
        }

        scoreView.innerView.setOnTouchListener { v, event ->
            if (hasData && event.action == MotionEvent.ACTION_DOWN) {
                lastTouchedX = event.x
            }
            false
        }

        scoreView.innerView.setOnClickListener {
            player.seek((lastTouchedX / scoreView.widthPerSec * 1000).toInt())
        }

        scoreView.hummingOption = hummingOption
        scoreView.playerPositionObservable = player.playerTimer.observeOn(AndroidSchedulers.mainThread())
        player.notesStream.observeOn(AndroidSchedulers.mainThread()).subscribe { textView.text = it.toNoteName() }
    }

    @NeedsPermission(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )
    fun record() {
        now = SimpleDateFormat("yyyy-MM-dd-hh-mm-ss-SSS").format(
            Date()
        )
        File(getExternalFilesDir(Environment.DIRECTORY_MUSIC)?.absolutePath + "/humming").mkdir()
        val wavFile = File(getExternalFilesDir(Environment.DIRECTORY_MUSIC)?.absolutePath + "/humming/$now.wav")
        val jsonFile = File(getExternalFilesDir(Environment.DIRECTORY_MUSIC)?.absolutePath + "/humming/$now.json")
        path = wavFile.absolutePath
        recorder = HummingRecorder(wavFile, jsonFile, hummingOption)
        recorder.start()
        observe()
        recording = true
        button_record.setImageResource(R.drawable.ic_stop)
        setting_wrapper.visibility = View.GONE
    }

    fun observe() {
        scoreView.notesObservable = recorder.notesObservable
        recorder.notesObservable.observeOn(AndroidSchedulers.mainThread()).subscribe {
            textView.text = it.toNoteName()
        }
        scoreView.previewSamplesObservable = recorder.previewSampleObservable
    }

    private fun stopRecord() {
        recorder.stop()
        recording = false
        hasData = true
        player.hummingOption = hummingOption

        button_record.setImageResource(R.drawable.ic_fiber_manual_record)
        button_record.isEnabled = false
        button_record.alpha = 0.5f
        button_play.alpha = 1f
        button_play.isEnabled = true
        button_edit.alpha = 1f
        button_edit.isEnabled = true
    }

    private fun togglePlayer() {
        when {
            // 初期状態
            !player.isPlaying && !player.isPrepared -> {
                player.prepare(
                    path,
                    recorder.notes
                )
                player.onComplete = {
                    button_play.setImageResource(R.drawable.ic_play_arrow)
                }
                player.play()
                button_play.setImageResource(R.drawable.ic_pause)
            }
            // 再生中
            player.isPlaying -> {
                player.pause()
                button_play.setImageResource(R.drawable.ic_play_arrow)
            }
            // 一時停止中
            player.isPrepared -> {
                player.play()
                button_play.setImageResource(R.drawable.ic_pause)
            }
        }
    }

    private fun showMidiMenu() {
        if (midiDevice != null) return

        val midiDeviceInfo = MidiDevice.getDeviceList(this)

        AlertDialog.Builder(this)
            .setTitle("Midiデバイスを選択してください")
            .setItems(midiDeviceInfo.map { "Midi Port ${it.id}" }.toTypedArray()) { _, which ->
                midiDevice = MidiDevice(
                    this,
                    MidiDevice.getDeviceList(this)[which]
                )
                midiPlayer = MidiPlayer(midiDevice!!, player.notesStream)
                button_midi.compoundDrawablesRelative[0].colorFilter =
                    PorterDuffColorFilter(resources.getColor(R.color.colorAccent), PorterDuff.Mode.SRC_IN)

                Toast.makeText(this, "Midi Port に接続しました", Toast.LENGTH_LONG).show()
            }.show()
    }

    private fun showEditDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_edit, null)
        val edit = view.findViewById<EditText>(R.id.name_edit_text)
        edit.setText(recorder.hummingData.name)
        AlertDialog.Builder(this)
            .setView(view)
            .setPositiveButton("保存") { _, _ ->
                recorder.rename(edit.text.toString())
                title = edit.text.toString()
            }
            .show()
    }

    private fun clearNoteButtonColor() {
        val filter = PorterDuffColorFilter(resources.getColor(R.color.gray), PorterDuff.Mode.SRC_IN)
        note_button_4.compoundDrawablesRelative[0].colorFilter = filter
        note_button_8.compoundDrawablesRelative[0].colorFilter = filter
        note_button_16.compoundDrawablesRelative[0].colorFilter = filter
        note_button_32.compoundDrawablesRelative[0].colorFilter = filter
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home)
            onBackPressed()
        return super.onOptionsItemSelected(item)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        onRequestPermissionsResult(requestCode, grantResults)
    }

    override fun onDestroy() {
        super.onDestroy()
        player.pause()
    }
}
