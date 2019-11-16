package space.siy.hummingscore

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import java.util.*

class ScoreView : View {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(
        context,
        attrs,
        defStyleAttr,
        defStyleRes
    )

    val paint = Paint()
    val noteWidth = 30f
    val noteHeight = 20f

    val center = PointF(0f, 200f)

    init {
        paint.color = Color.RED
        paint.style = Paint.Style.FILL
    }

    var notes = MutableList<Int>(0) { _ -> 0 }

    fun addAndDraw(note: Int) {
        notes.add(note)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(Color.TRANSPARENT)
//        if((notes.size * noteWidth).toDp() > context.resources.displayMetrics.widthPixels)

        notes.forEachIndexed { index, _note ->
            val note = _note - 36
            canvas.drawRect(
                index * noteWidth,
                center.y - note * noteHeight,
                (index + 1) * noteWidth,
                center.y - (note + 1) * noteHeight,
                paint
            )
        }
    }

    fun Float.toDp() = this / context.resources.displayMetrics.density

}
