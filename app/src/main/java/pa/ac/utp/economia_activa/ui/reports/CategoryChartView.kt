package pa.ac.utp.economia_activa.ui.reports

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import java.util.Locale

class CategoryChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paintArc = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 50f // Grosor del anillo del gráfico de dona
        strokeCap = Paint.Cap.BUTT
    }

    private val paintInnerCircle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private val paintTextAmount = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#212121")
        textAlign = Paint.Align.CENTER
        textSize = 48f
        isFakeBoldText = true
    }

    private val paintTextLabel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#757575")
        textAlign = Paint.Align.CENTER
        textSize = 28f
    }

    private val rectF = RectF()
    private var dataMap: Map<String, Double> = emptyMap()
    private var totalAmount = 0.0

    // Colores asignados a cada categoría estándar
    private val categoryColors = mapOf(
        "Alimentos y Super" to Color.parseColor("#2E7D32"), // Verde
        "Servicios Básicos" to Color.parseColor("#1565C0"), // Azul
        "Transporte" to Color.parseColor("#EF6C00"),        // Naranja
        "Salud" to Color.parseColor("#C62828"),             // Rojo
        "Entretenimiento / Ocio" to Color.parseColor("#6A1B9A"), // Púrpura
        "Otros Gastos" to Color.parseColor("#455A64")       // Gris Azulado
    )
    
    private val defaultColor = Color.parseColor("#9E9E9E")

    fun setData(data: Map<String, Double>) {
        dataMap = data
        totalAmount = data.values.sum()
        invalidate() // Redibujar la vista
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width.toFloat()
        val height = height.toFloat()
        val size = if (width < height) width else height
        val margin = 60f
        
        // Configurar rectángulo para el gráfico de dona
        rectF.set(
            margin + paintArc.strokeWidth / 2,
            margin + paintArc.strokeWidth / 2,
            size - margin - paintArc.strokeWidth / 2,
            size - margin - paintArc.strokeWidth / 2
        )

        // Centrar en el canvas
        canvas.save()
        canvas.translate((width - size) / 2, (height - size) / 2)

        if (dataMap.isEmpty() || totalAmount == 0.0) {
            // Dibujar un anillo gris por defecto si no hay datos
            paintArc.color = Color.parseColor("#E0E0E0")
            canvas.drawArc(rectF, 0f, 360f, false, paintArc)
            
            // Dibujar texto en el centro
            canvas.drawText("$0.00", size / 2, size / 2 + 10f, paintTextAmount)
            canvas.drawText("Sin Gastos", size / 2, size / 2 + 50f, paintTextLabel)
        } else {
            var startAngle = -90f // Empezar en la parte superior (-90 grados)

            for ((category, amount) in dataMap) {
                if (amount <= 0) continue

                val percentage = (amount / totalAmount).toFloat()
                val sweepAngle = percentage * 360f

                // Asignar color según la categoría
                paintArc.color = categoryColors[category] ?: defaultColor
                
                // Dibujar arco de la categoría
                // Dejamos un espacio de 2 grados entre arcos para un aspecto moderno
                val gap = if (dataMap.size > 1) 2.5f else 0f
                canvas.drawArc(rectF, startAngle + gap / 2, sweepAngle - gap, false, paintArc)

                startAngle += sweepAngle
            }

            // Dibujar el texto del total en el centro
            val totalStr = String.format(Locale.getDefault(), "$%.2f", totalAmount)
            
            // Ajustar tamaño del texto dinámicamente si es muy largo
            if (totalStr.length > 8) {
                paintTextAmount.textSize = 38f
            } else {
                paintTextAmount.textSize = 48f
            }

            canvas.drawText(totalStr, size / 2, size / 2 + 10f, paintTextAmount)
            canvas.drawText("Total Gastado", size / 2, size / 2 + 50f, paintTextLabel)
        }
        
        canvas.restore()
    }
}
