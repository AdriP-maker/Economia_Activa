package pa.ac.utp.economia_activa.ui.setup

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import pa.ac.utp.economia_activa.MainActivity
import pa.ac.utp.economia_activa.R

class BiometricLoginActivity : AppCompatActivity() {

    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_biometric_login)

        val tvGreeting = findViewById<TextView>(R.id.tvBiometricGreeting)
        val btnUnlock = findViewById<Button>(R.id.btnUnlock)

        val prefs = getSharedPreferences("economia_activa_prefs", Context.MODE_PRIVATE)
        val userName = prefs.getString("user_name", "")
        val userLastname = prefs.getString("user_lastname", "")
        tvGreeting.text = "Hola, $userName $userLastname.\nPor favor autentícate para ingresar."

        // Verificar si los datos biométricos están configurados y disponibles
        val biometricManager = BiometricManager.from(this)
        val canAuth = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )

        if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) {
            // Si el hardware no soporta o no hay huellas registradas, saltar de forma segura
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        val executor = ContextCompat.getMainExecutor(this)
        biometricPrompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                Toast.makeText(this@BiometricLoginActivity, "Error: $errString", Toast.LENGTH_SHORT).show()
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                Toast.makeText(this@BiometricLoginActivity, "Bienvenido", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this@BiometricLoginActivity, MainActivity::class.java))
                finish()
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                Toast.makeText(this@BiometricLoginActivity, "Autenticación fallida", Toast.LENGTH_SHORT).show()
            }
        })

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Acceso Seguro")
            .setSubtitle("Usa tus datos biométricos para ingresar")
            .setNegativeButtonText("Cancelar")
            .build()

        // Lanzar de forma automática
        biometricPrompt.authenticate(promptInfo)

        btnUnlock.setOnClickListener {
            biometricPrompt.authenticate(promptInfo)
        }
    }
}
