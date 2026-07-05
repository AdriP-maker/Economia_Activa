package pa.ac.utp.economia_activa.ui.expenses

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
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
import pa.ac.utp.economia_activa.R
import pa.ac.utp.economia_activa.data.DatabaseHelper
import pa.ac.utp.economia_activa.data.ExpenseLog
import pa.ac.utp.economia_activa.data.PeriodicExpense
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ExpensesActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper

    // Elementos de Toggles
    private lateinit var btnTabDaily: Button
    private lateinit var btnTabFixed: Button
    private lateinit var layoutDailyExpenses: LinearLayout
    private lateinit var layoutFixedExpenses: LinearLayout

    // Formulario Diario
    private lateinit var etDailyConcept: EditText
    private lateinit var etDailyAmount: EditText
    private lateinit var spinnerDailyCategory: AutoCompleteTextView
    private lateinit var btnSaveDaily: Button
    private lateinit var rvDaily: RecyclerView
    private lateinit var tvDailyEmpty: TextView
    private lateinit var dailyAdapter: ExpenseLogAdapter

    // Formulario Fijo
    private lateinit var etFixedConcept: EditText
    private lateinit var etFixedAmount: EditText
    private lateinit var etFixedDay: EditText
    private lateinit var spinnerFixedCategory: AutoCompleteTextView
    private lateinit var btnSaveFixed: Button
    private lateinit var rvFixed: RecyclerView
    private lateinit var tvFixedEmpty: TextView
    private lateinit var fixedAdapter: PeriodicExpenseAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_expenses)

        dbHelper = DatabaseHelper(this)

        // Botón Regresar
        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        // Toggles / Pestañas
        btnTabDaily = findViewById(R.id.btnTabDaily)
        btnTabFixed = findViewById(R.id.btnTabFixed)
        layoutDailyExpenses = findViewById(R.id.layoutDailyExpenses)
        layoutFixedExpenses = findViewById(R.id.layoutFixedExpenses)

        btnTabDaily.setOnClickListener {
            switchTab(true)
        }
        btnTabFixed.setOnClickListener {
            switchTab(false)
        }

        // --- Inicializar Sección Diaria ---
        etDailyConcept = findViewById(R.id.etDailyConcept)
        etDailyAmount = findViewById(R.id.etDailyAmount)
        spinnerDailyCategory = findViewById(R.id.spinnerDailyCategory)
        btnSaveDaily = findViewById(R.id.btnSaveDailyExpense)
        rvDaily = findViewById(R.id.rvExpensesHistory)
        tvDailyEmpty = findViewById(R.id.tvDailyEmpty)

        val categories = resources.getStringArray(R.array.categories_array)
        val dailySpinnerAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categories)
        spinnerDailyCategory.setAdapter(dailySpinnerAdapter)

        rvDaily.layoutManager = LinearLayoutManager(this)
        dailyAdapter = ExpenseLogAdapter(
            context = this,
            list = mutableListOf(),
            onEditClick = { expense ->
                showEditDailyDialog(expense)
            }
        )
        rvDaily.adapter = dailyAdapter

        btnSaveDaily.setOnClickListener {
            saveDailyExpense()
        }

        // --- Inicializar Sección Fija ---
        etFixedConcept = findViewById(R.id.etFixedConcept)
        etFixedAmount = findViewById(R.id.etFixedAmount)
        etFixedDay = findViewById(R.id.etFixedDay)
        spinnerFixedCategory = findViewById(R.id.spinnerFixedCategory)
        btnSaveFixed = findViewById(R.id.btnSaveFixedExpense)
        rvFixed = findViewById(R.id.rvPeriodicExpenses)
        tvFixedEmpty = findViewById(R.id.tvFixedEmpty)

        val fixedSpinnerAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categories)
        spinnerFixedCategory.setAdapter(fixedSpinnerAdapter)

        rvFixed.layoutManager = LinearLayoutManager(this)
        fixedAdapter = PeriodicExpenseAdapter(
            context = this,
            list = mutableListOf(),
            onEditClick = { expense ->
                showEditFixedDialog(expense)
            }
        )
        rvFixed.adapter = fixedAdapter

        btnSaveFixed.setOnClickListener {
            saveFixedExpense()
        }

        // Cargar datos
        loadDailyData()
        loadFixedData()
    }

    private fun switchTab(isDailyActive: Boolean) {
        if (isDailyActive) {
            btnTabDaily.setBackgroundResource(R.drawable.bg_tab_active)
            btnTabDaily.setTextColor(getColor(R.color.white))
            btnTabDaily.setTypeface(null, android.graphics.Typeface.BOLD)

            btnTabFixed.setBackgroundResource(R.drawable.bg_tab_inactive)
            btnTabFixed.setTextColor(getColor(R.color.colorTextMuted))
            btnTabFixed.setTypeface(null, android.graphics.Typeface.NORMAL)

            layoutDailyExpenses.visibility = View.VISIBLE
            layoutFixedExpenses.visibility = View.GONE
        } else {
            btnTabDaily.setBackgroundResource(R.drawable.bg_tab_inactive)
            btnTabDaily.setTextColor(getColor(R.color.colorTextMuted))
            btnTabDaily.setTypeface(null, android.graphics.Typeface.NORMAL)

            btnTabFixed.setBackgroundResource(R.drawable.bg_tab_active)
            btnTabFixed.setTextColor(getColor(R.color.white))
            btnTabFixed.setTypeface(null, android.graphics.Typeface.BOLD)

            layoutDailyExpenses.visibility = View.GONE
            layoutFixedExpenses.visibility = View.VISIBLE
        }
    }

    // --- Lógica de Gastos Diarios ---

    private fun loadDailyData() {
        val list = dbHelper.getAllExpenses()
        dailyAdapter.updateData(list)

        if (list.isEmpty()) {
            tvDailyEmpty.visibility = View.VISIBLE
            rvDaily.visibility = View.GONE
        } else {
            tvDailyEmpty.visibility = View.GONE
            rvDaily.visibility = View.VISIBLE
        }
    }

    private fun saveDailyExpense() {
        val concept = etDailyConcept.text.toString().trim()
        val amountStr = etDailyAmount.text.toString().trim()
        val category = spinnerDailyCategory.text.toString()

        if (concept.isNotEmpty() && amountStr.isNotEmpty()) {
            try {
                val amount = amountStr.toDouble()
                if (amount > 0) {
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val currentDateStr = dateFormat.format(Date())

                    dbHelper.insertExpenseLog(concept, amount, currentDateStr, category)
                    etDailyConcept.text.clear()
                    etDailyAmount.text.clear()
                    loadDailyData()
                    Toast.makeText(this, "Gasto registrado con éxito", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Ingrese un monto mayor a cero", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Monto inválido", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Complete todos los campos del formulario", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showEditDailyDialog(expense: ExpenseLog) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Editar Gasto Diario")

        val context = this
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(45, 20, 45, 20)
        }

        val etConcept = EditText(context).apply {
            hint = "Concepto"
            setText(expense.concept)
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            setTextColor(context.getColor(R.color.colorTextPrimary))
            setHintTextColor(context.getColor(R.color.colorTextSecondary))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 12)
            }
        }

        val etAmount = EditText(context).apply {
            hint = "Monto ($)"
            setText(expense.amount.toString())
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            setTextColor(context.getColor(R.color.colorTextPrimary))
            setHintTextColor(context.getColor(R.color.colorTextSecondary))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 12)
            }
        }

        val spinnerCategory = Spinner(context).apply {
            val categories = resources.getStringArray(R.array.categories_array)
            val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, categories)
            this.adapter = adapter
            val index = categories.indexOf(expense.category)
            if (index >= 0) setSelection(index)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 12)
            }
        }

        layout.addView(etConcept)
        layout.addView(etAmount)
        layout.addView(spinnerCategory)
        builder.setView(layout)

        builder.setPositiveButton("Guardar") { dialog, _ ->
            val concept = etConcept.text.toString().trim()
            val amountStr = etAmount.text.toString().trim()
            val category = spinnerCategory.selectedItem.toString()

            if (concept.isNotEmpty() && amountStr.isNotEmpty()) {
                try {
                    val amount = amountStr.toDouble()
                    if (amount > 0) {
                        dbHelper.updateExpenseLog(expense.id, concept, amount, category)
                        loadDailyData()
                        Toast.makeText(context, "Gasto actualizado con éxito", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    } else {
                        Toast.makeText(context, "Ingrese un monto mayor a cero", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Monto inválido", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Complete todos los campos", Toast.LENGTH_SHORT).show()
            }
        }

        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }

    // --- Lógica de Servicios Fijos ---

    private fun loadFixedData() {
        val list = dbHelper.getAllPeriodicExpenses()
        fixedAdapter.updateData(list)

        if (list.isEmpty()) {
            tvFixedEmpty.visibility = View.VISIBLE
            rvFixed.visibility = View.GONE
        } else {
            tvFixedEmpty.visibility = View.GONE
            rvFixed.visibility = View.VISIBLE
        }
    }

    private fun saveFixedExpense() {
        val concept = etFixedConcept.text.toString().trim()
        val amountStr = etFixedAmount.text.toString().trim()
        val dayStr = etFixedDay.text.toString().trim()
        val category = spinnerFixedCategory.text.toString()

        if (concept.isNotEmpty() && amountStr.isNotEmpty() && dayStr.isNotEmpty()) {
            try {
                val amount = amountStr.toDouble()
                val day = dayStr.toInt()

                if (amount <= 0) {
                    Toast.makeText(this, "Ingrese un monto mayor a cero", Toast.LENGTH_SHORT).show()
                    return
                }
                if (day !in 1..31) {
                    Toast.makeText(this, "El día de pago debe ser entre 1 y 31", Toast.LENGTH_SHORT).show()
                    return
                }

                dbHelper.insertPeriodicExpense(concept, amount, category, "Mensual", day)
                etFixedConcept.text.clear()
                etFixedAmount.text.clear()
                etFixedDay.text.clear()
                loadFixedData()
                Toast.makeText(this, "Servicio registrado con éxito", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Formato incorrecto en monto o día", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Complete todos los campos del formulario", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showEditFixedDialog(expense: PeriodicExpense) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Editar Servicio Fijo")

        val context = this
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(45, 20, 45, 20)
        }

        val etConcept = EditText(context).apply {
            hint = "Nombre del Servicio"
            setText(expense.concept)
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            setTextColor(context.getColor(R.color.colorTextPrimary))
            setHintTextColor(context.getColor(R.color.colorTextSecondary))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 12)
            }
        }

        val etAmount = EditText(context).apply {
            hint = "Monto ($)"
            setText(expense.amount.toString())
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            setTextColor(context.getColor(R.color.colorTextPrimary))
            setHintTextColor(context.getColor(R.color.colorTextSecondary))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 12)
            }
        }

        val etDay = EditText(context).apply {
            hint = "Día de pago"
            setText(expense.payDay.toString())
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setTextColor(context.getColor(R.color.colorTextPrimary))
            setHintTextColor(context.getColor(R.color.colorTextSecondary))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 12)
            }
        }

        val spinnerCategory = Spinner(context).apply {
            val categories = resources.getStringArray(R.array.categories_array)
            val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, categories)
            this.adapter = adapter
            val index = categories.indexOf(expense.category)
            if (index >= 0) setSelection(index)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 12)
            }
        }

        layout.addView(etConcept)
        layout.addView(etAmount)
        layout.addView(etDay)
        layout.addView(spinnerCategory)
        builder.setView(layout)

        builder.setPositiveButton("Guardar") { dialog, _ ->
            val concept = etConcept.text.toString().trim()
            val amountStr = etAmount.text.toString().trim()
            val dayStr = etDay.text.toString().trim()
            val category = spinnerCategory.selectedItem.toString()

            if (concept.isNotEmpty() && amountStr.isNotEmpty() && dayStr.isNotEmpty()) {
                try {
                    val amount = amountStr.toDouble()
                    val day = dayStr.toInt()

                    if (amount <= 0) {
                        Toast.makeText(context, "Ingrese un monto mayor a cero", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }
                    if (day !in 1..31) {
                        Toast.makeText(context, "El día de pago debe ser entre 1 y 31", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }

                    dbHelper.updatePeriodicExpense(expense.id, concept, amount, category, day)
                    loadFixedData()
                    Toast.makeText(context, "Servicio actualizado con éxito", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                } catch (e: Exception) {
                    Toast.makeText(context, "Formatos incorrectos", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Complete todos los campos", Toast.LENGTH_SHORT).show()
            }
        }

        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }
}

// =========================================================================
// ADAPTER PARA HISTORIAL DE GASTOS DIARIOS
// =========================================================================
class ExpenseLogAdapter(
    private val context: Context,
    private val list: MutableList<ExpenseLog>,
    private val onEditClick: (ExpenseLog) -> Unit
) : RecyclerView.Adapter<ExpenseLogAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvConcept: TextView = view.findViewById(R.id.tvConcept)
        val tvSubtext: TextView = view.findViewById(R.id.tvSubtext)
        val tvAmount: TextView = view.findViewById(R.id.tvAmount)
        val btnEdit: ImageView = view.findViewById(R.id.btnEdit)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_expense_log, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        holder.tvConcept.text = item.concept
        holder.tvSubtext.text = "${item.date} • ${item.category}"
        holder.tvAmount.text = String.format(Locale.getDefault(), "-$%.2f", item.amount)

        if (item.listId != null && item.listId > 0) {
            holder.btnEdit.visibility = View.GONE
        } else {
            holder.btnEdit.visibility = View.VISIBLE
            holder.btnEdit.setOnClickListener {
                onEditClick(item)
            }
        }
    }

    override fun getItemCount(): Int = list.size

    fun updateData(newList: List<ExpenseLog>) {
        list.clear()
        list.addAll(newList)
        notifyDataSetChanged()
    }
}

// =========================================================================
// ADAPTER PARA SERVICIOS FIJOS / RECURRENTES
// =========================================================================
class PeriodicExpenseAdapter(
    private val context: Context,
    private val list: MutableList<PeriodicExpense>,
    private val onEditClick: (PeriodicExpense) -> Unit
) : RecyclerView.Adapter<PeriodicExpenseAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvConcept: TextView = view.findViewById(R.id.tvConcept)
        val tvSubtext: TextView = view.findViewById(R.id.tvSubtext)
        val tvAmount: TextView = view.findViewById(R.id.tvAmount)
        val btnEdit: ImageView = view.findViewById(R.id.btnEdit)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_periodic_expense, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        holder.tvConcept.text = item.concept
        holder.tvSubtext.text = "Pago: día ${item.payDay} • ${item.category}"
        holder.tvAmount.text = String.format(Locale.getDefault(), "$%.2f", item.amount)

        holder.btnEdit.setOnClickListener {
            onEditClick(item)
        }
    }

    override fun getItemCount(): Int = list.size

    fun updateData(newList: List<PeriodicExpense>) {
        list.clear()
        list.addAll(newList)
        notifyDataSetChanged()
    }
}
