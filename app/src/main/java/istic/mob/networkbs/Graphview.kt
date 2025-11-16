package istic.mob.networkbs

import android.app.AlertDialog
import android.content.Context
import android.graphics.*
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.EditText
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign

/**
 * Graphview : vue personnalisée qui gère
 * - affichage du plan (background)
 * - dessin des nodes et des connections (courbes)
 * - ajout / déplacement / édition (long-press)
 * - pan / zoom
 * - modification de la courbure en touchant le milieu réel de l'arc (PathMeasure)
 *
 * S'appuie sur :
 * - Graph (model)
 * - Node (model)
 * - Connection (model) -- doit contenir controlOffset, midX, midY, color, strokeWidth, label
 */
class Graphview(context: Context, private var graph: Graph) : View(context) {

    // ---- Paints ----
    private val paintNode = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val paintNodeStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = Color.BLACK
    }
    private val paintEdge = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 5f
        color = Color.BLACK
    }
    private val paintText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 28f
        textAlign = Paint.Align.CENTER
    }
    private val paintTemp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = Color.GRAY
        pathEffect = DashPathEffect(floatArrayOf(12f, 8f), 0f)
    }

    // ---- Background plan ----
//    private var backgroundBitmap: Bitmap? = null
//    private var scaledBackground: Bitmap? = null
//    fun setBackgroundBitmap(bmp: Bitmap?) {
//        backgroundBitmap = bmp
//        // scaled when view size known
//        if (width > 0 && height > 0 && bmp != null) {
//            scaledBackground = Bitmap.createScaledBitmap(bmp, bmp.width, bmp.height, true)
//            // We do not auto-scale to view; we keep image "natural size" and allow panning/zooming.
//            // Optionally you can scale to fit by using createScaledBitmap(bmp, width, height, true)
//        } else scaledBackground = null
//        invalidate()
//    }

    // ---- Modes ----
    enum class Mode { VIEW, ADD_NODE, ADD_EDGE, EDIT, EDIT_CURVE }
    private var mode = Mode.VIEW
    fun setMode(m: Mode) { mode = m }

    // ---- Gesture detectors ----
    private val gestureDetector = GestureDetector(context, GestureListener())
    private val scaleDetector = ScaleGestureDetector(context, ScaleListener())

    // ---- Interaction state ----
    private var selectedNode: Node? = null
    private var connectStart: Node? = null
    private var tempX = 0f
    private var tempY = 0f

    private var selectedConnection: Connection? = null
    private var draggingCurve = false

    // Pan / zoom
    private var offsetX = 0f
    private var offsetY = 0f
    private var scaleFactor = 1f
    private val minScale = 0.5f
    private val maxScale = 3f

    // last touch for panning
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var isPanning = false

    // toolbar height to restrict nodes upward
    private var toolbarHeightPx = 0
    fun setToolbarHeight(h: Int) { toolbarHeightPx = h }

    // ---- Helpers: convert screen <-> local graph coordinates ----
    private fun toLocalX(screenX: Float) = (screenX - offsetX) / scaleFactor
    private fun toLocalY(screenY: Float) = (screenY - offsetY) / scaleFactor
    private fun toScreenX(localX: Float) = localX * scaleFactor + offsetX
    private fun toScreenY(localY: Float) = localY * scaleFactor + offsetY

    // ----------- FOND D'ÉCRAN DU PLAN ---------
    private var backgroundBitmap: Bitmap =
        BitmapFactory.decodeResource(resources, R.drawable.plan1)

    private var scaledBackground: Bitmap? = null

    fun setBackgroundImage(resId: Int) {
        backgroundBitmap = BitmapFactory.decodeResource(resources, resId)
        scaledBackground = Bitmap.createScaledBitmap(backgroundBitmap, width, height, true)
        invalidate()
    }
    // ---- View size changed ----
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // scale background to its natural size or keep as is. If you want fit-to-view:
        backgroundBitmap?.let {
            // keep original natural size, but create a scaled copy with same density if needed:
            scaledBackground = it // keep reference; drawing uses transforms
        }
    }

    // ---- Drawing ----
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.save()
        // apply pan/zoom
        canvas.translate(offsetX, offsetY)
        canvas.scale(scaleFactor, scaleFactor)

        // draw background at its (0,0) origin (natural size)
        scaledBackground?.let { bmp ->
            canvas.drawBitmap(bmp, 0f, 0f, null)
        }

        // draw connections (curved paths)
        for (conn in graph.connections) {
            val a = graph.findById(conn.aId)
            val b = graph.findById(conn.bId)
            if (a == null || b == null) continue

            paintEdge.color = conn.color
            paintEdge.strokeWidth = conn.strokeWidth

            // mid of AB
            val midX = (a.x + b.x) / 2f
            val midY = (a.y + b.y) / 2f

            // perp unit vector to AB
            val dx = b.x - a.x
            val dy = b.y - a.y
            val dist = hypot(dx, dy).coerceAtLeast(0.0001f)
            val ux = -dy / dist
            val uy = dx / dist

            val controlX = midX + ux * conn.controlOffset
            val controlY = midY + uy * conn.controlOffset

            // build path
            val path = Path().apply {
                moveTo(a.x, a.y)
                quadTo(controlX, controlY, b.x, b.y)
            }

            // draw path
            canvas.drawPath(path, paintEdge)

            // compute true midpoint on the path and store it in conn.midX/midY
            try {
                val pm = PathMeasure(path, false)
                val pos = FloatArray(2)
                pm.getPosTan(pm.length / 2f, pos, null)
                conn.midX = pos[0]
                conn.midY = pos[1]
            } catch (t: Throwable) {
                // fallback to straight mid if PathMeasure fails
                conn.midX = midX
                conn.midY = midY
            }

            // draw label at midpoint of curve (offset a bit perpendicular)
            if (conn.label.isNotEmpty()) {
                // draw using computed mid point
                canvas.drawText(conn.label, conn.midX, conn.midY - 10f, paintText)
            }
        }

        // draw temporary edge (during add-edge)
        if (connectStart != null) {
            paintTemp.strokeWidth = 3f
            val startX = connectStart!!.x
            val startY = connectStart!!.y
            val endX = toLocalX(tempX)
            val endY = toLocalY(tempY)
            canvas.drawLine(startX, startY, endX, endY, paintTemp)
        }

        // draw nodes
        for (n in graph.nodes) {
            paintNode.color = n.color
            val r = Node.R
            canvas.drawOval(RectF(n.x - r, n.y - r, n.x + r, n.y + r), paintNode)
            canvas.drawOval(RectF(n.x - r, n.y - r, n.x + r, n.y + r), paintNodeStroke)
            canvas.drawText(n.label, n.x, n.y - Node.R - 8f, paintText)
        }

        canvas.restore()
    }

    // ---- Hit testing ----
    private fun findNodeAt(screenX: Float, screenY: Float): Node? {
        val lx = toLocalX(screenX)
        val ly = toLocalY(screenY)
        return graph.nodes.lastOrNull { it.bounds.contains(lx, ly) }
    }

    private fun findConnectionAtScreen(screenX: Float, screenY: Float, thresholdPx: Float = 40f): Connection? {
        val lx = toLocalX(screenX)
        val ly = toLocalY(screenY)
        // First try to use stored mid point (best UX)
        for (conn in graph.connections.reversed()) {
            // if mid point exists and within threshold (in local coords)
            val mx = conn.midX
            val my = conn.midY
            val dx = lx - mx
            val dy = ly - my
            if (hypot(dx, dy) <= thresholdPx / scaleFactor) return conn
        }
        // fallback: search by distance to straight-line
        for (conn in graph.connections.reversed()) {
            val a = graph.findById(conn.aId) ?: continue
            val b = graph.findById(conn.bId) ?: continue
            val dx = b.x - a.x
            val dy = b.y - a.y
            val len = hypot(dx, dy)
            if (len < 0.1f) continue
            val distance = kotlin.math.abs(dy * lx - dx * ly + b.x * a.y - b.y * a.x) / len
            if (distance <= thresholdPx / scaleFactor) {
                // only accept if near the segment middle roughly
                val midX = (a.x + b.x) / 2f
                val midY = (a.y + b.y) / 2f
                if (hypot((lx - midX), (ly - midY)) <= 150f / scaleFactor) return conn
            }
        }
        return null
    }

    // ---- Touch handling ----
    override fun onTouchEvent(event: MotionEvent): Boolean {
        // give detectors first
        scaleDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)

        // if scaling, ignore other gestures in this implementation
        if (scaleDetector.isInProgress) return true

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.x
                lastTouchY = event.y
                isPanning = false

                val node = findNodeAt(event.x, event.y)

                if (mode == Mode.ADD_NODE) {
                    // do nothing here; long-press will create node
                } else if (mode == Mode.ADD_EDGE && node != null) {
                    connectStart = node
                    tempX = event.x
                    tempY = event.y
                } else if (mode == Mode.EDIT) {
                    // choose node or connection or start panning
                    selectedNode = node
                    if (selectedNode == null) {
                        // test if touch is near a connection midpoint (local coords)
                        val conn = findConnectionAtScreen(event.x, event.y)
                        if (conn != null) {
                            // we will drag the curve
                            selectedConnection = conn
                            draggingCurve = true
                            mode = Mode.EDIT_CURVE
                        } else {
                            // start panning
                            isPanning = true
                        }
                    }
                } else { // VIEW mode
                    selectedNode = node
                    if (selectedNode == null) isPanning = true
                }
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - lastTouchX
                val dy = event.y - lastTouchY

                if (draggingCurve && selectedConnection != null && mode == Mode.EDIT_CURVE) {
                    // compute controlOffset from finger position relative to AB
                    val conn = selectedConnection!!
                    val a = graph.findById(conn.aId)
                    val b = graph.findById(conn.bId)
                    if (a != null && b != null) {
                        val lx = toLocalX(event.x)
                        val ly = toLocalY(event.y)

                        // projection and signed perpendicular distance
                        val vx = b.x - a.x
                        val vy = b.y - a.y
                        val len2 = vx*vx + vy*vy
                        if (len2 < 25f) {
                            conn.controlOffset = 0f
                        } else {
                            val t = ((lx - a.x)*vx + (ly - a.y)*vy) / len2
                            val projx = a.x + t*vx
                            val projy = a.y + t*vy
                            val cross = vx*(ly - a.y) - vy*(lx - a.x)
                            val sign = sign(cross)
                            val dist = hypot(lx - projx, ly - projy)
                            conn.controlOffset = sign * dist
                        }
                        invalidate()
                    }
                } else if (selectedNode != null && (mode == Mode.EDIT || mode == Mode.VIEW)) {
                    // move node: use Node.moveTo which expects screen coords + offset + scale
                    selectedNode!!.moveTo(
                        event.x,
                        event.y,
                        width,
                        height,
                        toolbarHeightPx.toFloat(),
                        offsetX,
                        offsetY,
                        scaleFactor
                    )
                    invalidate()
                } else if (connectStart != null) {
                    tempX = event.x
                    tempY = event.y
                    invalidate()
                } else if (isPanning) {
                    offsetX += dx
                    offsetY += dy
                    invalidate()
                }

                lastTouchX = event.x
                lastTouchY = event.y
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (connectStart != null) {
                    val target = findNodeAt(event.x, event.y)
                    if (target != null && target != connectStart) {
                        // ask for label
                        showConnectionLabelDialog(connectStart!!, target)
                    }
                    connectStart = null
                    invalidate()
                }

                if (draggingCurve) {
                    draggingCurve = false
                    selectedConnection = null
                    mode = Mode.EDIT
                }

                selectedNode = null
                isPanning = false
            }
        }

        return true
    }

    // ---- Scale (pinch) ----
    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val prevScale = scaleFactor
            scaleFactor *= detector.scaleFactor
            scaleFactor = scaleFactor.coerceIn(minScale, maxScale)

            // keep focus consistent
            val focusX = detector.focusX
            val focusY = detector.focusY
            val focusLocalX = (focusX - offsetX) / prevScale
            val focusLocalY = (focusY - offsetY) / prevScale

            offsetX = focusX - focusLocalX * scaleFactor
            offsetY = focusY - focusLocalY * scaleFactor

            invalidate()
            return true
        }
    }

    // ---- Long press gesture, menus ----
    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onLongPress(e: MotionEvent) {
            val node = findNodeAt(e.x, e.y)
            val conn = findConnectionAtScreen(e.x, e.y)

            if (mode == Mode.ADD_NODE && node == null && conn == null) {
                // create node at local coords
                val lx = toLocalX(e.x)
                val ly = toLocalY(e.y)
                val input = EditText(context)
                AlertDialog.Builder(context)
                    .setTitle("Nom de l'objet")
                    .setView(input)
                    .setPositiveButton("OK") { _, _ ->
                        val label = input.text.toString().ifBlank { "Obj${(1..999).random()}" }
                        val n = Node(label, lx, ly)
                        graph.addNode(n)
                        invalidate()
                    }
                    .setNegativeButton("Annuler", null)
                    .show()
                return
            }

            if (node != null) {
                showNodeOptions(node)
                return
            }

            if (conn != null) {
                showConnectionOptions(conn)
                return
            }
        }
    }

    // ---- Dialogs for nodes/connections ----
    private fun showNodeOptions(node: Node) {
        val options = arrayOf("Modifier le nom", "Changer la couleur", "Supprimer")
        AlertDialog.Builder(context)
            .setTitle("Objet : ${node.label}")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> editNodeLabel(node)
                    1 -> changeNodeColor(node)
                    2 -> { graph.removeNode(node); invalidate() }
                }
            }.show()
    }

    private fun editNodeLabel(node: Node) {
        val input = EditText(context)
        input.setText(node.label)
        AlertDialog.Builder(context)
            .setTitle("Modifier le nom")
            .setView(input)
            .setPositiveButton("OK") { _, _ ->
                node.label = input.text.toString()
                invalidate()
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun changeNodeColor(node: Node) {
        val items = arrayOf("Rouge","Vert","Bleu","Orange","Cyan","Magenta","Noir")
        val vals = intArrayOf(Color.RED, Color.GREEN, Color.BLUE, 0xFFFFA500.toInt(), Color.CYAN, Color.MAGENTA, Color.BLACK)
        AlertDialog.Builder(context)
            .setTitle("Choisir une couleur")
            .setItems(items) { _, which ->
                node.color = vals[which]
                invalidate()
            }.show()
    }

    private fun showConnectionOptions(conn: Connection) {
        val options = arrayOf("Modifier étiquette", "Changer couleur", "Modifier épaisseur", "Supprimer")
        AlertDialog.Builder(context)
            .setTitle("Connexion")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> editConnectionLabel(conn)
                    1 -> changeConnectionColor(conn)
                    2 -> changeConnectionThickness(conn)
                    3 -> { graph.connections.remove(conn); invalidate() }
                }
            }.show()
    }

    private fun editConnectionLabel(conn: Connection) {
        val input = EditText(context)
        input.setText(conn.label)
        AlertDialog.Builder(context)
            .setTitle("Modifier l'étiquette")
            .setView(input)
            .setPositiveButton("OK") { _, _ -> conn.label = input.text.toString(); invalidate() }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun changeConnectionColor(conn: Connection) {
        val items = arrayOf("Rouge","Vert","Bleu","Orange","Cyan","Magenta","Noir")
        val vals = intArrayOf(Color.RED, Color.GREEN, Color.BLUE, 0xFFFFA500.toInt(), Color.CYAN, Color.MAGENTA, Color.BLACK)
        AlertDialog.Builder(context)
            .setTitle("Couleur")
            .setItems(items) { _, which -> conn.color = vals[which]; invalidate() }
            .show()
    }

    private fun changeConnectionThickness(conn: Connection) {
        val items = arrayOf("Fine","Moyenne","Épaisse")
        val vals = floatArrayOf(3f, 6f, 10f)
        AlertDialog.Builder(context)
            .setTitle("Épaisseur")
            .setItems(items) { _, which -> conn.strokeWidth = vals[which]; invalidate() }
            .show()
    }

    private fun showConnectionLabelDialog(a: Node, b: Node) {
        val input = EditText(context)
        AlertDialog.Builder(context)
            .setTitle("Étiquette de la connexion")
            .setView(input)
            .setPositiveButton("OK") { _, _ ->
                val label = input.text.toString()
                graph.addEdge(a, b)
                // set label on the newly created connection
                // find connection by ids
                val newConn = graph.connections.find { (it.aId == minOf(a.id, b.id) && it.bId == maxOf(a.id, b.id)) || (it.aId == maxOf(a.id,b.id) && it.bId == minOf(a.id,b.id)) }
                newConn?.label = label
                invalidate()
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    fun setGraph(newGraph: Graph) {
        this.graph = newGraph

        // Réinitialiser le mode
        this.mode = Mode.VIEW

        // Annuler les sélections temporaires
        selectedNode = null
        selectedConnection = null
        connectStart = null

        // Facultatif : remettre zoom + pan à zéro
        offsetX = 0f
        offsetY = 0f
        scaleFactor = 1f

        invalidate()
    }

}
