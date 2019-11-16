package space.siy.hummingscore

import android.content.Context
import android.media.midi.MidiDevice
import android.media.midi.MidiDeviceInfo
import android.media.midi.MidiInputPort
import android.media.midi.MidiManager
import android.os.Handler

class MidiDevice(val context: Context, val info: MidiDeviceInfo) {
    companion object {
        fun getDeviceList(context: Context) = (context.getSystemService(Context.MIDI_SERVICE) as MidiManager).devices
    }

    private val manager = context.getSystemService(Context.MIDI_SERVICE) as MidiManager
    lateinit var midiDevice: MidiDevice
    lateinit var inputPort: MidiInputPort

    init {
        manager.openDevice(info, {
            midiDevice = it
            inputPort = it.openInputPort(0)
        }, Handler())
    }

    fun noteOn(tone: Int) {
        val channel = 1
        val velocity = 127.toByte()
        val buf = byteArrayOf((0x90 + channel - 1).toByte(), (tone + 21).toByte(), velocity)
        inputPort.send(buf, 0, buf.size)
    }

    fun noteOff(tone: Int) {
        val channel = 1
        val velocity = 127.toByte()
        val buf = byteArrayOf((0x80 + channel - 1).toByte(), (tone + 21).toByte(), velocity)
        inputPort.send(buf, 0, buf.size)
    }
}
