package pa.ac.utp.economia_activa.ui.setup

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import pa.ac.utp.economia_activa.MainActivity
import pa.ac.utp.economia_activa.R

class SetupActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup)

        val etName = findViewById<EditText>(R.id.etSetupName)
        val etLastname = findViewById<EditText>(R.id.etSetupLastname)
        val etSalary = findViewById<EditText>(R.id.etSetupSalary)
        val switchBiometric = findViewById<SwitchCompat>(R.id.switchSetupBiometric)
        val btnSave = findViewById<Button>(R.id.btnSaveSetup)

        btnSave.setOnClickListener {
            val name = etName.text.toString().trim()
            val lastname = etLastname.text.toString().trim()
            val salaryStr = etSalary.text.toString().trim()
            val biometricEnabled = switchBiometric.isChecked

            if (name.isEmpty() || lastname.isEmpty() || salaryStr.isEmpty()) {
                Toast.makeText(this, "Por favor complete todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val namePattern = Regex("^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+\$")
            if (!name.matches(namePattern) || !lastname.matches(namePattern)) {
                Toast.makeText(this, "El nombre y apellido solo deben contener letras", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            try {
                val salary = salaryStr.toDouble()
                if (salary <= 0) {
                    Toast.makeText(this, "El sueldo debe ser mayor a cero", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                // Guardar en SharedPreferences
                val prefs = getSharedPreferences("economia_activa_prefs", Context.MODE_PRIVATE)
                prefs.edit().apply {
                    putString("user_name", name)
                    putString("user_lastname", lastname)
                    putFloat("user_salary", salary.toFloat())
                    putFloat("monthly_budget", salary.toFloat()) // Presupuesto mensual por defecto igual al sueldo
                    putBoolean("biometrics_enabled", biometricEnabled)
                    putBoolean("setup_complete", true)
                    apply()
                }

                Toast.makeText(this, "Perfil configurado con éxito", Toast.LENGTH_SHORT).show()

                // Ir al menú principal
                startActivity(Intent(this, MainActivity::class.java))
                finish()

            } catch (e: Exception) {
                Toast.makeText(this, "Formato de sueldo inválido", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
