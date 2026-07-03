package pa.ac.utp.economia_activa.ui.reports

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import pa.ac.utp.economia_activa.R
import pa.ac.utp.economia_activa.data.DatabaseHelper
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ReportsActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var categoryChartView: CategoryChartView
    private lateinit var layoutLegend: LinearLayout

    // Mapeo de colores idénticos a los del CategoryChartView para la leyenda
    private val categoryColors = mapOf(
        "Alimentos y Super" to Color.parseColor("#2E7D32"),
        "Servicios Básicos" to Color.parseColor("#1565C0"),
        "Transporte" to Color.parseColor("#EF6C00"),
        "Salud" to Color.parseColor("#C62828"),
        "Entretenimiento / Ocio" to Color.parseColor("#6A1B9A"),
        "Otros Gastos" to Color.parseColor("#455A64")
    )
    
    private val defaultColor = Color.parseColor("#9E9E9E")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reports)

        dbHelper = DatabaseHelper(this)

        // Botón regresar
        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        categoryChartView = findViewById(R.id.categoryChartView)
        layoutLegend = findViewById(R.id.layoutLegend)

        loadReportData()
    }

    private fun loadReportData() {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val currentMonthStr = dateFormat.format(calendar.time)

        // Obtener datos
        val data = dbHelper.getExpensesByCategory(currentMonthStr)

        // Pasar al gráfico
        categoryChartView.setData(data)

        // Limpiar leyenda
        layoutLegend.removeAllViews()

        val total = data.values.sum()

        if (data.isEmpty() || total == 0.0) {
            val tvNoData = TextView(this).apply {
                text = "Registra compras o gastos para ver el análisis de consumo aquí."
                setTextColor(getColor(R.color.colorTextSecondary))
                textSize = 14f
                gravity = Gravity.CENTER
                setPadding(0, 40, 0, 40)
            }
            layoutLegend.addView(tvNoData)
        } else {
            // Ordenar por mayor monto
            val sortedData = data.toList().sortedByDescending { (_, value) -> value }.toMap()

            for ((category, amount) in sortedData) {
                if (amount <= 0) continue

                val percentage = (amount / total) * 100
                val color = categoryColors[category] ?: defaultColor

                // Crear fila de leyenda
                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(0, 8, 0, 8)
                    }
                    gravity = Gravity.CENTER_VERTICAL
                }

                // Círculo indicador de color
                val dotDrawable = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(color)
                }

                val indicator = View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(16.dpToPx(), 16.dpToPx()).apply {
                        setMargins(0, 0, 12.dpToPx(), 0)
                    }
                    background = dotDrawable
                }

                // Nombre Categoría
                val tvName = TextView(this).apply {
                    text = category
                    setTextColor(getColor(R.color.colorTextPrimary))
                    textSize = 14f
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f)
                }

                // Porcentaje e Importe
                val tvValue = TextView(this).apply {
                    text = String.format(Locale.getDefault(), "%.1f%%  ($%.2f)", percentage, amount)
                    setTextColor(getColor(R.color.colorTextSecondary))
                    textSize = 14f
                    gravity = Gravity.END
                    setTypeface(null, android.graphics.Typeface.BOLD)
                }

                row.addView(indicator)
                row.addView(tvName)
                row.addView(tvValue)

                layoutLegend.addView(row)
            }
        }
    }

    // Extensión simple para convertir dp a pixeles
    private fun Int.dpToPx(): Int {
        val density = resources.displayMetrics.density
        return (this * density).toInt()
    }
}
