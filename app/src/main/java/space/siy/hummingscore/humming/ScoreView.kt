package space.siy.hummingscore.humming

import android.content.Context
import android.graphics.*
import android.os.Handler
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import io.reactivex.Observable
import space.siy.hummingscore.R

/**
 * 楽譜を表示するカスタムビュー
 */
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

    /** 表示部 */
    val innerView: InnerView

    /** 表示部をラップするScrollView */
    val scrollView: HorizontalScrollView

    /** テンポや解像度のオプション */
    var hummingOption: HummingOption
        set(value) {
            innerView.hummingOption = value
        }
        get() = innerView.hummingOption

    /** notes情報として監視するストリーム */
    var notesObservable: Observable<Int>?
        set(value) {
            innerView.notesObservable = value
        }
        get() = innerView.notesObservable

    /**  波形プレビューとして監視するストリーム */
    var previewSamplesObservable: Observable<Byte>?
        set(value) {
            innerView.previewSampleObservable = value
        }
        get() = innerView.previewSampleObservable

    /**  再生位置として監視するストリーム */
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

        private val notePaint = Paint().apply {
            color = context.resources.getColor(R.color.colorAccent)
        }
        private val noteWidth = 30f
        private val noteHeight = 20f
        private val noteCenter = PointF(0f, 500f)

        private val wavePaint = Paint().apply {
            color = context.resources.getColor(R.color.colorPrimary)
        }
        private val waveCenter = PointF(0f, 1000f)

        private val playerPositionPaint = Paint().apply {
            color = context.resources.getColor(R.color.colorPrimaryDark)
        }
        private val keyboardPaint = Paint()

        var scrollToRight: (() -> Unit)? = null
        var scrollToNextPage: (() -> Unit)? = null
        var scrollToPrevPage: (() -> Unit)? = null
        var scrollTo: ((left: Int) -> Unit)? = null

        var hummingOption = HummingOption()

        private val widthPerSec
            get() = hummingOption.bpm / 60f * (hummingOption.noteResolution / 4) * noteWidth

        private val parentWidth: Int
            get() {
                val p = parent.parent
                return if (p is View)
                    p.width
                else 0
            }

        private var playerPosition = -100000
            set(value) {
                val prevPage = ((field / 1000f * widthPerSec) / parentWidth).toInt()
                val page = ((value / 1000f * widthPerSec) / parentWidth).toInt()

                if (prevPage != page)
                    scrollTo?.invoke(page * parentWidth)

                field = value
                invalidate()
            }

        // TODO サブスクリプションの管理
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

        private var notes = MutableList<Int>(0) { _ -> 0 }
        private val previewSamples = MutableList<Byte>(0) { _ -> 0 }

        private val keyboardColor =
            arrayOf(false, true, false, true, false, false, true, false, true, false, true, false)

        /**  描画部分 */
        override fun onDraw(canvas: Canvas) {
            canvas.drawColor(Color.TRANSPARENT)

            // ページを増やす
            if (notes.size * noteWidth > width) {
                layoutParams = LinearLayout.LayoutParams(layoutParams).apply {
                    width += parentWidth
                }
                scrollToRight?.invoke()
            }

            // 音程バーの描画
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

            // 波形の描画
            previewSamples.forEachIndexed { index, byte ->
                val level = byte / 256f * 15f
                canvas.drawRect(
                    index * (noteWidth / hummingOption.previewWaveSampleRate),
                    waveCenter.y - level * noteHeight,
                    (index + 1) * (noteWidth / hummingOption.previewWaveSampleRate) - (noteWidth / hummingOption.previewWaveSampleRate / 2),
                    waveCenter.y + level * noteHeight,
                    wavePaint
                )
            }

            // キーボード
            (25..60).forEach { _i ->
                keyboardPaint.color = if (keyboardColor[(_i - 3) % 12]) Color.parseColor("#444444") else Color.parseColor("#EEEEEE")
                val i = _i - 36
                canvas.drawRect(
                    0f,
                    noteCenter.y - i * noteHeight,
                    100f,
                    noteCenter.y - (i + 1) * noteHeight,
                    keyboardPaint
                )
            }

            // 再生位置の描画
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
