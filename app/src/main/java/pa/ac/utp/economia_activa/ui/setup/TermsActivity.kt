package pa.ac.utp.economia_activa.ui.setup

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import androidx.appcompat.app.AppCompatActivity
import pa.ac.utp.economia_activa.R

class TermsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_terms)

        val chkAcceptTerms = findViewById<CheckBox>(R.id.chkAcceptTerms)
        val btnAcceptTerms = findViewById<Button>(R.id.btnAcceptTerms)
        val btnDeclineTerms = findViewById<Button>(R.id.btnDeclineTerms)

        chkAcceptTerms.setOnCheckedChangeListener { _, isChecked ->
            btnAcceptTerms.isEnabled = isChecked
        }

        btnAcceptTerms.setOnClickListener {
            val prefs = getSharedPreferences("economia_activa_prefs", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("terms_accepted", true).apply()

            // Ir a la pantalla de configuración de perfil
            startActivity(Intent(this, SetupActivity::class.java))
            finish()
        }

        btnDeclineTerms.setOnClickListener {
            finishAffinity() // Cerrar la app por completo
        }
    }
}
