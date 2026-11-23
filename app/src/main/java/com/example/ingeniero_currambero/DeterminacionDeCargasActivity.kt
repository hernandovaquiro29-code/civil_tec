package com.example.ingeniero_currambero

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.util.Locale

class DeterminacionDeCargasActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_determinacion_de_cargas)
        val campoVolumen=
            findViewById<EditText>(R.id.etVolumen)
        val campoDensidad=
            findViewById<EditText>(R.id.etDensidad)
        val campoCargaMuertaAdicional=
            findViewById<EditText>(R.id.etCargaMuertaAdicional)
        val campoCargaViva=
            findViewById<EditText>(R.id.etCarga_viva)
        val campoCargaCubierta=
            findViewById<EditText>(R.id.etCargaCubierta)
        val campoCargaGranizo=
            findViewById<EditText>(R.id.etCargaGranizo)
        val campoCargaLluvia=
            findViewById<EditText>(R.id.etCargaLluvia)
        val selectorTipoCubierta=
            findViewById<RadioGroup>(R.id.rgTipoCubierta)
        val campoCargaViento=
            findViewById<EditText>(R.id.etCargaViento)
        val campoCargaSismica=
            findViewById<EditText>(R.id.etCargaSismo)
        val botonCalcular=
            findViewById<Button>(R.id.btnCalcularCargas)
        val textoResultados=
            findViewById<TextView>(R.id.tvFormulas)
        botonCalcular.setOnClickListener {

            val volumen=leerNumero(campoVolumen)
            val densidad=leerNumero(campoDensidad)
            val cargaViva=leerNumero(campoCargaViva)

            if (volumen == null ||
                densidad == null ||
                cargaViva == null
            ) {
                mostrarError(
                    "Completa volumen, peso específico y carga viva."
                )
                return@setOnClickListener
            }
            if (volumen <= 0 ||
                densidad <= 0 ||
                cargaViva < 0
            ) {
                mostrarError(
                    "Verifica los valores de los campos principales."
                )
                return@setOnClickListener
            }
            val cargaMuertaAdicional: Double
            if (campoCargaMuertaAdicional.text.toString().isBlank()) {
                cargaMuertaAdicional=0.0
            } else {
                val valorAdicional=
                    leerNumero(campoCargaMuertaAdicional)
                if (valorAdicional == null || valorAdicional < 0) {
                    mostrarError(
                        "Verifica las cargas muertas adicionales."
                    )
                    return@setOnClickListener
                }
                cargaMuertaAdicional=valorAdicional
            }
            val seleccionCubierta=when (
                selectorTipoCubierta.checkedRadioButtonId
            ) {
                R.id.rbLr ->
                    campoCargaCubierta to "Carga viva de cubierta (Lr)"
                R.id.rbGranizo ->
                    campoCargaGranizo to "Carga de granizo (G)"
                R.id.rbLluvia ->
                    campoCargaLluvia to "Carga de lluvia (Le)"
                else ->
                    null to "Ninguna"
            }
            val campoCubierta=seleccionCubierta.first
            val nombreCargaCubierta=seleccionCubierta.second
            val cargaCubierta: Double

            if (campoCubierta == null) {
                cargaCubierta = 0.0
            } else {
                val valorCubierta=leerNumero(campoCubierta)
                if (valorCubierta == null || valorCubierta < 0) {
                    mostrarError(
                        "Ingresa un valor válido para $nombreCargaCubierta."
                    )
                    return@setOnClickListener
                }
                cargaCubierta=valorCubierta
            }
            val cargaViento: Double?
            if (campoCargaViento.text.toString().isBlank()) {
                cargaViento=null
            } else {
                cargaViento=leerNumero(campoCargaViento)
                if (cargaViento == null) {
                    mostrarError("Verifica el valor de viento.")
                    return@setOnClickListener
                }
            }
            val cargaSismica: Double?
            if (campoCargaSismica.text.toString().isBlank()) {
                cargaSismica=null
            } else {
                cargaSismica=leerNumero(campoCargaSismica)
                if (cargaSismica == null) {
                    mostrarError("Verifica el valor sísmico.")
                    return@setOnClickListener
                }
            }
            val pesoPropio=volumen * densidad
            val cargaMuertaTotal=
                pesoPropio + cargaMuertaAdicional
            if (!cargaMuertaTotal.isFinite()) {
                mostrarError(
                    "Los valores ingresados son demasiado grandes."
                )
                return@setOnClickListener
            }
            val resultados=buildString {
                appendLine("RESULTADOS DE DETERMINACIÓN DE CARGAS")
                appendLine()
                appendLine("CARGA MUERTA (D)")
                appendLine(
                    "Volumen: ${formatearNum(volumen)} m³"
                )
                appendLine(
                    "Peso específico: ${formatearNum(densidad)} kN/m³"
                )
                appendLine(
                    "Peso propio: ${formatearNum(pesoPropio)} kN"
                )
                appendLine(
                    "Cargas permanentes adicionales: " +
                            "${formatearNum(cargaMuertaAdicional)} kN"
                )
                appendLine(
                    "CARGA MUERTA TOTAL (D): " +
                            "${formatearNum(cargaMuertaTotal)} kN"
                )
                appendLine()
                appendLine("CARGA VIVA (L)")
                appendLine("${formatearNum(cargaViva)} kN")

                appendLine()
                appendLine("ACCIÓN DE CUBIERTA")
                appendLine(nombreCargaCubierta)
                appendLine("${formatearNum(cargaCubierta)} kN")

                appendLine()
                appendLine("VIENTO (W)")
                appendLine(
                    cargaViento?.let { valor ->
                        "${formatearNum(valor)} kN"
                    } ?: "No ingresado"
                )
                appendLine()
                appendLine("SISMO (E)")
                appendLine(
                    cargaSismica?.let { valor ->
                        "${formatearNum(valor)} kN"
                    } ?: "No ingresado"
                )
                appendLine()
                appendLine("ESTADO DEL PROCESAMIENTO")
                appendLine("Datos validados correctamente.")
                appendLine("Carga muerta calculada.")
            }
            textoResultados.text=resultados
            guardarResulFirebase(resultados)
        }
    }
    private fun guardarResulFirebase(resultado: String) {
        val usuario=FirebaseAuth.getInstance().currentUser
        val idProyecto=intent.getStringExtra("projectId")
        if (usuario == null || idProyecto.isNullOrBlank()) {
            mostrarError(
                "No se pudo guardar: falta el usuario o el proyecto."
            )
            return
        }
        val referencia=FirebaseDatabase.getInstance(
            "https://ingeniero-currambero-default-rtdb.firebaseio.com/"
        ).reference
            .child("users")
            .child(usuario.uid)
            .child("projects")
            .child(idProyecto)
            .child("resultadosCalculo")
            .child("determinacionCargas")
        val registro=hashMapOf<String, Any>(
            "modulo" to "Determinación de cargas",
            "resultadoTexto" to resultado,
            "fecha" to System.currentTimeMillis()
        )
        val idRegistro=referencia.child("historial").push().key
        if (idRegistro == null) {
            mostrarError("No se pudo generar el registro del historial.")
            return
        }
        val actualizacion=mapOf(
            "ultimoResultado" to registro,
            "historial/$idRegistro" to registro
        )
        referencia.updateChildren(actualizacion)
            .addOnSuccessListener {
                Toast.makeText(
                    this,
                    "Resultado guardado en el proyecto.",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener {
                mostrarError(
                    "No se pudo guardar el resultado en Firebase."
                )
            }
    }
    private fun leerNumero(campo: EditText): Double? {
        val texto=campo.text.toString()
            .trim()
            .replace(",", ".")
        if (texto.isBlank()) return null
        return texto.toDoubleOrNull()
            ?.takeIf { numero -> numero.isFinite() }
    }
    private fun formatearNum(numero: Double): String {
        return String.format(
            Locale("es", "CO"),
            "%.2f",
            numero
        )
    }
    private fun mostrarError(mensaje: String) {
        Toast.makeText(
            this,
            mensaje,
            Toast.LENGTH_SHORT
        ).show()
    }
}
