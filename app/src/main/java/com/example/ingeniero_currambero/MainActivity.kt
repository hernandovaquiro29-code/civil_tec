package com.example.ingeniero_currambero

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MainActivity : AppCompatActivity() {

    private lateinit var autenticacion: FirebaseAuth
    private lateinit var baseDatos: FirebaseDatabase
    private lateinit var contenPr: LinearLayout
    private lateinit var textPrVacios: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_dashboard)

        autenticacion=FirebaseAuth.getInstance()
        baseDatos=FirebaseDatabase.getInstance(
            "https://ingeniero-currambero-default-rtdb.firebaseio.com/"
        )
        val botonCerrarSesion=
            findViewById<Button>(R.id.btnLogout)
        botonCerrarSesion.setOnClickListener {
            autenticacion.signOut()
            val navegacion=Intent(
                this,
                LoginActivity::class.java
            )
            navegacion.flags=
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(navegacion)
            finish()
        }
        val botonAgregarPr=
            findViewById<Button>(R.id.btnAñadirPr)
        botonAgregarPr.setOnClickListener {
            val navegacion=Intent(
                this,
                ProjectFormActivity::class.java
            )
            startActivity(navegacion)
        }
        contenPr=
            findViewById(R.id.contenPr)
        textPrVacios=
            findViewById(R.id.tvvacioPr)
        ViewCompat.setOnApplyWindowInsetsListener(
            findViewById(android.R.id.content)
        ) { vista, inserciones ->
            val barrasSistema=inserciones.getInsets(
                WindowInsetsCompat.Type.systemBars()
            )
            vista.setPadding(
                barrasSistema.left,
                barrasSistema.top,
                barrasSistema.right,
                barrasSistema.bottom
            )
            inserciones
        }
    }
    override fun onResume() {
        super.onResume()
        cargarProyectos()
    }
    private fun cargarProyectos() {
        val usuarioActual=autenticacion.currentUser
        if (usuarioActual == null) {
            Toast.makeText(
                this,
                "Debes iniciar sesión",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        baseDatos.reference
            .child("users")
            .child(usuarioActual.uid)
            .child("projects")
            .addListenerForSingleValueEvent(
                object : ValueEventListener {
                    override fun onDataChange(
                        instantanea: DataSnapshot
                    ) {
                        contenPr.removeAllViews()

                        if (!instantanea.exists()) {
                            textPrVacios.visibility=
                                View.VISIBLE
                            return
                        }
                        textPrVacios.visibility=
                            View.GONE

                        for (proyecto in instantanea.children) {
                            val idProyecto=proyecto.key
                            if (idProyecto == null) {
                                continue
                            }
                            val nomPr=leerTextPr(
                                proyecto,
                                "nombre",
                                "nombre"
                            ) ?: "Sin nombre"
                            val descriPr=leerTextPr(
                                proyecto,
                                "descripcion",
                                "descrip",
                                "descripcion"
                            ) ?: "Sin descripción"
                            val presuPr=leerValorPr(
                                proyecto,
                                "presupuesto",
                                "Presu",
                                "presupuesto"
                            )?.toString() ?: "0"
                            val ubiPr=leerTextPr(
                                proyecto,
                                "ubicacion",
                                "ubicacion"
                            ) ?: ""
                            val tipoEdiPr=
                                leerTextPr(
                                    proyecto,
                                    "tipo_edi",
                                    "tipoEdificacion",
                                    "tipo_edificacion"
                                ) ?: ""
                            val enlacePlPr=
                                leerTextPr(
                                    proyecto,
                                    "planUrl",
                                    "urlPlano"
                                ) ?: ""
                            val tarjetaProyecto=
                                LinearLayout(this@MainActivity).apply {
                                    orientation=LinearLayout.VERTICAL
                                    setPadding(
                                        24,
                                        20,
                                        24,
                                        20
                                    )
                                    background=android.graphics.drawable.GradientDrawable().apply {
                                        setColor(Color.parseColor("@color/azul"))
                                        cornerRadius=24f
                                        setStroke(
                                            2,
                                            Color.parseColor("@color/amarillo")
                                        )
                                    }
                                }
                            val parametrosTarjeta=
                                LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                                )
                            parametrosTarjeta.setMargins(
                                0,
                                0,
                                0,
                                20
                            )
                            tarjetaProyecto.layoutParams=
                                parametrosTarjeta
                            val textNomb=
                                TextView(this@MainActivity).apply {
                                    text=nomPr
                                    textSize=20f
                                    setTextColor(
                                        Color.rgb(20, 43, 74)
                                    )
                                    setTypeface(
                                        null,
                                        Typeface.BOLD
                                    )
                                }
                            val textDescrip=
                                TextView(this@MainActivity).apply {
                                    text=descriPr
                                    textSize=15f
                                    setTextColor(Color.DKGRAY)
                                    setPadding(
                                        0,
                                        10,
                                        0,
                                        10
                                    )
                                }
                            val textPresu=
                                TextView(this@MainActivity).apply {
                                    text=
                                        "Presupuesto: $ $presuPr"
                                    textSize=16f
                                    setTextColor(
                                        Color.rgb(20, 43, 74)
                                    )
                                }
                            val botonAbrir=
                                Button(this@MainActivity).apply {
                                    text="ABRIR PROYECTO"
                                }
                            val botonEditar=
                                Button(this@MainActivity).apply {
                                    text="EDITAR PROYECTO"
                                }
                            val botonEliminar=
                                Button(this@MainActivity).apply {
                                    text="ELIMINAR PROYECTO"
                                }
                            botonAbrir.setOnClickListener {
                                val navegacion=Intent(
                                    this@MainActivity,
                                    ProjectDetailActivity::class.java
                                )
                                navegacion.putExtra(
                                    "projectId",
                                    idProyecto
                                )
                                startActivity(navegacion)
                            }
                            botonEditar.setOnClickListener {
                                val navegacion=Intent(
                                    this@MainActivity,
                                    ProjectFormActivity::class.java
                                )
                                navegacion.putExtra("proyectoId", idProyecto)
                                navegacion.putExtra("nompr", nomPr)
                                navegacion.putExtra("descrippr", descriPr)
                                navegacion.putExtra("presupr", presuPr)
                                navegacion.putExtra("ubipr", ubiPr)
                                navegacion.putExtra("edipr", tipoEdiPr)
                                navegacion.putExtra("enlaceplpr", enlacePlPr)
                                startActivity(navegacion)
                            }
                            botonEliminar.setOnClickListener {
                                AlertDialog.Builder(
                                    this@MainActivity
                                )
                                    .setTitle("Eliminar proyecto")
                                    .setMessage(
                                        "¿Estás seguro de que deseas " +
                                                "eliminar $nomPr?"
                                    )
                                    .setNegativeButton(
                                        "Cancelar",
                                        null
                                    )
                                    .setPositiveButton(
                                        "Eliminar"
                                    ) { _, _ ->
                                        val usuario=
                                            autenticacion.currentUser
                                        if (usuario != null) {
                                            baseDatos.reference
                                                .child("users")
                                                .child(usuario.uid)
                                                .child("projects")
                                                .child(idProyecto)
                                                .removeValue()
                                                .addOnSuccessListener {
                                                    Toast.makeText(
                                                        this@MainActivity,
                                                        "Proyecto eliminado correctamente",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                    cargarProyectos()
                                                }
                                                .addOnFailureListener { error ->
                                                    Toast.makeText(
                                                        this@MainActivity,
                                                        "Error al eliminar: ${error.message}",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                }
                                        }
                                    }
                                    .show()
                            }
                            tarjetaProyecto.addView(textNomb)
                            tarjetaProyecto.addView(textDescrip)
                            tarjetaProyecto.addView(textPresu)
                            tarjetaProyecto.addView(botonAbrir)
                            tarjetaProyecto.addView(botonEditar)
                            tarjetaProyecto.addView(botonEliminar)
                            contenPr.addView(tarjetaProyecto)
                        }
                    }
                    override fun onCancelled(
                        error: DatabaseError
                    ) {
                        Toast.makeText(
                            this@MainActivity,
                            "Error al cargar proyectos: ${error.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            )
    }
    private fun leerTextPr(
        proyecto: DataSnapshot,
        vararg claves: String
    ): String? {

        for (clave in claves) {
            val valor=proyecto
                .child(clave)
                .value
                ?.toString()
                ?.trim()
            if (!valor.isNullOrBlank()) {
                return valor
            }
        }
        return null
    }
    private fun leerValorPr(
        proyecto: DataSnapshot,
        vararg claves: String
    ): Any? {
        for (clave in claves) {
            val valor=proyecto.child(clave).value
            if (valor != null) {
                return valor
            }
        }
        return null
    }
}
