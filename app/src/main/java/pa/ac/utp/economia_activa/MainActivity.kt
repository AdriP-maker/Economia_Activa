package pa.ac.utp.economia_activa

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputType
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
}