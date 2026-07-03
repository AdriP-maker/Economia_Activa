package pa.ac.utp.economia_activa.ui.shopping

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import pa.ac.utp.economia_activa.R
import pa.ac.utp.economia_activa.data.DatabaseHelper
import pa.ac.utp.economia_activa.data.ShoppingList
import java.util.Locale

class ShoppingListsActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var adapter: ShoppingListAdapter
    private lateinit var rvShoppingLists: RecyclerView
    private lateinit var tvEmptyState: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shopping_lists)

        dbHelper = DatabaseHelper(this)

        // Botón regresar
        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        tvEmptyState = findViewById(R.id.tvEmptyState)
        rvShoppingLists = findViewById(R.id.rvShoppingLists)
        rvShoppingLists.layoutManager = LinearLayoutManager(this)

        adapter = ShoppingListAdapter(
            context = this,
            lists = mutableListOf(),
            onItemClick = { list ->
                val intent = Intent(this, ShoppingListDetailActivity::class.java).apply {
                    putExtra("LIST_ID", list.id)
                }
                startActivity(intent)
            },
            onDeleteClick = { list ->
                showDeleteConfirmDialog(list)
            }
        )
        rvShoppingLists.adapter = adapter

        // FAB Agregar Lista
        findViewById<FloatingActionButton>(R.id.fabAddList).setOnClickListener {
            showAddListDialog()
        }

        loadLists()
    }

    override fun onResume() {
        super.onResume()
        loadLists()
    }

    private fun loadLists() {
        val lists = dbHelper.getAllShoppingLists()
        adapter.updateData(lists)

        if (lists.isEmpty()) {
            tvEmptyState.visibility = View.VISIBLE
            rvShoppingLists.visibility = View.GONE
        } else {
            tvEmptyState.visibility = View.GONE
            rvShoppingLists.visibility = View.VISIBLE
        }
    }

    private fun showAddListDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.crear_nueva_lista))

        // Contenedor de inputs
        val context = this
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 20, 40, 20)
        }

        val etName = EditText(context).apply {
            hint = getString(R.string.hint_nombre_lista)
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 20)
            }
        }

        val etBudget = EditText(context).apply {
            hint = getString(R.string.hint_presupuesto)
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        }

        layout.addView(etName)
        layout.addView(etBudget)
        builder.setView(layout)

        builder.setPositiveButton(getString(R.string.btn_crear)) { dialog, _ ->
            val name = etName.text.toString().trim()
            val budgetStr = etBudget.text.toString().trim()

            if (name.isNotEmpty() && budgetStr.isNotEmpty()) {
                try {
                    val budget = budgetStr.toDouble()
                    if (budget > 0) {
                        val newId = dbHelper.insertShoppingList(name, budget)
                        dialog.dismiss()
                        loadLists()

                        // Abrir detalle directamente
                        val intent = Intent(this, ShoppingListDetailActivity::class.java).apply {
                            putExtra("LIST_ID", newId)
                        }
                        startActivity(intent)
                    } else {
                        Toast.makeText(context, "El presupuesto debe ser mayor a 0", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Monto inválido", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Complete todos los campos", Toast.LENGTH_SHORT).show()
            }
        }

        builder.setNegativeButton(getString(R.string.btn_cancelar)) { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }

    private fun showDeleteConfirmDialog(list: ShoppingList) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar Lista")
            .setMessage("¿Está seguro de que desea eliminar la lista \"${list.name}\" y todos sus productos? Esta acción no se puede deshacer.")
            .setPositiveButton("Eliminar") { dialog, _ ->
                dbHelper.deleteShoppingList(list.id)
                loadLists()
                Toast.makeText(this, "Lista eliminada", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
}

// =========================================================================
// ADAPTER PARA RECYCLERVIEW DE LISTAS DE COMPRAS
// =========================================================================
class ShoppingListAdapter(
    private val context: Context,
    private val lists: MutableList<ShoppingList>,
    private val onItemClick: (ShoppingList) -> Unit,
    private val onDeleteClick: (ShoppingList) -> Unit
) : RecyclerView.Adapter<ShoppingListAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvListName: TextView = view.findViewById(R.id.tvListName)
        val tvListDate: TextView = view.findViewById(R.id.tvListDate)
        val tvListBudget: TextView = view.findViewById(R.id.tvListBudget)
        val tvListSpent: TextView = view.findViewById(R.id.tvListSpent)
        val tvListStatus: TextView = view.findViewById(R.id.tvListStatus)
        val btnDeleteList: ImageView = view.findViewById(R.id.btnDeleteList)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_shopping_list, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val list = lists[position]
        holder.tvListName.text = list.name
        holder.tvListDate.text = list.date
        holder.tvListBudget.text = String.format(Locale.getDefault(), "$%.2f", list.budget)

        // Obtener el total gastado de la DB
        val dbHelper = DatabaseHelper(context)
        val total = dbHelper.getShoppingListTotalSpent(list.id)
        holder.tvListSpent.text = String.format(Locale.getDefault(), "$%.2f", total)

        // Configurar badge
        if (list.isFinished) {
            holder.tvListStatus.text = "Finalizada"
            holder.tvListStatus.backgroundTintList = ColorStateList.valueOf(context.getColor(R.color.colorTextSecondary))
        } else {
            holder.tvListStatus.text = "Activa"
            holder.tvListStatus.backgroundTintList = ColorStateList.valueOf(context.getColor(R.color.colorPrimary))
        }

        // Color del total gastado en base al presupuesto
        if (total > list.budget) {
            holder.tvListSpent.setTextColor(context.getColor(R.color.colorDanger))
        } else {
            holder.tvListSpent.setTextColor(context.getColor(R.color.colorPrimary))
        }

        holder.itemView.setOnClickListener {
            onItemClick(list)
        }

        holder.btnDeleteList.setOnClickListener {
            onDeleteClick(list)
        }
    }

    override fun getItemCount(): Int = lists.size

    fun updateData(newLists: List<ShoppingList>) {
        lists.clear()
        lists.addAll(newLists)
        notifyDataSetChanged()
    }
}
