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
    private val backgroundBitmap: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.plan1)
    private var scaledBackground: Bitmap? = null

    private val gestureDetector = GestureDetector(context, GestureListener())

    private var mode: Mode = Mode.VIEW
    private var selectedNode: Node? = null
    private var connectStart: Node? = null
    private var tempX = 0f
    private var tempY = 0f

    private var toolbarHeight = 0
    fun setToolbarHeight(height: Int) {
        toolbarHeight = height
    }

    enum class Mode { VIEW, ADD_NODE, ADD_EDGE, EDIT }
    private var selectedEdge: Connection? = null


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
                paintEdge.color = edge.color
                paintEdge.strokeWidth = edge.strokeWidth
                canvas.drawLine(a.x, a.y, b.x, b.y, paintEdge)
                // Dessiner l’étiquette au milieu
//                if (edge.label.isNotEmpty()) {
//                    val midX = (a.x + b.x) / 2
//                    val midY = (a.y + b.y) / 2
//                    canvas.drawText(edge.label, midX, midY - 10, paintText)
//                }
                paintEdge.color = edge.color
                paintEdge.strokeWidth = edge.strokeWidth

                // Calcul des points de contrôle pour la courbure
                val midX = (a.x + b.x) / 2
                val midY = (a.y + b.y) / 2
                val dx = b.x - a.x
                val dy = b.y - a.y

                // Normaliser le vecteur pour perpendiculaire
                val length = Math.hypot(dx.toDouble(), dy.toDouble()).toFloat()
                val nx = -dy / length
                val ny = dx / length

                val controlX = midX + nx * edge.controlOffset
                val controlY = midY + ny * edge.controlOffset

                val path = android.graphics.Path().apply {
                    moveTo(a.x, a.y)
                    quadTo(controlX, controlY, b.x, b.y)
                }

                canvas.drawPath(path, paintEdge)

                // Dessiner le label au niveau du milieu de la courbe
                if (edge.label.isNotEmpty()) {
                    canvas.drawText(edge.label, midX, midY - 10, paintText)
                }

            }
        }

        // Ligne temporaire pendant ajout de connexion
        if (connectStart != null) {
            canvas.drawLine(connectStart!!.x, connectStart!!.y, tempX, tempY, paintTemp)
        }

        // Dessin des connexions courbées, position centrale et modification de la courbure
        for (n in graph.nodes) {
            canvas.drawOval(n.bounds, paintNode)
            canvas.drawText(n.label, n.x, n.y, paintText)
        }

        // dans GraphView.onDraw(canvas)
