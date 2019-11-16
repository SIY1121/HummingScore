package space.siy.hummingscore

import io.reactivex.Observable

class MidiPlayer(val device: MidiDevice, val stream: Observable<Int>) {
    var prev = -1

    init {
        stream.subscribe {
            if (prev != it) {
                device.noteOff(prev)
                device.noteOn(it)
            }
            prev = it
        }
    }
}
