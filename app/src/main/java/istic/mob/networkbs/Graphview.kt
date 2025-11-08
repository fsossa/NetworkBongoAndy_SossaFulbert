package istic.mob.networkbs

import android.app.AlertDialog
import android.content.Context
import android.graphics.*
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.EditText

class Graphview(context: Context, private val graph: Graph) : View(context) {

    private val paintNode = Paint().apply {
        color = Color.CYAN
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val paintEdge = Paint().apply {
        color = Color.BLACK
        strokeWidth = 5f
        isAntiAlias = true
    }

    private val paintText = Paint().apply {
        color = Color.BLACK
        textSize = 30f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }

    private val paintTemp = Paint().apply {
        color = Color.GRAY
        strokeWidth = 3f
        isAntiAlias = true
    }

    // Image du plan d’appartement
    private val backgroundBitmap: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.background_image_plan)
    private var scaledBackground: Bitmap? = null

    private val gestureDetector = GestureDetector(context, GestureListener())

    private var mode: Mode = Mode.VIEW
    private var selectedNode: Object? = null
    private var connectStart: Object? = null
    private var tempX = 0f
    private var tempY = 0f

    private var toolbarHeight = 0
    fun setToolbarHeight(height: Int) {
        toolbarHeight = height
    }

    enum class Mode { VIEW, ADD_NODE, ADD_EDGE, EDIT }

    fun setMode(m: Mode) {
        mode = m
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // Redimensionner l’image du plan à la taille de la vue
        scaledBackground = Bitmap.createScaledBitmap(backgroundBitmap, w, h, true)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Dessiner le plan en fond
        scaledBackground?.let { canvas.drawBitmap(it, 0f, 0f, null) }

        // Dessiner les arêtes
        for (edge in graph.connections) {
            val a = graph.findById(edge.aId)
            val b = graph.findById(edge.bId)
            if (a != null && b != null) {
                canvas.drawLine(a.x, a.y, b.x, b.y, paintEdge)
            }
        }

        // Ligne temporaire pendant ajout de connexion
        if (connectStart != null) {
            canvas.drawLine(connectStart!!.x, connectStart!!.y, tempX, tempY, paintTemp)
        }

        // Dessiner les nœuds
        for (n in graph.objects) {
            canvas.drawOval(n.bounds, paintNode)
            canvas.drawText(n.label, n.x, n.y, paintText)
        }
    }

    private fun findNodeAt(x: Float, y: Float): Object? {
        return graph.objects.lastOrNull { it.bounds.contains(x, y) }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                selectedNode = findNodeAt(event.x, event.y)
                if (mode == Mode.ADD_EDGE && selectedNode != null) {
                    connectStart = selectedNode
                    tempX = event.x
                    tempY = event.y
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (selectedNode != null && mode == Mode.EDIT) {
                    selectedNode!!.moveTo(
                        event.x,
                        event.y,
                        width,
                        height,
                        toolbarHeight.toFloat()
                    )
                    invalidate()
                } else if (connectStart != null) {
                    tempX = event.x
                    tempY = event.y
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP -> {
                if (connectStart != null) {
                    val target = findNodeAt(event.x, event.y)
                    if (target != null && target != connectStart) {
                        graph.addEdge(connectStart, target)
                    }
                    connectStart = null
                    invalidate()
                }
                selectedNode = null
            }
        }
        return true
    }

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onLongPress(e: MotionEvent) {
            if (mode == Mode.ADD_NODE) {
                val input = EditText(context)
                AlertDialog.Builder(context)
                    .setTitle("Nom de l'objet")
                    .setView(input)
                    .setPositiveButton("OK") { _, _ ->
                        val label = input.text.toString()
                        graph.addNode(Object(label, e.x, e.y))
                        invalidate()
                    }
                    .setNegativeButton("Annuler", null)
                    .show()
            }
        }
    }
}
