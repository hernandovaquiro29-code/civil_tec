package com.example.ingeniero_currambero

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class CalculosEstructuralesActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calculos_estructurales)


        val idProyecto=intent.getStringExtra("projectId")
        val btnCargas=findViewById<Button>(R.id.btnCargas)
        val btnSismico=findViewById<Button>(R.id.btnSismico)
        val btnEstructura=findViewById<Button>(R.id.btnEstructura)
        btnCargas.setOnClickListener {
            abrirModulo(
                DeterminacionDeCargasActivity::class.java,
                idProyecto
            )
        }
        btnSismico.setOnClickListener {
            abrirModulo(
                AnalisisSismicoActivity::class.java,
                idProyecto
            )
        }
        btnEstructura.setOnClickListener {
            abrirModulo(
                AnalisisEstructuralActivity::class.java,
                idProyecto
            )
        }
    }
    private fun abrirModulo(
        actividad: Class<*>,
        idProyecto: String?
    ) {
        if (idProyecto.isNullOrBlank()) {
            mostrarMensaje("No se encontró el proyecto")
            return
        }
        val navegacion=Intent(this, actividad)
        navegacion.putExtra("projectId", idProyecto)
        startActivity(navegacion)
    }
    private fun mostrarMensaje(mensaje: String) {
        Toast.makeText(
            this,
            mensaje,
            Toast.LENGTH_SHORT
        ).show()
    }
}
