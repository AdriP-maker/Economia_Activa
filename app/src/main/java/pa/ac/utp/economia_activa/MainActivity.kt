package pa.ac.utp.economia_activa

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
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
        // Inflar layout moderno para el diálogo de presupuesto
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_budget, null)
        val etBudgetInput = dialogView.findViewById<TextInputEditText>(R.id.etBudgetInput)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancelBudget)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSaveBudget)

        etBudgetInput.setText(monthlyBudget.toString())

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSave.setOnClickListener {
            val budgetStr = etBudgetInput.text.toString().trim()
            if (budgetStr.isNotEmpty()) {
                try {
                    val newBudget = budgetStr.toDouble()
                    if (newBudget > 0) {
                        monthlyBudget = newBudget
                        val prefs = getSharedPreferences("economia_activa_prefs", Context.MODE_PRIVATE)
                        prefs.edit().putFloat("monthly_budget", newBudget.toFloat()).apply()
                        updateDashboard()
                        Toast.makeText(this, "Presupuesto actualizado", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    } else {
                        Toast.makeText(this, "Ingrese un valor mayor a cero", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Formato incorrecto", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Ingrese el presupuesto", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun showSettingsDialog() {
        val prefs = getSharedPreferences("economia_activa_prefs", Context.MODE_PRIVATE)
        val currentName = prefs.getString("user_name", "") ?: ""
        val currentLastname = prefs.getString("user_lastname", "") ?: ""
        val currentSalary = prefs.getFloat("user_salary", 0.0f).toDouble()
        val isBiometricEnabled = prefs.getBoolean("biometrics_enabled", false)

        // Inflar el nuevo layout moderno del diálogo de perfil
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_profile_settings, null)

        val etName = dialogView.findViewById<TextInputEditText>(R.id.etProfileName)
        val etLastname = dialogView.findViewById<TextInputEditText>(R.id.etProfileLastname)
        val etSalary = dialogView.findViewById<TextInputEditText>(R.id.etProfileSalary)
        val switchBiometric = dialogView.findViewById<SwitchCompat>(R.id.switchProfileBiometric)
        val btnDeleteData = dialogView.findViewById<Button>(R.id.btnDeleteAllData)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancelProfile)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSaveProfile)

        // Precargar datos actuales
        etName.setText(currentName)
        etLastname.setText(currentLastname)
        if (currentSalary > 0) etSalary.setText(currentSalary.toString())
        switchBiometric.isChecked = isBiometricEnabled

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // Botón Cancelar
        btnCancel.setOnClickListener { dialog.dismiss() }

        // Botón Borrar todos los datos
        btnDeleteData.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("¿Eliminar todos los datos?")
                .setMessage("Esta acción eliminará permanentemente todo tu historial, listas, facturas y configuraciones. No se puede deshacer.")
                .setPositiveButton("Sí, eliminar todo") { confirmDialog, _ ->
                    dbHelper.clearAllData()
                    prefs.edit().clear().apply()
                    Toast.makeText(this, "Todos los datos eliminados", Toast.LENGTH_SHORT).show()
                    confirmDialog.dismiss()
                    dialog.dismiss()
                    val restartIntent = Intent(this, Home::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    }
                    startActivity(restartIntent)
                    finish()
                }
                .setNegativeButton("Cancelar") { confirmDialog, _ -> confirmDialog.dismiss() }
                .show()
        }

        // Botón Guardar
        btnSave.setOnClickListener {
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
                        Toast.makeText(this, "Datos actualizados", Toast.LENGTH_SHORT).show()
                        updateDashboard()
                        dialog.dismiss()
                    } else {
                        Toast.makeText(this, "El sueldo debe ser mayor a cero", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Monto inválido", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Por favor complete todos los campos", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }
}