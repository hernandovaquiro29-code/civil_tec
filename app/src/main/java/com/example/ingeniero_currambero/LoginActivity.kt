package com.example.ingeniero_currambero

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class LoginActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private var isRegistering=false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        auth=FirebaseAuth.getInstance()

        val etNombre=findViewById<EditText>(R.id.etNombre)
        val etEmail=findViewById<EditText>(R.id.etEmail)
        val etcontrasena=findViewById<EditText>(R.id.etcontrasena)
        val btnAction=findViewById<Button>(R.id.btnAccion)
        val tvSwitch=findViewById<TextView>(R.id.tvSwitch)
        val tvTitle=findViewById<TextView>(R.id.tvTitle)
        val tvSubtitle=findViewById<TextView>(R.id.tvSubtitle)

        tvSwitch.setOnClickListener {
            isRegistering=!isRegistering
            if (isRegistering) {
                tvTitle.text="Crear cuenta"
                tvSubtitle.text="Regístrate en CIVIL-TEC"
                etNombre.visibility=View.VISIBLE
                btnAction.text="REGISTRARME"
                tvSwitch.text="¿Ya tienes cuenta? Inicia sesión"
            } else {
                tvTitle.text="Bienvenido"
                tvSubtitle.text="Inicia sesión para continuar"
                etNombre.visibility=View.GONE
                btnAction.text="INICIAR SESIÓN"
                tvSwitch.text="¿No tienes cuenta? Regístrate"
            }
        }
        btnAction.setOnClickListener {
            val nombre=etNombre.text.toString().trim()
            val email=etEmail.text.toString().trim()
            val contrasena=etcontrasena.text.toString()

            if (email.isEmpty() || contrasena.isEmpty() ||
                (isRegistering && nombre.isEmpty())) {
                Toast.makeText(
                    this,
                    "Completa todos los campos",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            if (isRegistering && contrasena.length < 8) {
                Toast.makeText(
                    this,
                    "La contraseña debe tener mínimo 8 caracteres",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            btnAction.isEnabled=false

            if (isRegistering) {
                registerUser(nombre, email, contrasena, btnAction)
            } else {
                loginUser(email, contrasena, btnAction)
            }
        }
        if (auth.currentUser != null) {
            goToMain()
        }
    }
    private fun registerUser(
        nombre: String,
        email: String,
        contrasena: String,
        button: Button
    ) {
        auth.createUserWithEmailAndPassword(email, contrasena)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid=auth.currentUser!!.uid
                    val user=hashMapOf(
                        "nombre" to nombre,
                        "email" to email
                    )
                    FirebaseDatabase.getInstance()
                        .reference
                        .child("users")
                        .child(uid)
                        .setValue(user)
                        .addOnSuccessListener {
                            Toast.makeText(
                                this,
                                "¡Bienvenido a CIVIL-TEC!",
                                Toast.LENGTH_SHORT
                            ).show()
                            goToMain()
                        }
                        .addOnFailureListener {
                            button.isEnabled=true
                            Toast.makeText(
                                this,
                                "Cuenta creada, pero no se guardaron los datos, intenta iniciar sesión nuevamente.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                } else {
                    button.isEnabled=true
                    Toast.makeText(
                        this,
                        task.exception?.localizedMessage ?: "Error al registrarse",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }
    private fun loginUser(
        email: String,
        contrasena: String,
        button: Button
    ) {
        auth.signInWithEmailAndPassword(email, contrasena)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(
                        this,
                        "Inicio de sesión exitoso",
                        Toast.LENGTH_SHORT
                    ).show()
                    goToMain()
                } else {
                    button.isEnabled=true
                    Toast.makeText(
                        this,
                        task.exception?.localizedMessage ?: "Error al iniciar sesión",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }
    private fun goToMain() {
        val intent=Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}