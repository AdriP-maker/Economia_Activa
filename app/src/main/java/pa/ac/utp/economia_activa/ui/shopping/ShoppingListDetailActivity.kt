package pa.ac.utp.economia_activa.ui.shopping

import android.app.AlertDialog
import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import android.Manifest
import android.content.pm.PackageManager
import android.widget.ImageButton
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions
import pa.ac.utp.economia_activa.R
import pa.ac.utp.economia_activa.data.DatabaseHelper
import pa.ac.utp.economia_activa.data.ListItem
import pa.ac.utp.economia_activa.data.Product
import pa.ac.utp.economia_activa.data.ShoppingList
import java.util.Locale

class ShoppingListDetailActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private var listId: Long = -1
    private var shoppingList: ShoppingList? = null

    private lateinit var tvTitle: TextView
    private lateinit var tvDetailBudget: TextView
    private lateinit var tvDetailTotal: TextView
    private lateinit var tvBudgetAlert: TextView
    private lateinit var rvListItems: RecyclerView
    private lateinit var tvDetailEmptyState: TextView
    private lateinit var fabAddItem: FloatingActionButton
    private lateinit var btnFinalizeList: Button
    private lateinit var lyFooter: LinearLayout

    private lateinit var adapter: ListItemAdapter

    private var activeEtBarcode: EditText? = null
    private var activeEtPrice: EditText? = null
    private var activeActvName: AutoCompleteTextView? = null
    private var activeSpinnerCategory: Spinner? = null

    private val barcodeLauncher = registerForActivityResult(ScanContract()) { result: ScanIntentResult ->
        if (result.contents != null) {
            onBarcodeScanned(result.contents)
        } else {
            Toast.makeText(this, "Escaneo cancelado", Toast.LENGTH_SHORT).show()
        }
    }

    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startBarcodeScanner()
        } else {
            Toast.makeText(this, "Se requiere permiso de cámara para usar el escáner", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shopping_list_detail)

        listId = intent.getLongExtra("LIST_ID", -1)
        dbHelper = DatabaseHelper(this)
        shoppingList = dbHelper.getShoppingList(listId)

        if (listId == -1L || shoppingList == null) {
            Toast.makeText(this, "Error al cargar la lista", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Configurar UI
        tvTitle = findViewById(R.id.tvTitle)
        tvDetailBudget = findViewById(R.id.tvDetailBudget)
        tvDetailTotal = findViewById(R.id.tvDetailTotal)
        tvBudgetAlert = findViewById(R.id.tvBudgetAlert)
        rvListItems = findViewById(R.id.rvListItems)
        tvDetailEmptyState = findViewById(R.id.tvDetailEmptyState)
        fabAddItem = findViewById(R.id.fabAddItem)
        btnFinalizeList = findViewById(R.id.btnFinalizeList)
        lyFooter = findViewById(R.id.lyFooter)

        tvTitle.text = shoppingList?.name
        tvDetailBudget.text = String.format(Locale.getDefault(), "$%.2f", shoppingList?.budget ?: 0.0)

        // Botón regresar
        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        rvListItems.layoutManager = LinearLayoutManager(this)
        adapter = ListItemAdapter(
            context = this,
            items = mutableListOf(),
            isEditable = !(shoppingList?.isFinished ?: false),
            onCheckClick = { item ->
                dbHelper.updateListItemBoughtState(item.id, !item.isBought, item.unitPrice, item.quantity)
                refreshData()
            },
            onEditClick = { item ->
                showCheckItemDialog(item)
            },
            onDeleteClick = { item ->
                showDeleteItemConfirm(item)
            }
        )
        rvListItems.adapter = adapter

        // Agregar Item
        fabAddItem.setOnClickListener {
            showAddItemDialog()
        }

        // Finalizar Lista
        btnFinalizeList.setOnClickListener {
            showFinalizeConfirmDialog()
        }

        // Ocultar controles si está finalizada
        if (shoppingList?.isFinished == true) {
            fabAddItem.visibility = View.GONE
            lyFooter.visibility = View.GONE
        }

        refreshData()
    }

    private fun refreshData() {
        val items = dbHelper.getListItems(listId)
        adapter.updateData(items)

        if (items.isEmpty()) {
            tvDetailEmptyState.visibility = View.VISIBLE
            rvListItems.visibility = View.GONE
        } else {
            tvDetailEmptyState.visibility = View.GONE
            rvListItems.visibility = View.VISIBLE
        }

        // Calcular totales
        val total = dbHelper.getShoppingListEstimatedTotal(listId)
        tvDetailTotal.text = String.format(Locale.getDefault(), "$%.2f", total)

        val budget = shoppingList?.budget ?: 0.0

        // Alertas de presupuesto
        if (total > budget) {
            tvBudgetAlert.visibility = View.VISIBLE
            tvBudgetAlert.text = getString(R.string.advertencia_presupuesto) + " (Exceso: " + String.format(Locale.getDefault(), "$%.2f", total - budget) + ")"
            tvBudgetAlert.setBackgroundColor(getColor(R.color.colorDanger))
            tvDetailTotal.setTextColor(getColor(R.color.colorDanger))
        } else if (total >= budget * 0.8) {
            tvBudgetAlert.visibility = View.VISIBLE
            tvBudgetAlert.text = getString(R.string.casi_presupuesto) + " (Límite: " + String.format(Locale.getDefault(), "$%.2f", budget) + ")"
            tvBudgetAlert.setBackgroundColor(getColor(R.color.colorWarning))
            tvDetailTotal.setTextColor(getColor(R.color.colorWarning))
        } else {
            tvBudgetAlert.visibility = View.GONE
            tvDetailTotal.setTextColor(getColor(R.color.colorPrimary))
        }
    }

    private fun showAddItemDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Agregar Producto")

        val context = this
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_add_item, null)
        builder.setView(view)

        val actvName = view.findViewById<AutoCompleteTextView>(R.id.actvProductName)
        val etQty = view.findViewById<EditText>(R.id.etProductQty)
        val etPrice = view.findViewById<EditText>(R.id.etProductPrice)
        val spinnerCategory = view.findViewById<Spinner>(R.id.spinnerCategory)
        val etBarcode = view.findViewById<EditText>(R.id.etBarcode)
        val btnScanBarcode = view.findViewById<ImageButton>(R.id.btnScanBarcode)

        // Asignar referencias para el escáner
        activeEtBarcode = etBarcode
        activeEtPrice = etPrice
        activeActvName = actvName
        activeSpinnerCategory = spinnerCategory

        btnScanBarcode.setOnClickListener {
            checkCameraPermissionAndScan()
        }

        // Rellenar campos si el usuario escribe un código de barra manualmente y pierde el foco
        etBarcode.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val barcode = etBarcode.text.toString().trim()
                if (barcode.isNotEmpty()) {
                    val (name, price, category) = getProductByBarcode(barcode)
                    if (name.isNotEmpty()) {
                        actvName.setText(name)
                        etPrice.setText(String.format(Locale.getDefault(), "%.2f", price))
                        val categoriesList = resources.getStringArray(R.array.categories_array)
                        val idx = categoriesList.indexOf(category)
                        if (idx >= 0) spinnerCategory.setSelection(idx)
                    }
                }
            }
        }

        // Configurar autocompletado con productos de la DB
        val products = dbHelper.getAllProducts()
        val productNames = products.map { it.name }
        val autocompleteAdapter = ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line, productNames)
        actvName.setAdapter(autocompleteAdapter)

        // Rellenar precio y categoría si el producto ya existe en la DB
        actvName.setOnItemClickListener { parent, _, position, _ ->
            val selectedName = parent.getItemAtPosition(position) as String
            val matchingProduct = products.find { it.name.lowercase() == selectedName.lowercase() }
            if (matchingProduct != null) {
                etPrice.setText(matchingProduct.lastPrice.toString())
                // Seleccionar categoría correspondiente
                val categories = resources.getStringArray(R.array.categories_array)
                val index = categories.indexOf(matchingProduct.category)
                if (index >= 0) {
                    spinnerCategory.setSelection(index)
                }
            }
        }

        // Cargar las categorías en el spinner
        val categories = resources.getStringArray(R.array.categories_array)
        val spinnerAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, categories)
        spinnerCategory.adapter = spinnerAdapter

        builder.setPositiveButton("Agregar") { dialog, _ ->
            val name = actvName.text.toString().trim()
            val qtyStr = etQty.text.toString().trim()
            val priceStr = etPrice.text.toString().trim()
            val category = spinnerCategory.selectedItem.toString()
            val barcode = etBarcode.text.toString().trim()

            if (name.isNotEmpty()) {
                val qty = if (qtyStr.isNotEmpty()) qtyStr.toDouble() else 1.0
                val price = if (priceStr.isNotEmpty()) priceStr.toDouble() else 0.0

                // Buscar o crear el producto en la DB asociando el código de barra
                val product = dbHelper.getOrCreateProduct(name, category, "unidad", if (barcode.isNotEmpty()) barcode else null, price)

                // Insertar ítem en la lista
                dbHelper.insertListItem(listId, product, qty, price)
                dialog.dismiss()
                refreshData()
            } else {
                Toast.makeText(context, "Ingrese el nombre del producto", Toast.LENGTH_SHORT).show()
            }
        }

        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.cancel()
        }

        val dialog = builder.create()
        dialog.setOnDismissListener {
            // Limpiar referencias
            activeEtBarcode = null
            activeEtPrice = null
            activeActvName = null
            activeSpinnerCategory = null
        }
        dialog.show()
    }

    private fun showCheckItemDialog(item: ListItem) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Editar Cantidad y Precio")

        val context = this
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(45, 20, 45, 20)
        }

        val tvLabel = TextView(context).apply {
            text = "Editar cantidad y precio de \"${item.productName}\":"
            setTextColor(context.getColor(R.color.colorTextPrimary))
            textSize = 14f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 16)
            }
        }

        val etQty = EditText(context).apply {
            hint = "Cantidad"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            setText(item.quantity.toString())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 12)
            }
        }

        val etPrice = EditText(context).apply {
            hint = "Precio unitario ($)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            setText(item.unitPrice.toString())
        }

        layout.addView(tvLabel)
        layout.addView(etQty)
        layout.addView(etPrice)
        builder.setView(layout)

        builder.setPositiveButton("Guardar") { dialog, _ ->
            val qtyStr = etQty.text.toString().trim()
            val priceStr = etPrice.text.toString().trim()

            if (qtyStr.isNotEmpty() && priceStr.isNotEmpty()) {
                try {
                    val finalQty = qtyStr.toDouble()
                    val finalPrice = priceStr.toDouble()
                    dbHelper.updateListItemBoughtState(item.id, item.isBought, finalPrice, finalQty)
                    dialog.dismiss()
                    refreshData()
                } catch (e: Exception) {
                    Toast.makeText(context, "Formatos incorrectos", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Complete ambos campos", Toast.LENGTH_SHORT).show()
            }
        }

        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }

    private fun showDeleteItemConfirm(item: ListItem) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar del mercado")
            .setMessage("¿Desea quitar \"${item.productName}\" de esta lista?")
            .setPositiveButton("Eliminar") { dialog, _ ->
                dbHelper.deleteListItem(item.id)
                refreshData()
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showFinalizeConfirmDialog() {
        AlertDialog.Builder(this)
            .setTitle("Finalizar Lista")
            .setMessage("¿Desea cerrar esta lista de compras? Al finalizar, se bloquearán los productos y el costo total se registrará automáticamente en el historial de gastos mensuales.")
            .setPositiveButton("Finalizar") { dialog, _ ->
                dbHelper.updateShoppingListStatus(listId, true)
                Toast.makeText(this, "Lista finalizada y gasto registrado", Toast.LENGTH_LONG).show()
                dialog.dismiss()
                finish() // Volver a la pantalla anterior
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun checkCameraPermissionAndScan() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startBarcodeScanner()
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startBarcodeScanner() {
        val options = ScanOptions().apply {
            setDesiredBarcodeFormats(ScanOptions.ALL_CODE_TYPES)
            setPrompt("Alinea el código de barra en el recuadro")
            setCameraId(0)
            setBeepEnabled(true)
            setBarcodeImageEnabled(false)
            setOrientationLocked(false)
            setCaptureActivity(CustomScannerActivity::class.java)
        }
        barcodeLauncher.launch(options)
    }

    private fun onBarcodeScanned(barcode: String) {
        activeEtBarcode?.setText(barcode)
        
        // 1. Buscar en la base de datos de productos para ver si ya fue aprendido/creado
        val dbProduct = dbHelper.getProductByBarcode(barcode)
        if (dbProduct != null) {
            activeActvName?.setText(dbProduct.name)
            activeEtPrice?.setText(String.format(Locale.getDefault(), "%.2f", dbProduct.lastPrice))
            
            val categoriesList = resources.getStringArray(R.array.categories_array)
            val index = categoriesList.indexOf(dbProduct.category)
            if (index >= 0) {
                activeSpinnerCategory?.setSelection(index)
            }
            Toast.makeText(this, "Producto encontrado (Guardado): ${dbProduct.name} ($${String.format(Locale.getDefault(), "%.2f", dbProduct.lastPrice)})", Toast.LENGTH_SHORT).show()
            return
        }

        // 2. Si no, buscar en la lista de productos semilla
        val (name, price, category) = getProductByBarcode(barcode)
        if (name.isNotEmpty()) {
            activeActvName?.setText(name)
            activeEtPrice?.setText(String.format(Locale.getDefault(), "%.2f", price))
            
            val categoriesList = resources.getStringArray(R.array.categories_array)
            val index = categoriesList.indexOf(category)
            if (index >= 0) {
                activeSpinnerCategory?.setSelection(index)
            }
            Toast.makeText(this, "Producto encontrado: $name ($$price)", Toast.LENGTH_SHORT).show()
        } else {
            // Si el código no está registrado, sugerimos un precio simulado aleatorio
            val simulatedPrice = (1.0 + Math.random() * 8.0)
            val roundedPrice = (Math.round(simulatedPrice * 100.0) / 100.0)
            activeEtPrice?.setText(String.format(Locale.getDefault(), "%.2f", roundedPrice))
            Toast.makeText(this, "Código nuevo. Precio estimado sugerido: $$roundedPrice", Toast.LENGTH_LONG).show()
        }
    }

    private fun getProductByBarcode(barcode: String): Triple<String, Double, String> {
        return when (barcode.trim()) {
            "75010011", "7501031311309" -> Triple("Leche Entera", 1.60, "Alimentos y Super")
            "75010022" -> Triple("Pan Molde", 2.10, "Alimentos y Super")
            "75010033" -> Triple("Arroz de Primera", 1.25, "Alimentos y Super")
            "75010044" -> Triple("Aceite Vegetal", 3.50, "Alimentos y Super")
            "75010055" -> Triple("Huevo Docena", 2.20, "Alimentos y Super")
            "75010066" -> Triple("Pechuga de Pollo", 4.50, "Alimentos y Super")
            "75010077" -> Triple("Azúcar", 0.95, "Alimentos y Super")
            "75010088" -> Triple("Café", 2.80, "Alimentos y Super")
            "75010099" -> Triple("Detergente Ropa", 4.25, "Otros Gastos")
            "75010100" -> Triple("Jabón de Baño", 0.85, "Otros Gastos")
            else -> Triple("", 0.0, "")
        }
    }
}

// =========================================================================
// ADAPTER PARA ELEMENTOS DE LA LISTA DE COMPRAS
// =========================================================================
class ListItemAdapter(
    private val context: Context,
    private val items: MutableList<ListItem>,
    private val isEditable: Boolean,
    private val onCheckClick: (ListItem) -> Unit,
    private val onEditClick: (ListItem) -> Unit,
    private val onDeleteClick: (ListItem) -> Unit
) : RecyclerView.Adapter<ListItemAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val btnCheckItem: ImageView = view.findViewById(R.id.btnCheckItem)
        val tvItemName: TextView = view.findViewById(R.id.tvItemName)
        val tvItemSubtext: TextView = view.findViewById(R.id.tvItemSubtext)
        val tvItemTotal: TextView = view.findViewById(R.id.tvItemTotal)
        val btnEditItem: ImageView = view.findViewById(R.id.btnEditItem)
        val btnDeleteItem: ImageView = view.findViewById(R.id.btnDeleteItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_list_product, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvItemName.text = item.productName
        
        holder.tvItemSubtext.text = "${item.productCategory} • ${String.format(Locale.getDefault(), "%.1f", item.quantity)} x $${String.format(Locale.getDefault(), "%.2f", item.unitPrice)}"
        
        val total = item.quantity * item.unitPrice
        holder.tvItemTotal.text = String.format(Locale.getDefault(), "$%.2f", total)

        // Estado comprado con iconos de checkbox
        if (item.isBought) {
            holder.btnCheckItem.setImageResource(R.drawable.ic_checkbox_checked)
            holder.btnCheckItem.imageTintList = ColorStateList.valueOf(context.getColor(R.color.colorPrimary))
            holder.tvItemName.paintFlags = holder.tvItemName.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
            holder.tvItemName.setTextColor(context.getColor(R.color.colorTextSecondary))
        } else {
            holder.btnCheckItem.setImageResource(R.drawable.ic_checkbox_unchecked)
            holder.btnCheckItem.imageTintList = ColorStateList.valueOf(context.getColor(R.color.colorTextSecondary))
            holder.tvItemName.paintFlags = holder.tvItemName.paintFlags and android.graphics.Paint.STRIKE_THRU_TEXT_FLAG.inv()
            holder.tvItemName.setTextColor(context.getColor(R.color.colorTextPrimary))
        }

        if (isEditable) {
            holder.btnCheckItem.setOnClickListener {
                onCheckClick(item)
            }
            holder.btnEditItem.setOnClickListener {
                onEditClick(item)
            }
            holder.btnDeleteItem.setOnClickListener {
                onDeleteClick(item)
            }
            holder.btnEditItem.visibility = View.VISIBLE
            holder.btnDeleteItem.visibility = View.VISIBLE
        } else {
            holder.btnCheckItem.setOnClickListener(null)
            holder.btnEditItem.setOnClickListener(null)
            holder.btnDeleteItem.setOnClickListener(null)
            holder.btnEditItem.visibility = View.GONE
            holder.btnDeleteItem.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<ListItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}
