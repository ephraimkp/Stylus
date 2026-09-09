package com.doxel.stylusnotes

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

/**
 * Vue de dessin avec rejet de paume LOGICIEL.
 *
 * Contexte : un stylet capacitif passif (non-MPP, non-Wacom) n'est PAS reconnu par
 * Android comme TOOL_TYPE_STYLUS : il génère les mêmes événements qu'un doigt.
 * Le système ne peut donc pas distinguer "pointe du stylet" de "paume posée".
 *
 * Stratégie (3 heuristiques combinables) :
 *
 * 1. TAILLE DE CONTACT : la paume touche l'écran sur une surface bien plus large
 *    que la pointe fine du stylet -> on ignore tout pointeur dont getSize() dépasse
 *    un seuil réglable.
 *
 * 2. PREMIER TRACÉ GAGNANT : dès qu'un tracé "valide" (petit, en cours de mouvement)
 *    est actif, tout nouveau pointeur qui apparaît est ignoré tant que le premier
 *    n'est pas relâché. Ça bloque la paume qui se pose PENDANT l'écriture.
 *
 * 3. TOOL_TYPE réel si dispo : si un jour le stylet (ou un autre) est bien détecté
 *    comme TOOL_TYPE_STYLUS, on privilégie ce signal fiable et on ignore les
 *    TOOL_TYPE_FINGER simultanés — le meilleur des cas.
 */
class DrawingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    // ---- Réglages exposés (persistables via SharedPreferences si besoin) ----
    var maxContactSize: Float = 0.12f      // seuil taille normalisée (0..1) au-delà duquel on rejette
    var strokeWidth: Float = 6f
    var strokeColor: Int = Color.BLACK
    var restrictToWritingZone: Boolean = false
    var writingZoneTopRatio: Float = 0f    // 0f = toute la vue ; ex. 0.15f = ignore les 15% du haut

    private var bitmap: Bitmap? = null
    private var bitmapCanvas: Canvas? = null

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }

    private val currentPath = Path()
    private var activePointerId = -1  // le pointeur actuellement autorisé à dessiner (-1 = aucun)
    private val undoStack = ArrayList<Bitmap>()

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0) {
            val old = bitmap
            bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            bitmapCanvas = Canvas(bitmap!!)
            bitmapCanvas!!.drawColor(Color.WHITE)
            old?.let { bitmapCanvas!!.drawBitmap(it, 0f, 0f, null) }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        bitmap?.let { canvas.drawBitmap(it, 0f, 0f, null) }
        paint.color = strokeColor
        paint.strokeWidth = strokeWidth
        canvas.drawPath(currentPath, paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val action = event.actionMasked
        val pointerIndex = event.actionIndex
        val pointerId = event.getPointerId(pointerIndex)

        when (action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                if (activePointerId != -1) {
                    // Un tracé est déjà en cours -> ce nouveau doigt/paume est ignoré (règle 2)
                    return true
                }
                if (!isAcceptablePointer(event, pointerIndex)) {
                    return true
                }
                activePointerId = pointerId
                val idx = event.findPointerIndex(pointerId)
                currentPath.reset()
                currentPath.moveTo(event.getX(idx), event.getY(idx))
                invalidate()
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (activePointerId == -1) return true
                val idx = event.findPointerIndex(activePointerId)
                if (idx == -1) return true
                // On revérifie la taille en continu : si le contact "grossit" (la main
                // s'affaisse sur l'écran), on coupe le tracé au lieu de dessiner un gribouillis.
                if (!isAcceptablePointer(event, idx)) {
                    endStroke()
                    return true
                }
                currentPath.lineTo(event.getX(idx), event.getY(idx))
                invalidate()
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                if (pointerId == activePointerId) {
                    endStroke()
                }
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                currentPath.reset()
                activePointerId = -1
                invalidate()
                return true
            }
        }
        return false
    }

    private fun isAcceptablePointer(event: MotionEvent, index: Int): Boolean {
        // Priorité au signal fiable si le device le fournit un jour
        val toolType = event.getToolType(index)
        if (toolType == MotionEvent.TOOL_TYPE_STYLUS || toolType == MotionEvent.TOOL_TYPE_ERASER) {
            return true
        }
        if (toolType == MotionEvent.TOOL_TYPE_FINGER || toolType == MotionEvent.TOOL_TYPE_UNKNOWN) {
            // Heuristique 1 : taille de contact (0..1, normalisée par l'écran)
            val size = event.getSize(index)
            if (size > maxContactSize) return false
        }
        // Heuristique 3 : zone d'écriture restreinte
        if (restrictToWritingZone) {
            val y = event.getY(index)
            if (y < height * writingZoneTopRatio) return false
        }
        return true
    }

    private fun endStroke() {
        bitmapCanvas?.let {
            saveUndoState()
            paint.color = strokeColor
            paint.strokeWidth = strokeWidth
            it.drawPath(currentPath, paint)
        }
        currentPath.reset()
        activePointerId = -1
        invalidate()
    }

    private fun saveUndoState() {
        bitmap?.let {
            undoStack.add(it.copy(Bitmap.Config.ARGB_8888, false))
            if (undoStack.size > 20) undoStack.removeAt(0)
        }
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        bitmap = undoStack.removeAt(undoStack.size - 1)
        bitmapCanvas = Canvas(bitmap!!)
        invalidate()
    }

    fun clear() {
        saveUndoState()
        bitmapCanvas?.drawColor(Color.WHITE)
        invalidate()
    }

    fun exportBitmap(): Bitmap? = bitmap?.copy(Bitmap.Config.ARGB_8888, false)

    fun loadBitmap(source: Bitmap) {
        bitmap = source.copy(Bitmap.Config.ARGB_8888, true)
        bitmapCanvas = Canvas(bitmap!!)
        invalidate()
    }
}
