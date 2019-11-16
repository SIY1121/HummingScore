package space.siy.hummingscore

import android.content.Context
import android.graphics.*
import android.os.Handler
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import io.reactivex.Observable

class ScoreView : FrameLayout {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(
        context,
        attrs,
        defStyleAttr,
        defStyleRes
    )

    val innerView: InnerView
    val scrollView: HorizontalScrollView

    var hummingOption: HummingOption
        set(value) {
            innerView.hummingOption = value
        }
        get() = innerView.hummingOption

    var notesObservable: Observable<Int>?
        set(value) {
            innerView.notesObservable = value
        }
        get() = innerView.notesObservable

    var previewSamplesObservable: Observable<Byte>?
        set(value) {
            innerView.previewSampleObservable = value
        }
        get() = innerView.previewSampleObservable

    var playerPositionObservable: Observable<Int>?
        set(value) {
            innerView.playerPositionObservable = value
        }
        get() = innerView.playerPositionObservable

    init {
        inflate(context, R.layout.view_score, this)
        innerView = findViewById(R.id.score_inner)
        scrollView = findViewById(R.id.score_outer)
        innerView.scrollToRight = {
            Handler(context.mainLooper).post {
                scrollView.fullScroll(HorizontalScrollView.FOCUS_RIGHT)
            }
        }
        innerView.scrollToNextPage = {
            scrollView.pageScroll(HorizontalScrollView.FOCUS_RIGHT)
        }
        innerView.scrollToPrevPage = {
            scrollView.pageScroll(HorizontalScrollView.FOCUS_LEFT)
        }
        innerView.scrollTo = { left ->
            scrollView.smoothScrollTo(left, 0)
        }
    }

    class InnerView : View {
        constructor(context: Context) : super(context)
        constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
        constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
        constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(
            context,
            attrs,
            defStyleAttr,
            defStyleRes
        )

        val notePaint = Paint().apply {
            color = context.resources.getColor(R.color.colorAccent)
        }
        val noteWidth = 30f
        val noteHeight = 20f

        val noteCenter = PointF(0f, 500f)


        val wavePaint = Paint().apply {
            color = context.resources.getColor(R.color.colorPrimary)
        }
        val waveCenter = PointF(0f, 1000f)

        var scrollToRight: (() -> Unit)? = null
        var scrollToNextPage: (() -> Unit)? = null
        var scrollToPrevPage: (() -> Unit)? = null
        var scrollTo: ((left: Int) -> Unit)? = null

        var hummingOption = HummingOption()

        val widthPerSec
            get() = hummingOption.bpm / 60f * (hummingOption.noteResolution / 4) * noteWidth

        val parentWidth: Int
            get() {
                val p = parent.parent
                return if (p is View)
                    p.width
                else 0
            }

        var playerPosition = -1
            set(value) {
                val prevPage = ((field / 1000f * widthPerSec) / parentWidth).toInt()
                val page = ((value / 1000f * widthPerSec) / parentWidth).toInt()

                if (prevPage != page)
                    scrollTo?.invoke(page * parentWidth)

                field = value
                invalidate()
            }

        var notesObservable: Observable<Int>? = null
            set(value) {
                value?.subscribe {
                    notes.add(it)
                    invalidate()
                }
                field = value
            }

        var previewSampleObservable: Observable<Byte>? = null
            set(value) {
                value?.subscribe {
                    previewSamples.add(it)
                    invalidate()
                }
                field = value
            }

        var playerPositionObservable: Observable<Int>? = null
            set(value) {
                value?.subscribe {
                    playerPosition = it
                }
            }

        init {
            layoutParams = LinearLayout.LayoutParams(
                context.resources.displayMetrics.widthPixels,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }

        var notes = MutableList<Int>(0) { _ -> 0 }
        val previewSamples = MutableList<Byte>(0) { _ -> 0 }

        override fun onDraw(canvas: Canvas) {
            canvas.drawColor(Color.TRANSPARENT)
            if (notes.size * noteWidth > width) {
                layoutParams = LinearLayout.LayoutParams(layoutParams).apply {
                    width += parentWidth
                }
                scrollToRight?.invoke()
            }
            notes.forEachIndexed { index, _note ->
                val note = _note - 36
                canvas.drawRect(
                    index * noteWidth,
                    noteCenter.y - note * noteHeight,
                    (index + 1) * noteWidth,
                    noteCenter.y - (note + 1) * noteHeight,
                    notePaint
                )
            }
            previewSamples.forEachIndexed { index, byte ->
                val level = byte / 256f * 25f
                canvas.drawRect(
                    index * (noteWidth / hummingOption.previewWaveSampleRate),
                    waveCenter.y - level * noteHeight,
                    (index + 1) * (noteWidth / hummingOption.previewWaveSampleRate) - (noteWidth / hummingOption.previewWaveSampleRate / 2),
                    waveCenter.y + level * noteHeight,
                    wavePaint
                )
            }
            canvas.drawRect(
                playerPosition / 1000f * widthPerSec,
                0f,
                playerPosition / 1000f * widthPerSec + 10
                , 1000f,
                wavePaint
            )
        }

        fun Float.toDp() = this / context.resources.displayMetrics.density

    }
}