//        for (conn in graph.connections) {
//            val a = graph.findNodeById(conn.aId) ?: continue
//            val b = graph.findNodeById(conn.bId) ?: continue
//
//            // points A et B
//            val x1 = a.x; val y1 = a.y
//            val x2 = b.x; val y2 = b.y
//
//            // milieu de la ligne droite
//            val mx = (x1 + x2) / 2f
//            val my = (y1 + y2) / 2f
//
//            // vecteur perpendiculaire unitaire à la ligne AB
//            val dx = x2 - x1
//            val dy = y2 - y1
//            val dist = Math.hypot(dx.toDouble(), dy.toDouble()).toFloat()
//            val ux = -dy / (dist.coerceAtLeast(0.0001f))
//            val uy = dx / (dist.coerceAtLeast(0.0001f))
//
//            // point de contrôle pour quadTo = milieu + (ux,uy) * curvature
//            val cx = mx + ux * conn.curvature
//            val cy = my + uy * conn.curvature
//
//            // créer path
//            val path = Path()
//            path.moveTo(x1, y1)
//            path.quadTo(cx, cy, x2, y2)
//
//            // paint selon couleur / épaisseur
//            paintEdge.color = conn.color
//            paintEdge.strokeWidth = conn.thickness
//            paintEdge.style = Paint.Style.STROKE
//            canvas.drawPath(path, paintEdge)
//
//            // mesurer et dessiner l'étiquette près du milieu
//            val pm = PathMeasure(path, false)
//            val len = pm.length
//            val pos = FloatArray(2)
//            pm.getPosTan(len / 2, pos, null)
//            // décaler l'étiquette légèrement sur le côté (ux, uy) * 12
//            canvas.drawText(conn.label, pos[0] + ux*12f, pos[1] + uy*12f, paintText)
//
//            // stocker path / pos pour détection touch (ex: map conn->path)
//        }

    }

    private fun findNodeAt(x: Float, y: Float): Node? {
        return graph.nodes.lastOrNull { it.bounds.contains(x, y) }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (mode == Mode.EDIT && selectedNode == null) {
                    val edge = findConnectionAt(event.x, event.y)
                    if (edge != null) {
                        selectedEdge = edge
                    }
                }
                selectedNode = findNodeAt(event.x, event.y)
                if (mode == Mode.ADD_EDGE && selectedNode != null) {
                    connectStart = selectedNode
                    tempX = event.x
                    tempY = event.y
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (selectedNode != null && mode == Mode.EDIT) {
                    val edge = selectedEdge!!
                    val a = graph.findById(edge.aId)
                    val b = graph.findById(edge.bId)
                    if (a != null && b != null) {
                        val midX = (a.x + b.x) / 2
                        val midY = (a.y + b.y) / 2
                        val dx = b.x - a.x
                        val dy = b.y - a.y
                        val length = Math.hypot(dx.toDouble(), dy.toDouble()).toFloat()
                        val nx = -dy / length
                        val ny = dx / length
                        // Calcul du décalage selon la position du doigt
                        edge.controlOffset = ((event.x - midX) * nx + (event.y - midY) * ny)
                        invalidate()
                    }
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
                selectedEdge = null
            }
        }
        return true
    }

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onLongPress(e: MotionEvent) {
            val node = findNodeAt(e.x, e.y)
            val edge = findConnectionAt(e.x, e.y)

            if (mode == Mode.ADD_NODE && node == null && edge == null) {
                // Ajouter un nouvel objet
                val input = EditText(context)
                AlertDialog.Builder(context)
                    .setTitle("Nom de l'objet")
                    .setView(input)
                    .setPositiveButton("OK") { _, _ ->
                        val label = input.text.toString()
                        graph.addNode(Node(label, e.x, e.y))
                        invalidate()
                    }
                    .setNegativeButton("Annuler", null)
                    .show()
            } else if (node != null) {
                showNodeOptions(node)
            } else if (edge != null) {
                showConnectionOptions(edge)
            }
        }
    }

    private fun showNodeOptions(node: Node) {
        val options = arrayOf("Modifier le nom", "Changer la couleur", "Supprimer")
        AlertDialog.Builder(context)
            .setTitle("Objet : ${node.label}")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> editNodeLabel(node)
                    1 -> changeNodeColor(node)
                    2 -> {
                        graph.removeNode(node)
                        invalidate()
                    }
                }
            }
            .show()
    }

    private fun showConnectionOptions(edge: Connection) {
        val options = arrayOf("Modifier l'étiquette", "Changer la couleur", "Modifier l'épaisseur", "Supprimer")
        AlertDialog.Builder(context)
            .setTitle("Connexion entre ${edge.aId} et ${edge.bId}")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> editConnectionLabel(edge)
                    1 -> changeConnectionColor(edge)
                    2 -> changeConnectionThickness(edge)
                    3 -> {
                        graph.connections.remove(edge)
                        invalidate()
                    }
                }
            }
            .show()
    }


    fun setBackgroundImage(resId: Int) {
        val bitmap = BitmapFactory.decodeResource(resources, resId)
        scaledBackground = Bitmap.createScaledBitmap(bitmap, width, height, true)
        invalidate()
    }

    private fun findConnectionAt(x: Float, y: Float): Connection? {
        for (edge in graph.connections) {
            val a = graph.findById(edge.aId)
            val b = graph.findById(edge.bId)
            if (a != null && b != null) {
                val dx = b.x - a.x
                val dy = b.y - a.y
                val length = Math.hypot(dx.toDouble(), dy.toDouble())
                if (length == 0.0) continue
                val distance = Math.abs(dy * x - dx * y + b.x * a.y - b.y * a.x) / length
                if (distance < 40) {
                    // Vérifie aussi si le point est proche du milieu de la connexion
                    val midX = (a.x + b.x) / 2
                    val midY = (a.y + b.y) / 2
                    val distMid = Math.hypot((x - midX).toDouble(), (y - midY).toDouble())
                    if (distMid < 150) return edge
                }
            }
        }
        return null
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
        val colors = arrayOf("Rouge", "Vert", "Bleu", "Orange", "Cyan", "Magenta", "Noir")
        val colorValues = intArrayOf(
            Color.RED, Color.GREEN, Color.BLUE, Color.rgb(255,165,0),
            Color.CYAN, Color.MAGENTA, Color.BLACK
        )
        AlertDialog.Builder(context)
            .setTitle("Choisir une couleur")
            .setItems(colors) { _, which ->
                paintNode.color = colorValues[which]
                invalidate()
            }
            .show()
    }

    private fun editConnectionLabel(edge: Connection) {
        val input = EditText(context)
        input.setText(edge.label)
        AlertDialog.Builder(context)
            .setTitle("Modifier l'étiquette")
            .setView(input)
            .setPositiveButton("OK") { _, _ ->
                edge.label = input.text.toString()
                invalidate()
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun changeConnectionColor(edge: Connection) {
        val colors = arrayOf("Rouge", "Vert", "Bleu", "Orange", "Cyan", "Magenta", "Noir")
        val colorValues = intArrayOf(
            Color.RED, Color.GREEN, Color.BLUE, Color.rgb(255,165,0),
            Color.CYAN, Color.MAGENTA, Color.BLACK
        )
        AlertDialog.Builder(context)
            .setTitle("Couleur de la connexion")
            .setItems(colors) { _, which ->
                edge.color = colorValues[which]
                invalidate()
            }
            .show()
    }

    private fun changeConnectionThickness(edge: Connection) {
        val options = arrayOf("Fine", "Moyenne", "Épaisse")
        val thicknessValues = floatArrayOf(3f, 6f, 10f)
        AlertDialog.Builder(context)
            .setTitle("Épaisseur de la connexion")
            .setItems(options) { _, which ->
                edge.strokeWidth = thicknessValues[which]
                invalidate()
            }
            .show()
    }


}
