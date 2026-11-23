package com.example.ingeniero_currambero

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.Locale

class ProjectDetailActivity : AppCompatActivity() {

    private lateinit var autenticacion: FirebaseAuth
    private lateinit var baseDatos: FirebaseDatabase
    private var refPr: DatabaseReference?=null
    private var escuchaPr: ValueEventListener?=null
    private lateinit var textNom: TextView
    private lateinit var textDescrip: TextView
    private lateinit var textPresu: TextView
    private lateinit var textUbi: TextView
    private lateinit var textTipoEdi: TextView
    private lateinit var textImagen: ImageView
    private lateinit var textEstadoPl: TextView
    private lateinit var botonAbrirPl: Button
    private var urlPl: String?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_project_detail)

        autenticacion=FirebaseAuth.getInstance()
        baseDatos=FirebaseDatabase.getInstance(
            "https://ingeniero-currambero-default-rtdb.firebaseio.com/"
        )
        textNom=findViewById(R.id.tvDetPronombre)
        textDescrip=findViewById(R.id.tvDetDes)
        textPresu=findViewById(R.id.tvDetPresu)
        textUbi=findViewById(R.id.tvDetUbicacion)
        textTipoEdi=
            findViewById(R.id.tvDetTipoEdificacion)
        textImagen=findViewById(R.id.tvImagenPr)
        textEstadoPl=findViewById(R.id.tvEstadoPl)
        botonAbrirPl=findViewById(R.id.btnAbrirPl)

        val botonCalcu=
            findViewById<Button>(R.id.btncalcuabrir)
        val botonResul=
            findViewById<Button>(R.id.btnCalcuResu)
        val idProyecto = intent.getStringExtra("projectId")
        if (idProyecto.isNullOrBlank()) {
            mostrarMensaje("No se encontró el proyecto")
            finish()
            return
        }

        botonCalcu.setOnClickListener {
            val navegacion=Intent(
                this,
                CalculosEstructuralesActivity::class.java
            )
            navegacion.putExtra("projectId", idProyecto)
            startActivity(navegacion)
        }
        botonResul.setOnClickListener {
            val navegacion=Intent(
                this,
                ResultadosHistorialActivity::class.java
            )
            navegacion.putExtra("projectId", idProyecto)
            startActivity(navegacion)
        }
        val usuario=autenticacion.currentUser
        if (usuario == null) {
            mostrarMensaje("Debes iniciar sesión")
            finish()
            return
        }
        refPr=baseDatos.reference
            .child("users")
            .child(usuario.uid)
            .child("projects")
            .child(idProyecto)
        escuchaPr=object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    textNom.text="Proyecto no encontrado"
                    textDescrip.text=""
                    textPresu.text="Presupuesto: --"
                    textUbi.text="Ubicación: --"
                    textTipoEdi.text=
                        "Tipo de edificación: --"
                    textImagen.setImageResource(R.drawable.imagen_predeterminada)
                    urlPl=null
                    botonAbrirPl.isEnabled=false
                    textEstadoPl.text="No hay planos registrados"
                    return
                }
                val nom=obtenerTexto(
                    snapshot,
                    "name",
                    "nombre"
                )
                textNom.text=
                    nom ?: "Nombre no definido"

                val descrip=obtenerTexto(
                    snapshot,
                    "description",
                    "descrip",
                    "descripcion"
                )
                textDescrip.text=
                    descrip ?: "Descripción no registrada"

                val presupuesto=obtenerValor(
                    snapshot,
                    "budget",
                    "Presu",
                    "presupuesto"
                )
                textPresu.text=
                    "Presupuesto: ${formatearPresupuesto(presupuesto)}"

                val ubi=obtenerTexto(
                    snapshot,
                    "location",
                    "ubicacion"
                )
                textUbi.text=
                    "Ubicación: ${ubi ?: "No registrada"}"
                val tipoEdificacion=obtenerTexto(
                    snapshot,
                    "tipo_edi",
                    "buildingType",
                    "tipoEdificacion",
                    "tipo_edificacion",
                    "tipodeedificacion"
                )
                textTipoEdi.text=
                    "Tipo de edificación: " +
                            (tipoEdificacion ?: "No registrado")
                cargarImagenSegunTipo(tipoEdificacion)
                val imagenUrl=obtenerTexto(
                    snapshot,
                    "imageUrl",
                    "imagenUrl"
                )
                urlPl=obtenerTexto(
                    snapshot,
                    "planUrl",
                    "urlPlano"
                )
                if (urlPl.isNullOrBlank()) {
                    botonAbrirPl.isEnabled=false
                    textEstadoPl.text="No hay planos registrados"
                } else {
                    botonAbrirPl.isEnabled=true
                    textEstadoPl.text="Plano disponible"
                }
            }
            override fun onCancelled(error: DatabaseError) {
                mostrarMensaje(
                    "Error al sincronizar: ${error.message}"
                )
            }
        }
        refPr?.addValueEventListener(
            escuchaPr!!
        )
        botonAbrirPl.setOnClickListener {
            val enlace=urlPl
            if (enlace.isNullOrBlank()) {
                mostrarMensaje("No hay ningún plano disponible")
            } else {
                try {
                    val navegacion=Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(enlace)
                    )
                    startActivity(navegacion)
                } catch (error: Exception) {
                    mostrarMensaje(
                        "No se pudo abrir el plano"
                    )
                }
            }
        }
    }
    private fun cargarImagenSegunTipo(tipoEdificacion: String?) {
        val tipo=tipoEdificacion
            ?.lowercase(Locale.getDefault())
            ?.trim() ?: ""
        when {
            tipo.contains("puente") -> {
                textImagen.setImageResource(R.drawable.img)
            }
            tipo.contains("edificio") ||
                    tipo.contains("vivienda") ||
                    tipo.contains("residencial") ||
                    tipo.contains("casa") -> {
                textImagen.setImageResource(R.drawable.imagen_vivienda)
            }
            tipo.contains("vía") ||
                    tipo.contains("via") ||
                    tipo.contains("carretera") -> {
                textImagen.setImageResource(R.drawable.imagen_via)
            }
            tipo.contains("industrial") ||
                    tipo.contains("nave") -> {
                textImagen.setImageResource(R.drawable.edificios_industriales)
            }
            else -> {
                textImagen.setImageResource(R.drawable.imagen_predeterminada)
            }
        }
    }
    private fun obtenerTexto(
        instantanea: DataSnapshot,
        vararg claves: String
    ): String? {
        for (clave in claves) {
            val valor=instantanea.child(clave)
                .value
                ?.toString()
                ?.trim()
            if (!valor.isNullOrBlank()) {
                return valor
            }
        }
        return null
    }
    private fun obtenerValor(
        instantanea: DataSnapshot,
        vararg claves: String
    ): Any? {
        for (clave in claves) {
            val valor=instantanea.child(clave).value
            if (valor != null) {
                return valor
            }
        }
        return null
    }
    private fun formatearPresupuesto(valor: Any?): String {
        if (valor == null) {
            return "No registrado"
        }
        val numero=valor.toString().toDoubleOrNull()
        return if (numero != null) {
            "$ " + String.format(
                Locale("es", "CO"),
                "%,.2f",
                numero
            )
        } else {
            valor.toString()
        }
    }
    private fun mostrarMensaje(mensaje: String) {
        Toast.makeText(
            this,
            mensaje,
            Toast.LENGTH_SHORT
        ).show()
    }
    override fun onDestroy() {
        super.onDestroy()
        if (refPr != null &&
            escuchaPr != null
        ) {
            refPr?.removeEventListener(
                escuchaPr!!
            )
        }
    }
}
