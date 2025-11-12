package istic.mob.networkbs

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import istic.mob.networkbs.Graph
import istic.mob.networkbs.Object
import istic.mob.networkbs.Graphview
import androidx.appcompat.widget.Toolbar
import android.widget.FrameLayout
import android.content.Intent
import android.graphics.Bitmap
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class MainActivity : AppCompatActivity() {

    private lateinit var graph: Graph
    private lateinit var graphView: Graphview

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Chargeement du layout activity_main.xml
        setContentView(R.layout.activity_main)

        // Activer la Toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)


        // Récupère la hauteur du Toolbar une fois qu’il est mesuré
        toolbar.post {
            val toolbarHeight = toolbar.height
            graphView.setToolbarHeight(toolbarHeight)
        }

        graph = Graph().apply {
        }

        // Créer la vue qui dessine le graphe
        graphView = Graphview(this, graph)

        // Ajouter la vue dans le container prévu dans le layout
        val container = findViewById<FrameLayout>(R.id.graph_container)
        container.addView(graphView)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_mode_add_node -> {
                graphView.setMode(Graphview.Mode.ADD_NODE)
                true
            }
            R.id.action_mode_add_edge -> {
                graphView.setMode(Graphview.Mode.ADD_EDGE)
                true
            }
            R.id.action_mode_edit -> {
                graphView.setMode(Graphview.Mode.EDIT)
                true
            }
            R.id.action_reset -> {
                graph.clear()
                graphView.invalidate()
                true
            }
            R.id.action_save -> {
                saveGraph();
                true
            }
            R.id.action_load -> {
                loadGraph();
                true
            }
            R.id.action_send_email -> {
                captureAndSendEmail()
                true
            }
            R.id.action_choose_plan -> {
                showChoosePlanDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private val saveFileName = "graph.json"

    private fun saveGraph() {
        val json = graph.toJson()
        openFileOutput(saveFileName, MODE_PRIVATE).use { it.write(json.toByteArray()) }
    }

    private fun loadGraph() {
        val file = getFileStreamPath(saveFileName)
        if (file.exists()) {
            val json = file.readText()
            val newGraph = Graph.fromJson(json)
            graph = newGraph
            graphView.invalidate()
        }
    }

    private fun captureAndSendEmail() {
        // Capturer le contenu de la zone du graphe
        val bitmap = Bitmap.createBitmap(
            graphView.width,
            graphView.height,
            Bitmap.Config.ARGB_8888
        )
        val canvas = android.graphics.Canvas(bitmap)
        graphView.draw(canvas)

        // Sauvegarder l'image temporairement
        val imageFile = File(cacheDir, "graph_capture.png")
        val outputStream = FileOutputStream(imageFile)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        outputStream.flush()
        outputStream.close()

        // Obtenir un URI sécurisé pour le partage
        val uri = FileProvider.getUriForFile(
            this,
            "${applicationContext.packageName}.provider",
            imageFile
        )

        // Créer l’intent d’envoi d’email
        val emailIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_SUBJECT, "Mon réseau domestique")
            putExtra(Intent.EXTRA_TEXT, "Voici la capture de mon réseau.")
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        // Lancer le choix de l’application de mail
        startActivity(Intent.createChooser(emailIntent, "Envoyer par e-mail"))
    }

    private fun showChoosePlanDialog() {
        val planNames = arrayOf("Plan 1", "Plan 2", "Plan 3")
        val planResources = intArrayOf(
            R.drawable.plan1,
            R.drawable.plan2,
            R.drawable.plan3
        )

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Choisir un plan d'appartement")
            .setItems(planNames) { _, which ->
                graphView.setBackgroundImage(planResources[which])
            }
            .setNegativeButton("Annuler", null)
            .show()
    }


    //
//    private val gson = Gson()
//    private val SAVE_FILENAME = "network_graph.json"

    /*fun saveGraphToInternal(graph: Graph) {
        val json = gson.toJson(graph)
        openFileOutput(SAVE_FILENAME, MODE_PRIVATE).use { it.write(json.toByteArray()) }
        Toast.makeText(this, getString(R.string.save_done), Toast.LENGTH_SHORT).show()
    }*/

    /*fun loadGraphFromInternal(): Graph? {
        val file = File(filesDir, SAVE_FILENAME)
        if(!file.exists()) {
            Toast.makeText(this, getString(R.string.no_saved_graph), Toast.LENGTH_SHORT).show()
            return null
        }
        val json = file.readText()
        return gson.fromJson(json, Graph::class.java).also {
            // remplacer le graph courant et invalider la vue
            this.graph = it
            graphView.setGraph(it) // ajoute setter dans GraphView
            graphView.invalidate()
        }
    }*/

}
