package pa.ac.utp.economia_activa.ui.tips

import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import pa.ac.utp.economia_activa.R
import pa.ac.utp.economia_activa.data.DatabaseHelper
import android.speech.tts.TextToSpeech
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class SavingsTipsActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var layoutTipsContainer: LinearLayout
    private var monthlyBudget = 500.0
    private var tts: TextToSpeech? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_savings_tips)

        dbHelper = DatabaseHelper(this)

        // Inicializar TextToSpeech
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale.forLanguageTag("es-ES"))
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.language = Locale.getDefault()
                }
            } else {
                Toast.makeText(this, "No se pudo iniciar el asistente de voz", Toast.LENGTH_SHORT).show()
            }
        }

        // Cargar presupuesto guardado
        val prefs = getSharedPreferences("economia_activa_prefs", Context.MODE_PRIVATE)
        monthlyBudget = prefs.getFloat("monthly_budget", 500.0f).toDouble()

        // Botón regresar
        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        layoutTipsContainer = findViewById(R.id.layoutTipsContainer)

        generateTips()
    }

    private fun generateTips() {
        layoutTipsContainer.removeAllViews()
        val inflater = LayoutInflater.from(this)

        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val currentMonthStr = dateFormat.format(calendar.time)
        
        val spent = dbHelper.getMonthlySpent(currentMonthStr)
        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        // 1. ANÁLISIS DE INFLACIÓN (Diferencia de Precios en SQLite)
        val priceAnalysis = dbHelper.getProductPriceAnalysis()
        var inflationTipAdded = false
        for (item in priceAnalysis) {
            val name = item.first
            val avgPrice = item.second
            val lastPrice = item.third
            if (lastPrice > avgPrice * 1.05) { // Alza mayor al 5%
                val diffPercent = ((lastPrice - avgPrice) / avgPrice * 100).toInt()
                val title = "📈 Alerta de Inflación: $name"
                val desc = "El precio de '$name' subió a $${String.format(Locale.getDefault(), "%.2f", lastPrice)} (un incremento del $diffPercent% sobre tu promedio histórico de $${String.format(Locale.getDefault(), "%.2f", avgPrice)}). Te sugerimos buscar una marca alternativa o sustituto genérico para ahorrar."
                addTipView(inflater, title, desc, R.drawable.ic_wallet, getColor(R.color.colorDanger))
                inflationTipAdded = true
                break // Solo mostrar el alza más crítica
            }
        }
        if (!inflationTipAdded) {
            val title = "💡 Consejo de Canasta Básica"
            val desc = "Al realizar tus compras del mercado, prioriza la adquisición de frutas y verduras frescas de temporada en ferias locales. Comprar local te permite ahorrar hasta un 30% en comparación con grandes cadenas de supermercados."
            addTipView(inflater, title, desc, R.drawable.ic_wallet, getColor(R.color.colorPrimary))
        }

        // 2. CONTROL Y RITMO DE PRESUPUESTO (Dinámico con límite mensual)
        if (monthlyBudget > 0) {
            val consumptionPercent = (spent / monthlyBudget) * 100
            val expectedPercent = (dayOfMonth.toDouble() / daysInMonth.toDouble()) * 100

            if (consumptionPercent > expectedPercent + 10.0) { // Ritmo acelerado
                val title = "⚠️ Alerta de Ritmo de Gasto"
                val desc = "Has consumido el ${consumptionPercent.toInt()}% ($${String.format(Locale.getDefault(), "%.2f", spent)}) de tu límite mensual de $${String.format(Locale.getDefault(), "%.2f", monthlyBudget)} en solo $dayOfMonth días. Te sugerimos congelar consumos no esenciales en ocio para evitar sobregiros antes de fin de mes."
                addTipView(inflater, title, desc, R.drawable.ic_chart, getColor(R.color.colorWarning))
            } else { // Progreso óptimo
                val remaining = monthlyBudget - spent
                val title = "✅ Progreso de Presupuesto Excelente"
                val desc = "Llevas gastado el ${consumptionPercent.toInt()}% de tu presupuesto mensual en el día $dayOfMonth del mes. Tu proyección de consumo es óptima, dejándote un saldo libre estimado de $${String.format(Locale.getDefault(), "%.2f", remaining)}. ¡Buen trabajo de planificación!"
                addTipView(inflater, title, desc, R.drawable.ic_check, getColor(R.color.colorPrimary))
            }
        } else {
            val title = "📊 Establece un Límite Mensual"
            val desc = "Aún no has definido tu presupuesto mensual en el perfil. Los usuarios que fijan una meta de gasto logran reducir sus compras compulsivas y ahorrar un promedio de 18% más al mes. ¡Establece tu meta hoy!"
            addTipView(inflater, title, desc, R.drawable.ic_chart, getColor(R.color.colorSecondary))
        }

        // 3. ANÁLISIS DE COMPRAS RECURRENTES (Ahorro por lote)
        val popularProducts = dbHelper.getPopularProducts()
        if (popularProducts.isNotEmpty()) {
            val firstPopular = popularProducts[0]
            val title = "📦 Sugerencia de Compra por Lote: ${firstPopular.name}"
            val desc = "Hemos detectado que compras '${firstPopular.name}' de forma muy recurrente. Para optimizar el ahorro familiar, te aconsejamos comprar la presentación familiar o cajas al por mayor. Esto reduce el costo unitario hasta en un 15%."
            addTipView(inflater, title, desc, R.drawable.ic_tips, getColor(R.color.colorSecondary))
        } else {
            val title = "📝 Planificación Inteligente de Compras"
            val desc = "Antes de ir de compras, usa siempre el módulo de 'Organizar Compras' para registrar tus necesidades reales. Planificar tus artículos evita que caigas en ofertas engañosas de último minuto."
            addTipView(inflater, title, desc, R.drawable.ic_tips, getColor(R.color.colorSecondary))
        }

        // 4. AUDITORÍA DE GASTOS FIJOS Y SERVICIOS
        val fixedExpenses = dbHelper.getAllPeriodicExpenses()
        if (fixedExpenses.isNotEmpty()) {
            val totalFixed = fixedExpenses.sumOf { it.amount }
            val fixedCount = fixedExpenses.size
            val title = "🔍 Auditoría de Gastos Fijos ($fixedCount Servicios)"
            val desc = "Tus servicios y pagos fijos (ej. Luz, Internet) suman $${String.format(Locale.getDefault(), "%.2f", totalFixed)} mensuales. Considera revisar tus facturas para identificar cargos ocultos, seguros duplicados o negociar tarifas con tus proveedores."
            addTipView(inflater, title, desc, R.drawable.ic_cart, getColor(R.color.colorSecondary))
        } else {
            val title = "📡 Registra tus Facturas Recurrentes"
            val desc = "Añade tus facturas fijas de electricidad, agua, internet o suscripciones en la pestaña 'Servicios Fijos'. Con esto, la aplicación te enviará alertas de vencimiento para evitar recargos por mora."
            addTipView(inflater, title, desc, R.drawable.ic_cart, getColor(R.color.colorSecondary))
        }

        // 5. RETO DE AHORRO SEMANAL (Gamificación)
        val challengeAmount = if (monthlyBudget > 0) (monthlyBudget * 0.05).toInt() else 20
        val title = "🎯 Reto Semanal: Cocina en Casa"
        val desc = "Te retamos a preparar todas tus cenas en casa durante los próximos 5 días, evitando pedir comida a domicilio o comer fuera. Al cumplir este reto familiar, acumularás un ahorro estimado de $${challengeAmount}.00."
        addTipView(inflater, title, desc, R.drawable.ic_add, getColor(R.color.colorAccent))
    }

    private fun addTipView(inflater: LayoutInflater, title: String, desc: String, iconRes: Int, iconColor: Int) {
        val tipView = inflater.inflate(R.layout.item_savings_tip, layoutTipsContainer, false)
        val ivIcon = tipView.findViewById<ImageView>(R.id.ivTipIcon)
        val tvTitle = tipView.findViewById<TextView>(R.id.tvTipTitle)
        val tvDesc = tipView.findViewById<TextView>(R.id.tvTipDescription)
        val btnListen = tipView.findViewById<ImageView>(R.id.btnListenTip)

        tvTitle.text = title
        tvDesc.text = desc
        ivIcon.setImageResource(iconRes)
        ivIcon.imageTintList = ColorStateList.valueOf(iconColor)

        btnListen.setOnClickListener {
            speakTip(title, desc)
        }

        layoutTipsContainer.addView(tipView)
    }

    private fun speakTip(title: String, description: String) {
        val textToSpeak = "$title. $description"
        tts?.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, "TipSpeakID")
    }

    override fun onDestroy() {
        if (tts != null) {
            tts?.stop()
            tts?.shutdown()
        }
        super.onDestroy()
    }
}
