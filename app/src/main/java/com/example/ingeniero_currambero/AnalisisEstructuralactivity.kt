package com.example.ingeniero_currambero

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sqrt

class AnalisisEstructuralActivity : AppCompatActivity() {
    private data class CargaViga(
        val tipo: String,
        val magnitud: Double,
        val posicionInicial: Double,
        val posicionFinal: Double
    )

    private val listaCargas=mutableListOf<CargaViga>()
    private lateinit var etLongitudViga: EditText
    private lateinit var etMagnitudCargaViga: EditText
    private lateinit var etPosicionCargaViga: EditText
    private lateinit var rbCargaPuntual: RadioButton
    private lateinit var rbVoladizo: RadioButton
    private lateinit var tvResultadoViga: TextView
    private lateinit var etModuloElasticidadViga: EditText
    private lateinit var etInerciaViga: EditText

    private data class ElementoPortico(
        val nombre: String,
        val nodoInicial: Int,
        val nodoFinal: Int,
        val area: Double,
        val inercia: Double
    )
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_analisis_estructural)

        val btnVigas=findViewById<Button>(R.id.btnAnalisisVigas)
        val btnColumnas=findViewById<Button>(R.id.btnAnalisisColumnas)
        val btnPorticos=findViewById<Button>(R.id.btnAnalisisPorticos)
        val vigas=findViewById<LinearLayout>(R.id.contenedorVigas)
        val columnas=findViewById<LinearLayout>(R.id.contenedorColumnas)
        val porticos=findViewById<LinearLayout>(R.id.contenedorPorticos)

        etLongitudViga=findViewById(R.id.etLongitudViga)
        etMagnitudCargaViga=findViewById(R.id.etMagnitudCargaViga)
        etPosicionCargaViga=findViewById(R.id.etPosicionCargaViga)
        rbCargaPuntual=findViewById(R.id.rbCargaPuntual)
        rbVoladizo=findViewById(R.id.rbVoladizo)
        tvResultadoViga=findViewById(R.id.tvResultadoViga)
        etModuloElasticidadViga=findViewById(R.id.etModuloElasticidadViga)
        etInerciaViga=findViewById(R.id.etInerciaViga)

        fun ocultarModulos() {
            vigas.visibility=View.GONE
            columnas.visibility=View.GONE
            porticos.visibility=View.GONE
        }
        btnVigas.setOnClickListener {
            ocultarModulos()
            vigas.visibility=View.VISIBLE
        }
        btnColumnas.setOnClickListener {
            ocultarModulos()
            columnas.visibility=View.VISIBLE
        }
        btnPorticos.setOnClickListener {
            ocultarModulos()
            porticos.visibility=View.VISIBLE
        }
        findViewById<Button>(R.id.btnCalcularViga).setOnClickListener {
            calcularViga()
        }
        findViewById<Button>(R.id.btnAgregarCarga).setOnClickListener {
            agregarCarga()
        }
        findViewById<Button>(R.id.btnCalcularColumna).setOnClickListener {
            calcularColumna()
        }
        findViewById<Button>(R.id.btnCalcularPortico).setOnClickListener {
            calcularPortico()
        }
    }
    private fun numero(campo: EditText): Double? {
        return campo.text.toString()
            .trim()
            .replace(",", ".")
            .toDoubleOrNull()
    }
    private fun agregarCarga() {

        val longitud=numero(etLongitudViga)
        val magnitud=numero(findViewById<EditText>(R.id.etNuevaMagnitud))
        val inicio=numero(findViewById<EditText>(R.id.etNuevaPosicion))
        val esDistribuida=
            findViewById<RadioButton>(R.id.rbNuevaDistribuida).isChecked
        val posicionFinal= if (esDistribuida) {
            numero(findViewById<EditText>(R.id.etNuevaFinal))
        } else {
            inicio
        }

        if (
            longitud == null || magnitud == null ||
            inicio == null || posicionFinal == null ||
            longitud <= 0 || magnitud <= 0 ||
            inicio < 0 || posicionFinal < inicio ||
            posicionFinal > longitud ||
            (esDistribuida && posicionFinal <= inicio)
        ) {
            Toast.makeText(
                this,
                "Verifica los datos de la carga",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        val tipo= if (esDistribuida) "Distribuida" else "Puntual"
        listaCargas.add(
            CargaViga(tipo, magnitud, inicio, posicionFinal)
        )
        mostrarCargas()
        findViewById<EditText>(R.id.etNuevaMagnitud).text.clear()
        findViewById<EditText>(R.id.etNuevaPosicion).text.clear()
        findViewById<EditText>(R.id.etNuevaFinal).text.clear()
    }
    private fun mostrarCargas() {
        val contenedor=
            findViewById<LinearLayout>(R.id.contenedorListaCargas)
        contenedor.removeAllViews()

        listaCargas.forEachIndexed { indice, carga ->
            val texto=TextView(this)

            texto.text=if (carga.tipo == "Puntual") {
                "Carga ${indice + 1}: ${carga.magnitud} kN en ${carga.posicionInicial} m"
            } else {
                "Carga ${indice + 1}: ${carga.magnitud} kN/m desde ${carga.posicionInicial} m hasta ${carga.posicionFinal} m"
            }
            texto.textSize=14f
            texto.setPadding(10, 15, 10, 15)

            val botonEliminar=Button(this)
            botonEliminar.text="ELIMINAR"

            botonEliminar.setOnClickListener {
                listaCargas.removeAt(indice)
                mostrarCargas()
            }
            contenedor.addView(texto)
            contenedor.addView(botonEliminar)
        }
    }
    private fun calcularViga() {
        val longitud=numero(etLongitudViga) ?: 0.0

        if (longitud <= 0.0) {
            etLongitudViga.error="Ingresa una longitud válida"
            return
        }
        val cargas=listaCargas.toMutableList()
        val magnitudInicial=numero(etMagnitudCargaViga)

        if (magnitudInicial != null) {
            if (magnitudInicial <= 0.0) {
                etMagnitudCargaViga.error = "La carga debe ser positiva"
                return
            }
            if (rbCargaPuntual.isChecked) {
                val posicion=numero(etPosicionCargaViga)

                if (posicion == null || posicion < 0 || posicion > longitud) {
                    etPosicionCargaViga.error = "Posición inválida"
                    return
                }
                cargas.add(
                    CargaViga(
                        "Puntual",
                        magnitudInicial,
                        posicion,
                        posicion
                    )
                )
            } else {
                cargas.add(
                    CargaViga(
                        "Distribuida",
                        magnitudInicial,
                        0.0,
                        longitud
                    )
                )
            }
        }
        if (cargas.isEmpty()) {
            tvResultadoViga.text="Agrega al menos una carga."
            return
        }
        for (carga in cargas) {
            if (
                carga.magnitud <= 0 ||
                carga.posicionInicial < 0 ||
                carga.posicionFinal > longitud ||
                carga.posicionFinal < carga.posicionInicial
            ) {
                tvResultadoViga.text="Hay una carga con datos inválidos."
                return
            }
            if (
                carga.tipo == "Distribuida" &&
                carga.posicionFinal <= carga.posicionInicial
            ) {
                tvResultadoViga.text=
                    "Revisa el tramo de una carga distribuida."
                return
            }
        }
        var cargaTotal=0.0
        var momentoTotal=0.0

        for (carga in cargas) {
            val fuerza: Double
            val posicion: Double

            if (carga.tipo == "Puntual") {
                fuerza=carga.magnitud
                posicion=carga.posicionInicial
            } else {
                fuerza=carga.magnitud *
                        (carga.posicionFinal - carga.posicionInicial)
                posicion=
                    (carga.posicionInicial + carga.posicionFinal) / 2.0
            }
            cargaTotal += fuerza
            momentoTotal += fuerza * posicion
        }
        val esVoladizo=rbVoladizo.isChecked
        val reaccionA: Double
        val reaccionB: Double

        if (esVoladizo) {
            reaccionA=cargaTotal
            reaccionB=0.0
        } else {
            reaccionB=momentoTotal / longitud
            reaccionA=cargaTotal - reaccionB
        }
        var cortanteMaximo=0.0
        var momentoMaximo=0.0
        val pasos=2000

        for (i in 0..pasos) {
            val x = longitud * i / pasos
            var cortante=if (esVoladizo) 0.0 else reaccionA
            var momento=if (esVoladizo) 0.0 else reaccionA * x
            for (carga in cargas) {
                if (carga.tipo == "Puntual") {
                    val p=carga.magnitud
                    val a=carga.posicionInicial

                    if (esVoladizo) {
                        if (a >= x) {
                            cortante += p
                            momento += p * (a - x)
                        }
                    } else {
                        if (a <= x) {
                            cortante -= p
                            momento -= p * (x - a)
                        }
                    }

                } else {
                    val w=carga.magnitud
                    val a=carga.posicionInicial
                    val b=carga.posicionFinal

                    if (esVoladizo) {
                        val inicio=maxOf(a, x)
                        if (b > inicio) {
                            val fuerza=w * (b - inicio)
                            val centro=(inicio + b) / 2.0
                            cortante += fuerza
                            momento += fuerza * (centro - x)
                        }
                    } else {
                        val fin=minOf(x, b)
                        if (fin > a) {
                            val fuerza=w * (fin - a)
                            val centro=(a + fin) / 2.0
                            cortante -= fuerza
                            momento -= fuerza * (x - centro)
                        }
                    }
                }
            }
            cortanteMaximo=maxOf(cortanteMaximo, abs(cortante))
            momentoMaximo=maxOf(momentoMaximo, abs(momento))
        }
        val elasticidad=numero(etModuloElasticidadViga)
        val inercia=numero(etInerciaViga)
        var textoDeflexion="Deflexión no calculada: ingresa E e I."

        if (elasticidad != null || inercia != null) {
            if (elasticidad == null || elasticidad <= 0) {
                etModuloElasticidadViga.error =
                    "Ingresa un módulo de elasticidad válido"
                return
            }
            if (inercia == null || inercia <= 0) {
                etInerciaViga.error =
                    "Ingresa un momento de inercia válido"
                return
            }
            val resultadoDeflexion = calcularDeflexion(
                cargas,
                longitud,
                esVoladizo,
                reaccionA,
                elasticidad,
                inercia
            )
            textoDeflexion="""
                Deflexión máxima:
                ${formato(resultadoDeflexion.first)} mm
                Ubicación:
                ${formato(resultadoDeflexion.second)} m
            """.trimIndent()
        }
        val momentoApoyo=if (esVoladizo) momentoTotal else 0.0

        tvResultadoViga.text="""
            ANÁLISIS DE VIGA
            Tipo: ${if (esVoladizo) "Voladizo" else "Simplemente apoyada"}
            Longitud: ${formato(longitud)} m
            Número de cargas: ${cargas.size}
            Carga total:
            ${formato(cargaTotal)} kN
            Reacción vertical A:
            ${formato(reaccionA)} kN
            Reacción vertical B:
            ${formato(reaccionB)} kN
            Momento en el empotramiento:
            ${formato(momentoApoyo)} kN·m
            Cortante máximo aproximado:
            ${formato(cortanteMaximo)} kN
            Momento máximo aproximado:
            ${formato(momentoMaximo)} kN·m
            $textoDeflexion
        """.trimIndent()

        guardarResultadoEstructural("vigas", tvResultadoViga.text.toString())
    }
    private fun calcularDeflexion(
        cargas: List<CargaViga>,
        longitud: Double,
        esVoladizo: Boolean,
        reaccionA: Double,
        E: Double,
        I: Double
    ): Pair<Double, Double> {

        val pasos=2000
        val dx=longitud / pasos
        val rigidez=E * I
        val curvatura=DoubleArray(pasos + 1)
        val pendiente=DoubleArray(pasos + 1)
        val desplazamiento=DoubleArray(pasos + 1)

        for (i in 0..pasos) {
            val x=i * dx
            var momento=0.0
            for (carga in cargas) {
                if (carga.tipo == "Puntual") {
                    val p=carga.magnitud
                    val a=carga.posicionInicial

                    if (esVoladizo) {
                        if (a >= x) {
                            momento += p * (a - x)
                        }
                    } else {
                        if (a <= x) {
                            momento += p * (x - a)
                        }
                    }
                } else {
                    val w=carga.magnitud
                    val a=carga.posicionInicial
                    val b=carga.posicionFinal

                    if (esVoladizo) {
                        val inicio=maxOf(a, x)

                        if (b > inicio) {
                            val fuerza=w * (b - inicio)
                            val centro=(inicio + b) / 2.0
                            momento += fuerza * (centro - x)
                        }
                    } else {
                        val fin=minOf(x, b)

                        if (fin > a) {
                            val fuerza=w * (fin - a)
                            val centro=(a + fin) / 2.0
                            momento += fuerza * (x - centro)
                        }
                    }
                }
            }
            if (!esVoladizo) {
                momento=reaccionA * x - momento
                curvatura[i]=-momento / rigidez
            } else {
                curvatura[i]=momento / rigidez
            }
        }

        for (i in 1..pasos) {
            pendiente[i]=pendiente[i - 1] +
                    (curvatura[i] + curvatura[i - 1]) * dx / 2.0
            desplazamiento[i]=desplazamiento[i - 1] +
                    (pendiente[i] + pendiente[i - 1]) * dx / 2.0
        }
        if (!esVoladizo) {
            val correccion=desplazamiento[pasos] / longitud

            for (i in 0..pasos) {
                desplazamiento[i] -= correccion * (i * dx)
            }
        }
        var maxima=0.0
        var posicionMaxima=0.0
        for (i in 0..pasos) {
            val valor=abs(desplazamiento[i])

            if (valor > maxima) {
                maxima=valor
                posicionMaxima=i * dx
            }
        }
        return Pair(maxima * 1000.0, posicionMaxima)
    }
    private fun calcularColumna() {
        val ancho=numero(findViewById<EditText>(R.id.etAnchoColumna))
        val alto=numero(findViewById<EditText>(R.id.etAltoColumna))
        val longitud=numero(findViewById<EditText>(R.id.etLongitudColumna))
        val carga=numero(findViewById<EditText>(R.id.etCargaAxial))
        val elasticidad=numero(findViewById<EditText>(R.id.etElasticidadColumna))
        val factorK=numero(findViewById<EditText>(R.id.etFactorKColumna))
        val resultado=
            findViewById<TextView>(R.id.tvResultadoColumna)
        if (
            ancho == null || alto == null ||
            longitud == null || carga == null ||
            elasticidad == null || factorK == null
        ) {
            resultado.text = "Completa todos los campos."
            return
        }
        if (
            ancho <= 0 || alto <= 0 ||
            longitud <= 0 || carga <= 0 ||
            elasticidad <= 0 || factorK <= 0
        ) {
            resultado.text="Todos los valores deben ser positivos."
            return
        }
        val area=ancho * alto
        val ix=ancho * alto * alto * alto / 12.0
        val iy=alto * ancho * ancho * ancho / 12.0
        val inerciaMinima=minOf(ix, iy)
        val radioGiro=sqrt(inerciaMinima / area)
        val esfuerzo=carga / area
        val esbeltez=factorK * longitud / radioGiro
        val longitudEfectiva=factorK * longitud
        val cargaCritica=
            (PI * PI * elasticidad * inerciaMinima) /
                    (longitudEfectiva * longitudEfectiva)
        val relacion=carga / cargaCritica
        val estado=if (carga >= cargaCritica) {
            "ADVERTENCIA: la carga alcanza o supera la carga crítica ideal."
        } else {
            "La carga está por debajo de la carga crítica ideal."
        }
        resultado.text = """
            ANÁLISIS DE COLUMNA
            Área transversal:
            ${formato(area)} m²
            Inercia X:
            ${formato(ix)} m⁴
            Inercia Y:
            ${formato(iy)} m⁴
            Radio de giro mínimo:
            ${formato(radioGiro)} m
            Esfuerzo axial:
            ${formato(esfuerzo)} kN/m²
            Esbeltez:
            ${formato(esbeltez)}
            Carga crítica de Euler:
            ${formato(cargaCritica)} kN
            Relación P/Pcr:
            ${formato(relacion)}
            $estado
            Modelo ideal de Euler. No sustituye el diseño normativo.
        """.trimIndent()
        guardarResultadoEstructural("columnas", resultado.text.toString())
    }
    private fun calcularPortico() {
        val altura=numero(findViewById<EditText>(R.id.etAlturaPortico))
        val luz=numero(findViewById<EditText>(R.id.etLuzPortico))
        val elasticidad=numero(findViewById<EditText>(R.id.etElasticidadPortico))
        val inerciaViga=
            numero(findViewById<EditText>(R.id.etInerciaVigaPortico))
        val inerciaColumna=
            numero(findViewById<EditText>(R.id.etInerciaColumnaPortico))
        val areaViga=
            numero(findViewById<EditText>(R.id.etAreaVigaPortico))
        val areaColumna=
            numero(findViewById<EditText>(R.id.etAreaColumnaPortico))
        val cargaDistribuida=
            numero(findViewById<EditText>(R.id.etCargaDistribuidaPortico)) ?: 0.0
        val cargaHorizontal=
            numero(findViewById<EditText>(R.id.etCargaHorizontalPortico)) ?: 0.0
        val resultado=
            findViewById<TextView>(R.id.tvResultadoPortico)
        if (
            altura == null || luz == null || elasticidad == null ||
            inerciaViga == null || inerciaColumna == null ||
            areaViga == null || areaColumna == null
        ) {
            resultado.text="Completa todos los datos geométricos y de sección."
            return
        }
        if (
            altura <= 0 || luz <= 0 || elasticidad <= 0 ||
            inerciaViga <= 0 || inerciaColumna <= 0 ||
            areaViga <= 0 || areaColumna <= 0 ||
            cargaDistribuida < 0 || cargaHorizontal < 0
        ) {
            resultado.text="Verifica los valores. Las propiedades deben ser positivas y las cargas no negativas."
            return
        }
        try {
            val elementos=listOf(
                ElementoPortico(
                    "Columna izquierda",
                    0, 2,
                    areaColumna,
                    inerciaColumna
                ),
                ElementoPortico(
                    "Viga",
                    2, 3,
                    areaViga,
                    inerciaViga
                ),
                ElementoPortico(
                    "Columna derecha",
                    1, 3,
                    areaColumna,
                    inerciaColumna
                )
            )
            val coordenadas=arrayOf(
                Pair(0.0, 0.0),
                Pair(luz, 0.0),
                Pair(0.0, altura),
                Pair(luz, altura)
            )
            val cantidadDof=12
            val rigidezGlobal=
                Array(cantidadDof) { DoubleArray(cantidadDof) }
            val fuerzasGlobal=DoubleArray(cantidadDof)
            val cargasLocales=mutableMapOf<String, DoubleArray>()

            for (elemento in elementos) {
                val xi=coordenadas[elemento.nodoInicial].first
                val yi=coordenadas[elemento.nodoInicial].second
                val xf=coordenadas[elemento.nodoFinal].first
                val yf=coordenadas[elemento.nodoFinal].second

                val dx=xf - xi
                val dy=yf - yi
                val longitudElemento=sqrt(dx * dx + dy * dy)
                val c=dx / longitudElemento
                val s=dy / longitudElemento
                val kLocal=rigidezLocalPortico(
                    elasticidad,
                    elemento.area,
                    elemento.inercia,
                    longitudElemento
                )
                val transformacion=transformacionPortico(c, s)
                val kGlobalElemento=multiplicarMatrices(
                    transponer(transformacion),
                    multiplicarMatrices(kLocal, transformacion)
                )
                val indices=intArrayOf(
                    elemento.nodoInicial * 3,
                    elemento.nodoInicial * 3 + 1,
                    elemento.nodoInicial * 3 + 2,
                    elemento.nodoFinal * 3,
                    elemento.nodoFinal * 3 + 1,
                    elemento.nodoFinal * 3 + 2
                )
                for (i in 0 until 6) {
                    for (j in 0 until 6) {
                        rigidezGlobal[indices[i]][indices[j]] +=
                            kGlobalElemento[i][j]
                    }
                }
                val cargaLocal=if (elemento.nombre == "Viga") {
                    doubleArrayOf(
                        0.0,
                        -cargaDistribuida * longitudElemento / 2.0,
                        -cargaDistribuida * longitudElemento * longitudElemento / 12.0,
                        0.0,
                        -cargaDistribuida * longitudElemento / 2.0,
                        cargaDistribuida * longitudElemento * longitudElemento / 12.0
                    )
                } else {
                    DoubleArray(6)
                }
                cargasLocales[elemento.nombre]=cargaLocal
                val cargaGlobalElemento=multiplicarVector(
                    transponer(transformacion),
                    cargaLocal
                )
                for (i in 0 until 6) {
                    fuerzasGlobal[indices[i]] += cargaGlobalElemento[i]
                }
            }
            fuerzasGlobal[9] += cargaHorizontal

            val libres=intArrayOf(6, 7, 8, 9, 10, 11)
            val rigidezReducida=
                Array(6) { DoubleArray(6) }
            val fuerzasReducidas=DoubleArray(6)

            for (i in libres.indices) {
                fuerzasReducidas[i]=fuerzasGlobal[libres[i]]
                for (j in libres.indices) {
                    rigidezReducida[i][j]=
                        rigidezGlobal[libres[i]][libres[j]]
                }
            }
            val desplazamientosLibres=
                resolverSistema(rigidezReducida, fuerzasReducidas)
            val desplazamientos=DoubleArray(cantidadDof)
            for (i in libres.indices) {
                desplazamientos[libres[i]]=desplazamientosLibres[i]
            }
            val fuerzasCalculadas=
                multiplicarVector(rigidezGlobal, desplazamientos)
            val reacciones=DoubleArray(cantidadDof)
            for (i in 0 until cantidadDof) {
                reacciones[i]=fuerzasCalculadas[i] - fuerzasGlobal[i]
            }
            val resultadosElementos=StringBuilder()

            for (elemento in elementos) {
                val xi=coordenadas[elemento.nodoInicial].first
                val yi=coordenadas[elemento.nodoInicial].second
                val xf=coordenadas[elemento.nodoFinal].first
                val yf=coordenadas[elemento.nodoFinal].second
                val dx=xf - xi
                val dy=yf - yi
                val longitudElemento=sqrt(dx * dx + dy * dy)
                val c=dx / longitudElemento
                val s=dy / longitudElemento
                val transformacion=transformacionPortico(c, s)
                val kLocal=rigidezLocalPortico(
                    elasticidad,
                    elemento.area,
                    elemento.inercia,
                    longitudElemento
                )
                val indices=intArrayOf(
                    elemento.nodoInicial * 3,
                    elemento.nodoInicial * 3 + 1,
                    elemento.nodoInicial * 3 + 2,
                    elemento.nodoFinal * 3,
                    elemento.nodoFinal * 3 + 1,
                    elemento.nodoFinal * 3 + 2
                )
                val desplazamientosGlobales=
                    DoubleArray(6) { desplazamientos[indices[it]] }
                val desplazamientosLocales=
                    multiplicarVector(
                        transformacion,
                        desplazamientosGlobales
                    )
                val fuerzasLocales=
                    multiplicarVector(kLocal, desplazamientosLocales)
                val cargaLocal=
                    cargasLocales[elemento.nombre] ?: DoubleArray(6)
                for (i in 0 until 6) {
                    fuerzasLocales[i] -= cargaLocal[i]
                }
                resultadosElementos.append(
                    """
                    ${elemento.nombre.uppercase(Locale.ROOT)}
                    Extremo inicial:
                    Axial: ${formato(fuerzasLocales[0])} kN
                    Cortante: ${formato(fuerzasLocales[1])} kN
                    Momento: ${formato(fuerzasLocales[2])} kN·m

                    Extremo final:
                    Axial: ${formato(fuerzasLocales[3])} kN
                    Cortante: ${formato(fuerzasLocales[4])} kN
                    Momento: ${formato(fuerzasLocales[5])} kN·m
                    """.trimIndent()
                )
            }
            resultado.text = """
                ANÁLISIS DE PÓRTICO PLANO

                Geometría:
                Altura: ${formato(altura)} m
                Luz: ${formato(luz)} m

                Cargas:
                Distribuida: ${formato(cargaDistribuida)} kN/m
                Horizontal: ${formato(cargaHorizontal)} kN

                DESPLAZAMIENTOS DEL NIVEL SUPERIOR
                Nodo superior izquierdo:
                Horizontal: ${formato(desplazamientos[6] * 1000.0)} mm
                Vertical: ${formato(desplazamientos[7] * 1000.0)} mm
                Rotación: ${formato(desplazamientos[8])} rad

                Nodo superior derecho:
                Horizontal: ${formato(desplazamientos[9] * 1000.0)} mm
                Vertical: ${formato(desplazamientos[10] * 1000.0)} mm
                Rotación: ${formato(desplazamientos[11])} rad

                REACCIONES EN LAS BASES

                Base izquierda:
                Horizontal: ${formato(reacciones[0])} kN
                Vertical: ${formato(reacciones[1])} kN
                Momento: ${formato(reacciones[2])} kN·m

                Base derecha:
                Horizontal: ${formato(reacciones[3])} kN
                Vertical: ${formato(reacciones[4])} kN
                Momento: ${formato(reacciones[5])} kN·m

                FUERZAS INTERNAS DE LOS ELEMENTOS
                $resultadosElementos

                MÉTODO:
                Rigidez matricial de pórtico plano.

                Modelo lineal elástico de primer orden.
                Bases empotradas y uniones rígidas.
            """.trimIndent()

            guardarResultadoEstructural("porticos", resultado.text.toString())

        } catch (e: Exception) {
            resultado.text =
                "No fue posible resolver el pórtico.\nVerifica los datos y la estabilidad del modelo.\nDetalle: ${e.message}"
        }
    }
    private fun rigidezLocalPortico(
        E: Double,
        A: Double,
        I: Double,
        L: Double
    ): Array<DoubleArray> {

        val ea=E * A / L
        val ei=E * I
        val a=12.0 * ei / (L * L * L)
        val b=6.0 * ei / (L * L)
        val c=4.0 * ei / L
        val d=2.0 * ei / L

        return arrayOf(
            doubleArrayOf(ea, 0.0, 0.0, -ea, 0.0, 0.0),
            doubleArrayOf(0.0, a, b, 0.0, -a, b),
            doubleArrayOf(0.0, b, c, 0.0, -b, d),
            doubleArrayOf(-ea, 0.0, 0.0, ea, 0.0, 0.0),
            doubleArrayOf(0.0, -a, -b, 0.0, a, -b),
            doubleArrayOf(0.0, b, d, 0.0, -b, c)
        )
    }
    private fun transformacionPortico(
        c: Double,
        s: Double
    ): Array<DoubleArray> {
        val matriz=Array(6) { DoubleArray(6) }
        val bloque=arrayOf(
            doubleArrayOf(c, s, 0.0),
            doubleArrayOf(-s, c, 0.0),
            doubleArrayOf(0.0, 0.0, 1.0)
        )
        for (bloqueIndice in 0..1) {
            for (i in 0..2) {
                for (j in 0..2) {
                    matriz[bloqueIndice * 3 + i][bloqueIndice * 3 + j] =
                        bloque[i][j]
                }
            }
        }
        return matriz
    }
    private fun multiplicarMatrices(
        a: Array<DoubleArray>,
        b: Array<DoubleArray>
    ): Array<DoubleArray> {
        val filas=a.size
        val columnas=b[0].size
        val comun=b.size
        val resultado=Array(filas) { DoubleArray(columnas) }
        for (i in 0 until filas) {
            for (j in 0 until columnas) {
                for (k in 0 until comun) {
                    resultado[i][j] += a[i][k] * b[k][j]
                }
            }
        }
        return resultado
    }
    private fun transponer(
        matriz: Array<DoubleArray>
    ): Array<DoubleArray> {

        val resultado=
            Array(matriz[0].size) { DoubleArray(matriz.size) }
        for (i in matriz.indices) {
            for (j in matriz[i].indices) {
                resultado[j][i]=matriz[i][j]
            }
        }
        return resultado
    }
    private fun multiplicarVector(
        matriz: Array<DoubleArray>,
        vector: DoubleArray
    ): DoubleArray {
        val resultado=DoubleArray(matriz.size)
        for (i in matriz.indices) {
            for (j in vector.indices) {
                resultado[i] += matriz[i][j] * vector[j]
            }
        }
        return resultado
    }
    private fun resolverSistema(
        matriz: Array<DoubleArray>,
        vector: DoubleArray
    ): DoubleArray {
        val n=vector.size
        val a=Array(n) { i ->
            matriz[i].clone()
        }
        val b=vector.clone()
        for (i in 0 until n) {
            var pivote=i
            for (j in i + 1 until n) {
                if (abs(a[j][i]) > abs(a[pivote][i])) {
                    pivote=j
                }
            }
            if (abs(a[pivote][i]) < 1e-12) {
                throw IllegalArgumentException(
                    "La matriz es singular o el pórtico es inestable."
                )
            }
            val filaTemporal=a[i]
            a[i]=a[pivote]
            a[pivote]=filaTemporal
            val valorTemporal=b[i]
            b[i]=b[pivote]
            b[pivote]=valorTemporal

            for (j in i + 1 until n) {
                val factor = a[j][i] / a[i][i]
                for (k in i until n) {
                    a[j][k] -= factor * a[i][k]
                }
                b[j] -= factor * b[i]
            }
        }
        val solucion=DoubleArray(n)

        for (i in n - 1 downTo 0) {
            var suma=b[i]
            for (j in i + 1 until n) {
                suma -= a[i][j] * solucion[j]
            }
            solucion[i]=suma / a[i][i]
        }
        return solucion
    }
    private fun guardarResultadoEstructural(tipo: String, resultadoTexto: String) {
        val usuario=FirebaseAuth.getInstance().currentUser
        val idProyecto=intent.getStringExtra("projectId")
        if (usuario == null || idProyecto.isNullOrEmpty()) {
            Toast.makeText(
                this,
                "No se pudo guardar: falta el usuario o el proyecto.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        val referencia = FirebaseDatabase.getInstance(
            "https://ingeniero-currambero-default-rtdb.firebaseio.com/"
        ).reference
            .child("users")
            .child(usuario.uid)
            .child("projects")
            .child(idProyecto)
            .child("resultadosCalculo")
            .child("analisisEstructural")
            .child(tipo)
        val idRegistro=referencia.child("historial").push().key

        if (idRegistro == null) {
            Toast.makeText(
                this,
                "No se pudo crear el registro del resultado.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        val registro=mapOf(
            "modulo" to tipo,
            "resultadoTexto" to resultadoTexto,
            "fecha" to System.currentTimeMillis()
        )
        val actualizaciones=mapOf<String, Any>(
            "ultimoResultado" to registro,
            "historial/$idRegistro" to registro
        )
        referencia.updateChildren(actualizaciones)
            .addOnSuccessListener {
                Toast.makeText(
                    this,
                    "Resultado de $tipo guardado en el proyecto.",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener {
                Toast.makeText(
                    this,
                    "No se pudo guardar el resultado: ${it.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }
    private fun formato(valor: Double): String {
        return String.format(Locale.US, "%.4f", valor)
    }
}