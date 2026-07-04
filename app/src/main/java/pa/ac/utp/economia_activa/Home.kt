package pa.ac.utp.economia_activa

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import pa.ac.utp.economia_activa.ui.setup.BiometricLoginActivity
import pa.ac.utp.economia_activa.ui.setup.SetupActivity
import pa.ac.utp.economia_activa.ui.setup.TermsActivity

class Home : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Delay de 1 segundo y luego enrutar dinámicamente
        Handler(Looper.getMainLooper()).postDelayed({
            val prefs = getSharedPreferences("economia_activa_prefs", Context.MODE_PRIVATE)
            val termsAccepted = prefs.getBoolean("terms_accepted", false)
            val setupComplete = prefs.getBoolean("setup_complete", false)
            val biometricsEnabled = prefs.getBoolean("biometrics_enabled", false)

            val intent = when {
                !termsAccepted -> Intent(this, TermsActivity::class.java)
                !setupComplete -> Intent(this, SetupActivity::class.java)
                biometricsEnabled -> Intent(this, BiometricLoginActivity::class.java)
                else -> Intent(this, MainActivity::class.java)
            }
            startActivity(intent)
            finish() // para que no regrese al splash
        }, 1000)
    }
}