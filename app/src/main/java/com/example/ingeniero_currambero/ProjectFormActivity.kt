package com.example.ingeniero_currambero

import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
class ProjectFormActivity : AppCompatActivity() {

    private lateinit var autenticacion: FirebaseAuth
    private lateinit var baseDatos: FirebaseDatabase
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_project_form)

        autenticacion=FirebaseAuth.getInstance()
        baseDatos=FirebaseDatabase.getInstance(
            "https://ingeniero-currambero-default-rtdb.firebaseio.com/"
        )
        val camponom=
            findViewById<EditText>(R.id.etPrnombre)
        val campodescri=
            findViewById<EditText>(R.id.etDescrip)
        val campopresu=
            findViewById<EditText>(R.id.etpresu)
        val campoubicacion=
            findViewById<EditText>(R.id.etUbicacion)
        val campotipoedi=
            findViewById<EditText>(R.id.etTipoEdi)
        val campoenlacePl=
            findViewById<EditText>(R.id.etEnlacePl)
        val botonGuardar=
            findViewById<Button>(R.id.btnguardarpr)
        val idProyecto=intent.getStringExtra("proyectoId")

        if (idProyecto != null) {
            camponom.setText(
                intent.getStringExtra("nompr")
            )
            campodescri.setText(
                intent.getStringExtra("descrippr")
            )
            campopresu.setText(
                intent.getStringExtra("presupr")
            )
            campoubicacion.setText(
                intent.getStringExtra("ubipr")
            )
            campotipoedi.setText(
                intent.getStringExtra("edipr")
            )
            campoenlacePl.setText(
                intent.getStringExtra("enlaceplpr")
            )

            botonGuardar.text="ACTUALIZAR PROYECTO"
        }
        botonGuardar.setOnClickListener {

            val nomb=
                camponom.text.toString().trim()
            val descrip=
                campodescri.text.toString().trim()
            val presuText=
                campopresu.text.toString()
                    .trim()
                    .replace(",", ".")
            val ubicacion=
                campoubicacion.text.toString().trim()
            val tipoEdi=
                campotipoedi.text.toString().trim()
            val enlacePl=
                campoenlacePl.text.toString().trim()
            if (
                nomb.isEmpty() ||
                descrip.isEmpty() ||
                presuText.isEmpty()
            ) {
                mostrarMensaje(
                    "Completa nombre, descripción y presupuesto."
                )
                return@setOnClickListener
            }
            val presu=
                presuText.toDoubleOrNull()
            if (
                presu == null ||
                !presu.isFinite() ||
                presu < 0
            ) {
                mostrarMensaje(
                    "Ingresa un presupuesto válido."
                )
                return@setOnClickListener
            }
            if (
                enlacePl.isNotEmpty() &&
                !validarEnlace(enlacePl)
            ) {
                mostrarMensaje(
                    "Ingresa un enlace HTTP o HTTPS válido."
                )
                return@setOnClickListener
            }
            val usuario=autenticacion.currentUser

            if (usuario == null) {
                mostrarMensaje(
                    "Debes iniciar sesión nuevamente."
                )
                return@setOnClickListener
            }
            botonGuardar.isEnabled=false

            val refPr=baseDatos.reference
                .child("users")
                .child(usuario.uid)
                .child("projects")
            val referenciaPr: DatabaseReference=
                if (idProyecto != null) {
                    refPr.child(idProyecto)
                } else {
                    refPr.push()
                }
            if (referenciaPr.key == null) {
                botonGuardar.isEnabled = true
                mostrarMensaje(
                    "No se pudo identificar el proyecto."
                )
                return@setOnClickListener
            }
            val datosProyecto=hashMapOf<String, Any>(
                "nombre" to nomb,
                "descripcion" to descrip,
                "presupuesto" to presu,
                "ubicacion" to ubicacion,
                "tipo_edi" to tipoEdi,
                "planUrl" to enlacePl
            )
            guardarProyecto(
                referenciaPr,
                datosProyecto,
                botonGuardar,
                idProyecto != null
            )
        }
    }
    private fun validarEnlace(enlace: String): Boolean {
        val direccion=Uri.parse(enlace)
        val protocolo=direccion.scheme?.lowercase()
        return (
                protocolo == "https" ||
                        protocolo == "http"
                ) && !direccion.host.isNullOrBlank()
    }
    private fun guardarProyecto(
        referencia: DatabaseReference,
        datosProyecto: HashMap<String, Any>,
        botonGuardar: Button,
        esEdicion: Boolean
    ) {
        referencia.updateChildren(datosProyecto)
            .addOnSuccessListener {
                botonGuardar.isEnabled=true
                Toast.makeText(
                    this,
                    if (esEdicion) {
                        "¡Proyecto actualizado correctamente!"
                    } else {
                        "¡Proyecto guardado correctamente!"
                    },
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
            .addOnFailureListener { error ->
                botonGuardar.isEnabled=true
                mostrarMensaje(
                    "Error al guardar: ${error.message}"
                )
            }
    }
    private fun mostrarMensaje(mensaje: String) {
        Toast.makeText(
            this,
            mensaje,
            Toast.LENGTH_LONG
        ).show()
    }
}
