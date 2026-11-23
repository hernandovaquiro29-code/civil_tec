package com.example.ingeniero_currambero

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ResultadosHistorialActivity : AppCompatActivity() {

    private lateinit var autenticacion: FirebaseAuth
    private lateinit var baseDatos: FirebaseDatabase
    private lateinit var contenHisto: LinearLayout
    private lateinit var textEsta: TextView
    private lateinit var textNomPr: TextView
    private var refPr: DatabaseReference?=null
    private var escuchaPr: ValueEventListener?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_resultados_historial)

        autenticacion=FirebaseAuth.getInstance()
        baseDatos=FirebaseDatabase.getInstance(
            "https://ingeniero-currambero-default-rtdb.firebaseio.com/"
        )
        contenHisto=findViewById(R.id.contenHisto)
        textEsta=findViewById(R.id.tvEstadoHisto)
        textNomPr=findViewById(R.id.tvNomPrHisto)

        val botonVolver=findViewById<Button>(R.id.btnVolverHisto)
        botonVolver.setOnClickListener {
            finish()
        }
        val idProyecto=intent.getStringExtra("projectId")
        if (idProyecto.isNullOrBlank()) {
            mostrarMensaje("No se encontró el proyecto")
            finish()
            return
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

        escucharDatosPr()
    }
    private fun escucharDatosPr() {
        escuchaPr=object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    mostrarMensaje("No se encontró la información del proyecto")
                    return
                }
                contenHisto.removeAllViews()

                val nombre=obtenerTexto(
                    snapshot,
                    "name",
                    "nombre"
                ) ?: "Proyecto seleccionado"

                textNomPr.text=nombre

                mostrarInfoPr(snapshot)
                mostrarInfoPl(snapshot)
                mostrarResulCargas(snapshot)
                mostrarResulSismicos(snapshot)
                mostrarResulEstructurales(snapshot)
            }
            override fun onCancelled(error: DatabaseError) {
                mostrarMensaje(
                    "Error al cargar el proyecto: ${error.message}"
                )
            }
        }
        refPr?.addValueEventListener(escuchaPr!!)
    }
    private fun mostrarInfoPr(snapshot: DataSnapshot) {
        agregarTituloSeccion("INFORMACIÓN GENERAL DEL PROYECTO")
        val nombre=obtenerTexto(
            snapshot,
            "name",
            "nombre"
        )
        val descrip=obtenerTexto(
            snapshot,
            "description",
            "descrip",
            "descripcion"
        )
        val ubica=obtenerTexto(
            snapshot,
            "location",
            "ubicacion"
        )
        val presu=obtenerTexto(
            snapshot,
            "budget",
            "Presu",
            "presupuesto"
        )
        val tipoEdi=obtenerTexto(
            snapshot,
            "buildingType",
            "tipoEdificacion",
            "tipo_edificacion",
            "tipo_edi"
        )
        val tarjeta=crearTarjeta()

        agregarTextoTarjeta(
            tarjeta,
            "Nombre: ${nombre ?: "--"}",
            true
        )
        agregarTextoTarjeta(
            tarjeta,
            "Descripción: ${descrip ?: "--"}"
        )
        agregarTextoTarjeta(
            tarjeta,
            "Ubicación: ${ubica ?: "--"}"
        )
        agregarTextoTarjeta(
            tarjeta,
            "Presupuesto: ${presu ?: "--"}"
        )
        agregarTextoTarjeta(
            tarjeta,
            "Tipo de edificación: ${tipoEdi ?: "--"}"
        )
        contenHisto.addView(tarjeta)
        agregarSeparador()
    }
    private fun mostrarInfoPl(snapshot: DataSnapshot) {
        agregarTituloSeccion("PLANOS DEL PROYECTO")

        val tarjeta=crearTarjeta()
        val nombrePlano=obtenerTexto(
            snapshot,
            "planName",
            "nombrePlano",
            "planNombre"
        )
        val estadoPl=obtenerTexto(
            snapshot,
            "planStatus",
            "estadoPlano"
        )
        val urlPl=obtenerTexto(
            snapshot,
            "planUrl",
            "urlPlano"
        )
        agregarTextoTarjeta(
            tarjeta,
            "Nombre del plano: ${nombrePlano ?: "No registrado"}",
            true
        )
        agregarTextoTarjeta(
            tarjeta,
            "Estado: ${estadoPl ?: if (urlPl.isNullOrBlank()) "Sin plano cargado" else "Plano disponible"}"
        )
        if (!urlPl.isNullOrBlank()) {

            val botonAbrirPl=Button(this)
            botonAbrirPl.text="ABRIR PLANO"
            botonAbrirPl.setOnClickListener {
                try {
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(urlPl)
                    )
                    startActivity(intent)
                } catch (e: Exception) {
                    mostrarMensaje(
                        "No fue posible abrir el plano."
                    )
                }
            }
            tarjeta.addView(botonAbrirPl)
        } else {
            agregarTextoTarjeta(
                tarjeta,
                "Todavía no hay un enlace de plano guardado."
            )
        }
        contenHisto.addView(tarjeta)
        agregarSeparador()
    }
    private fun mostrarResulCargas(snapshot: DataSnapshot) {
        agregarTituloSeccion("DETERMINACIÓN DE CARGAS")

        val ref=snapshot
            .child("resultadosCalculo")
            .child("determinacionCargas")
        mostrarUlResulHistorial(
            ref,
            "Determinación de cargas"
        )
    }
    private fun mostrarResulSismicos(snapshot: DataSnapshot) {
        agregarTituloSeccion("ANÁLISIS SÍSMICO Y DINÁMICO")

        val ref=snapshot
            .child("resultadosCalculo")
            .child("analisisSismico")
        val tarjeta=crearTarjeta()
        val resulEspectro=buscarTextClave(
            ref,
            "resultadoEspectroTexto"
        )
        val fuerzaHorizontal=ref
            .child("fuerzaHorizontalEquivalente")
            .child("resultadoTexto")
            .getValue(String::class.java)
        var hayResultados=false

        if (!resulEspectro.isNullOrBlank()) {
            agregarTextoTarjeta(
                tarjeta,
                "ESPECTRO ELÁSTICO",
                true
            )
            agregarTextoTarjeta(
                tarjeta,
                resulEspectro
            )
            hayResultados = true
        }
        if (!fuerzaHorizontal.isNullOrBlank()) {
            agregarTextoTarjeta(
                tarjeta,
                "FUERZA HORIZONTAL EQUIVALENTE",
                true
            )
            agregarTextoTarjeta(
                tarjeta,
                fuerzaHorizontal
            )
            hayResultados = true
        }
        if (!hayResultados) {
            agregarTextoTarjeta(
                tarjeta,
                "Todavía no hay resultados sísmicos guardados."
            )
        }
        contenHisto.addView(tarjeta)
        agregarSeparador()
    }
    private fun mostrarResulEstructurales(snapshot: DataSnapshot) {
        agregarTituloSeccion("ANÁLISIS ESTRUCTURAL")

        val ref=snapshot
            .child("resultadosCalculo")
            .child("analisisEstructural")
        agregarSubtitulo("VIGAS")
        mostrarUlResulHistorial(
            ref.child("vigas"),
            "Vigas"
        )
        agregarSubtitulo("COLUMNAS")
        mostrarUlResulHistorial(
            ref.child("columnas"),
            "Columnas"
        )
        agregarSubtitulo("PÓRTICOS")
        mostrarUlResulHistorial(
            ref.child("porticos"),
            "Pórticos"
        )
    }
    private fun mostrarUlResulHistorial(
        referencia: DataSnapshot,
        nombreModulo: String
    ) {
        val ulResul=referencia.child("ultimoResultado")
        val historial=referencia.child("historial")

        if (!ulResul.exists() && !historial.hasChildren()) {
            val tarjetaVacia = crearTarjeta()
            agregarTextoTarjeta(
                tarjetaVacia,
                "Todavía no hay resultados guardados para $nombreModulo."
            )
            contenHisto.addView(tarjetaVacia)
            agregarSeparador()
            return
        }
        if (ulResul.exists()) {
            val tarjeta=crearTarjeta()
            agregarTextoTarjeta(
                tarjeta,
                "ÚLTIMO RESULTADO",
                true
            )
            val resultadoTexto=ulResul
                .child("resultadoTexto")
                .getValue(String::class.java)
            val fecha=obtenerFecha(
                ulResul.child("fecha")
            )
            agregarTextoTarjeta(
                tarjeta,
                "Fecha: $fecha"
            )
            agregarTextoTarjeta(
                tarjeta,
                resultadoTexto ?: "No hay texto de resultado registrado."
            )
            contenHisto.addView(tarjeta)
            agregarSeparador()
        }
        if (historial.hasChildren()) {
            agregarSubtitulo("HISTORIAL DE $nombreModulo".uppercase(Locale.ROOT))

            val registros=historial.children.toList().asReversed()
            for (registro in registros) {
                val fechaRegistro=obtenerFecha(
                    registro.child("fecha")
                )
                val fechaUl=obtenerFecha(
                    ulResul.child("fecha")
                )
                if (
                    ulResul.exists() &&
                    fechaRegistro == fechaUl
                ) {
                    continue
                }
                val resulText=registro
                    .child("resultadoTexto")
                    .getValue(String::class.java)
                val tarjeta=crearTarjeta()
                agregarTextoTarjeta(
                    tarjeta,
                    "Registro de $nombreModulo",
                    true
                )
                agregarTextoTarjeta(
                    tarjeta,
                    "Fecha: $fechaRegistro"
                )
                agregarTextoTarjeta(
                    tarjeta,
                    resulText ?: "No hay texto de resultado registrado."
                )
                contenHisto.addView(tarjeta)
                agregarSeparador()
            }
        }
    }
    private fun buscarTextClave(
        snapshot: DataSnapshot,
        clave: String
    ): String? {

        if (snapshot.key == clave) {
            val valor=snapshot.getValue(String::class.java)
            if (!valor.isNullOrBlank()) {
                return valor
            }
        }
        for (hijo in snapshot.children) {
            val resul=buscarTextClave(
                hijo,
                clave
            )
            if (!resul.isNullOrBlank()) {
                return resul
            }
        }
        return null
    }
    private fun obtenerTexto(
        snapshot: DataSnapshot,
        vararg claves: String
    ): String? {

        for (clave in claves) {
            val valor=snapshot
                .child(clave)
                .value
                ?.toString()
            if (!valor.isNullOrBlank()) {
                return valor
            }
        }
        return null
    }
    private fun obtenerFecha(snapshot: DataSnapshot): String {
        val valor=snapshot.value
        if (valor == null) {
            return "Fecha no registrada"
        }
        return try {
            val tiempo=valor.toString().toLong()
            val formato=SimpleDateFormat(
                "dd/MM/yyyy HH:mm",
                Locale.getDefault()
            )
            formato.format(Date(tiempo))
        } catch (e: Exception) {
            valor.toString()
        }
    }
    private fun agregarTituloSeccion(titulo: String) {
        val texto=crearTexto(
            titulo,
            18,
            true,
            "@color/azul"
        )
        texto.setPadding(0, 12, 0, 12)
        contenHisto.addView(texto)
    }
    private fun agregarSubtitulo(subtitulo: String) {
        val texto=crearTexto(
            subtitulo,
            16,
            true,
            "@color/azul"
        )
        texto.setPadding(0, 8, 0, 8)
        contenHisto.addView(texto)
    }
    private fun crearTarjeta(): LinearLayout {

        return LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL
            setPadding(18, 18, 18, 18)
            setBackgroundColor(
                0xFFFFFFFF.toInt()
            )
        }
    }
    private fun agregarTextoTarjeta(
        tarjeta: LinearLayout,
        contenido: String,
        negrita: Boolean=false
    ) {
        tarjeta.addView(
            crearTexto(
                contenido,
                if (negrita) 16 else 14,
                negrita,
                if (negrita) "@color/azul" else "@color/texto_de_input"
            )
        )
    }
    private fun agregarSeparador() {
        val separador=TextView(this).apply {
            text=""
            height=18
        }
        contenHisto.addView(separador)
    }
    private fun crearTexto(
        contenido: String,
        tamano: Int,
        negrita: Boolean,
        color: String
    ): TextView {
        return TextView(this).apply {
            text=contenido
            setTextSize(
                android.util.TypedValue.COMPLEX_UNIT_SP,
                tamano.toFloat()
            )
            val colorId=resources.getIdentifier(
                color.substringAfterLast('/'),
                "color",
                packageName
            )
            setTextColor(
                if (colorId != 0) {
                    androidx.core.content.ContextCompat.getColor(
                        this@ResultadosHistorialActivity,
                        colorId
                    )
                } else {
                    android.graphics.Color.BLACK
                }
            )
            if (negrita) {
                setTypeface(
                    null,
                    android.graphics.Typeface.BOLD
                )
            }
            setPadding(0, 5, 0, 5)
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
        if (refPr != null && escuchaPr != null) {
            refPr?.removeEventListener(
                escuchaPr!!
            )
        }
    }
}