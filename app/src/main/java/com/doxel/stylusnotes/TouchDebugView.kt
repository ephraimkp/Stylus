package com.doxel.stylusnotes

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import java.util.Locale

/**
 * Vue de diagnostic : affiche pour chaque pointeur actif ses valeurs brutes
 * (toolType, size, pressure, touchMajor/Minor) directement sur l'écran, à
 * l'endroit du contact. Sert à vérifier si le digitizer distingue vraiment
 * la paume du stylet, ou s'il renvoie des valeurs constantes/inutilisables.
 */
class TouchDebugView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private data class PointerInfo(
        val x: Float, val y: Float, val toolType: Int,
        val size: Float, val pressure: Float,
        val touchMajor: Float, val touchMinor: Float
    )

    private var pointers: List<PointerInfo> = emptyList()

    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.RED
        style = Paint.Style.FILL
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 32f
    }
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(220, 255, 255, 200)
        style = Paint.Style.FILL
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val list = ArrayList<PointerInfo>()
        for (i in 0 until event.pointerCount) {
            list.add(
                PointerInfo(
                    x = event.getX(i),
                    y = event.getY(i),
                    toolType = event.getToolType(i),
                    size = event.getSize(i),
                    pressure = event.getPressure(i),
                    touchMajor = event.getTouchMajor(i),
                    touchMinor = event.getTouchMinor(i)
                )
            )
        }
        pointers = if (event.actionMasked == MotionEvent.ACTION_UP ||
            event.actionMasked == MotionEvent.ACTION_CANCEL
        ) emptyList() else list
        invalidate()
        return true
    }

    private fun toolName(t: Int) = when (t) {
        MotionEvent.TOOL_TYPE_STYLUS -> "STYLET"
        MotionEvent.TOOL_TYPE_FINGER -> "DOIGT"
        MotionEvent.TOOL_TYPE_ERASER -> "GOMME"
        else -> "INCONNU"
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        pointers.forEach { p ->
            canvas.drawCircle(p.x, p.y, 16f, dotPaint)
            val lines = listOf(
                "outil: ${toolName(p.toolType)}",
                "size: ${fmt(p.size)}",
                "pressure: ${fmt(p.pressure)}",
                "major/minor: ${fmt(p.touchMajor)}/${fmt(p.touchMinor)}"
            )
            val boxW = 320f
            val boxH = lines.size * 38f + 16f
            var bx = p.x + 24f
            var by = p.y - boxH / 2
            if (bx + boxW > width) bx = p.x - 24f - boxW
            if (by < 0) by = 0f
            if (by + boxH > height) by = height - boxH
            canvas.drawRect(bx, by, bx + boxW, by + boxH, bgPaint)
            lines.forEachIndexed { idx, line ->
                canvas.drawText(line, bx + 12f, by + 30f + idx * 38f, textPaint)
            }
        }
    }

    private fun fmt(v: Float) = String.format(Locale.getDefault(), "%.3f", v)
}
