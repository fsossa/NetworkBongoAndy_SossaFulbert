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
            else -> super.onOptionsItemSelected(item)
        }
    }
}
