package pa.ac.utp.economia_activa

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import pa.ac.utp.economia_activa.data.DatabaseHelper
import pa.ac.utp.economia_activa.ui.expenses.ExpensesActivity
import pa.ac.utp.economia_activa.ui.reports.ReportsActivity
import pa.ac.utp.economia_activa.ui.shopping.ShoppingListsActivity
import pa.ac.utp.economia_activa.ui.tips.SavingsTipsActivity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private var monthlyBudget = 500.0

    private lateinit var tvBudgetVal: TextView
    private lateinit var tvSpentVal: TextView
    private lateinit var tvRemainingVal: TextView
    private lateinit var budgetProgressBar: ProgressBar
    private lateinit var tvPercentageSpent: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Aplicar insets para el contenedor principal
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        dbHelper = DatabaseHelper(this)

        // Cargar presupuesto guardado
        val prefs = getSharedPreferences("economia_activa_prefs", Context.MODE_PRIVATE)
        monthlyBudget = prefs.getFloat("monthly_budget", 500.0f).toDouble()

        // Inicializar vistas del dashboard
        tvBudgetVal = findViewById(R.id.tvBudgetVal)
        tvSpentVal = findViewById(R.id.tvSpentVal)
        tvRemainingVal = findViewById(R.id.tvRemainingVal)
        budgetProgressBar = findViewById(R.id.budgetProgressBar)
        tvPercentageSpent = findViewById(R.id.tvPercentageSpent)

        val btnEditBudget: ImageView = findViewById(R.id.btnEditBudget)
        btnEditBudget.setOnClickListener {
            showEditBudgetDialog()
        }

        val btnSettings: ImageView = findViewById(R.id.btnSettings)
        btnSettings.setOnClickListener {
            showSettingsDialog()
        }

        // Configurar botones/tarjetas del menú
        findViewById<CardView>(R.id.cardShopping).setOnClickListener {
            startActivity(Intent(this, ShoppingListsActivity::class.java))
        }

        findViewById<CardView>(R.id.cardExpenses).setOnClickListener {
            startActivity(Intent(this, ExpensesActivity::class.java))
        }

        findViewById<CardView>(R.id.cardReports).setOnClickListener {
            startActivity(Intent(this, ReportsActivity::class.java))
        }

        findViewById<CardView>(R.id.cardTips).setOnClickListener {
            startActivity(Intent(this, SavingsTipsActivity::class.java))
        }

        updateDashboard()
    }

    override fun onResume() {
        super.onResume()
        updateDashboard()
    }

    private fun updateDashboard() {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val currentMonthStr = dateFormat.format(calendar.time)

        // Calcular total gastado en el mes (historial + recurrentes activos)
        val spent = dbHelper.getMonthlySpent(currentMonthStr)
        val remaining = monthlyBudget - spent

        // Actualizar UI
        tvBudgetVal.text = String.format(Locale.getDefault(), "$%.2f", monthlyBudget)
        tvSpentVal.text = String.format(Locale.getDefault(), "$%.2f", spent)
        tvRemainingVal.text = String.format(Locale.getDefault(), "$%.2f", remaining)

        // Calcular porcentaje
        val percent = if (monthlyBudget > 0) ((spent / monthlyBudget) * 100).toInt() else 0
        budgetProgressBar.progress = if (percent > 100) 100 else percent

        tvPercentageSpent.text = "$percent% gastado"

        // Cambiar colores si excede el presupuesto
        if (remaining < 0) {
            tvRemainingVal.setTextColor(getColor(R.color.colorDanger))
            budgetProgressBar.progressTintList = getColorStateList(R.color.colorDanger)
        } else if (percent >= 80) {
            tvRemainingVal.setTextColor(getColor(R.color.colorWarning))
            budgetProgressBar.progressTintList = getColorStateList(R.color.colorWarning)
        } else {
            tvRemainingVal.setTextColor(getColor(R.color.colorSuccess))
            budgetProgressBar.progressTintList = getColorStateList(R.color.colorPrimary)
        }
    }

    private fun showEditBudgetDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Definir Presupuesto Mensual")

        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        input.setText(monthlyBudget.toString())
        builder.setView(input)

        builder.setPositiveButton("Guardar") { dialog, _ ->
            val budgetStr = input.text.toString().trim()
            if (budgetStr.isNotEmpty()) {
                try {
                    val newBudget = budgetStr.toDouble()
                    if (newBudget > 0) {
                        monthlyBudget = newBudget
                        val prefs = getSharedPreferences("economia_activa_prefs", Context.MODE_PRIVATE)
                        prefs.edit().putFloat("monthly_budget", newBudget.toFloat()).apply()
                        updateDashboard()
                        Toast.makeText(this, "Presupuesto actualizado", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Ingrese un valor mayor a cero", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Formato incorrecto", Toast.LENGTH_SHORT).show()
                }
            }
            dialog.dismiss()
        }

        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }

    private fun showSettingsDialog() {
        val prefs = getSharedPreferences("economia_activa_prefs", Context.MODE_PRIVATE)
        val currentName = prefs.getString("user_name", "") ?: ""
        val currentLastname = prefs.getString("user_lastname", "") ?: ""
        val currentSalary = prefs.getFloat("user_salary", 0.0f).toDouble()
        val isBiometricEnabled = prefs.getBoolean("biometrics_enabled", false)

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Configuración de Perfil")

        val context = this
        val layout = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(50, 30, 50, 30)
        }

        // Campo Nombre
        val tvNameLabel = TextView(context).apply {
            text = "Nombre"
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 12f)
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(context.getColor(R.color.colorTextPrimary))
        }
        val etName = EditText(context).apply {
            setText(currentName)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
            setTextColor(context.getColor(R.color.colorTextPrimary))
            setHintTextColor(context.getColor(R.color.colorTextSecondary))
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 16) }
        }

        // Campo Apellido
        val tvLastnameLabel = TextView(context).apply {
            text = "Apellido"
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 12f)
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(context.getColor(R.color.colorTextPrimary))
        }
        val etLastname = EditText(context).apply {
            setText(currentLastname)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
            setTextColor(context.getColor(R.color.colorTextPrimary))
            setHintTextColor(context.getColor(R.color.colorTextSecondary))
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 16) }
        }

        // Campo Sueldo
        val tvSalaryLabel = TextView(context).apply {
            text = "Sueldo Mensual ($)"
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 12f)
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(context.getColor(R.color.colorTextPrimary))
        }
        val etSalary = EditText(context).apply {
            setText(currentSalary.toString())
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            setTextColor(context.getColor(R.color.colorTextPrimary))
            setHintTextColor(context.getColor(R.color.colorTextSecondary))
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 20) }
        }

        // Switch Biométrico
        val switchBiometric = androidx.appcompat.widget.SwitchCompat(context).apply {
            text = "Seguridad Biométrica"
            isChecked = isBiometricEnabled
            setTextColor(context.getColor(R.color.colorTextPrimary))
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 24) }
        }

        // Botón Borrar Datos (Rojo)
        val btnClearData = Button(context).apply {
            text = "Borrar todos los datos"
            backgroundTintList = android.content.res.ColorStateList.valueOf(context.getColor(R.color.colorDanger))
            setTextColor(context.getColor(R.color.white))
            setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 8) }

            setOnClickListener {
                AlertDialog.Builder(context)
                    .setTitle("⚠️ ¿Eliminar todos los datos?")
                    .setMessage("Esta acción eliminará de forma permanente todo tu historial de gastos, facturas, productos guardados y configuraciones de perfil. La aplicación se reiniciará por completo.")
                    .setPositiveButton("Sí, Eliminar todo") { confirmDialog, _ ->
                        // Limpiar SQLite
                        dbHelper.clearAllData()
                        // Limpiar SharedPreferences
                        prefs.edit().clear().apply()

                        Toast.makeText(context, "Todos los datos eliminados", Toast.LENGTH_SHORT).show()
                        confirmDialog.dismiss()

                        // Reiniciar aplicación
                        val restartIntent = Intent(context, Home::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        }
                        startActivity(restartIntent)
                        finish()
                    }
                    .setNegativeButton("Cancelar") { confirmDialog, _ ->
                        confirmDialog.dismiss()
                    }
                    .show()
            }
        }

        layout.addView(tvNameLabel)
        layout.addView(etName)
        layout.addView(tvLastnameLabel)
        layout.addView(etLastname)
        layout.addView(tvSalaryLabel)
        layout.addView(etSalary)
        layout.addView(switchBiometric)
        layout.addView(btnClearData)

        builder.setView(layout)

        builder.setPositiveButton("Guardar") { dialog, _ ->
            val name = etName.text.toString().trim()
            val lastname = etLastname.text.toString().trim()
            val salaryStr = etSalary.text.toString().trim()
            val biometricVal = switchBiometric.isChecked

            if (name.isNotEmpty() && lastname.isNotEmpty() && salaryStr.isNotEmpty()) {
                try {
                    val salaryVal = salaryStr.toDouble()
                    if (salaryVal > 0) {
                        prefs.edit().apply {
                            putString("user_name", name)
                            putString("user_lastname", lastname)
                            putFloat("user_salary", salaryVal.toFloat())
                            putBoolean("biometrics_enabled", biometricVal)
                            apply()
                        }
                        Toast.makeText(context, "Datos actualizados", Toast.LENGTH_SHORT).show()
                        updateDashboard()
                        dialog.dismiss()
                    } else {
                        Toast.makeText(context, "El sueldo debe ser mayor a cero", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Monto inválido", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Por favor complete todos los campos", Toast.LENGTH_SHORT).show()
            }
        }

        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.dismiss()
        }

        builder.show()
    }
}