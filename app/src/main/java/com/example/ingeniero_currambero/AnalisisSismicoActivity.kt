package com.example.ingeniero_currambero

import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import java.util.Locale

class AnalisisSismicoActivity : AppCompatActivity() {
    private lateinit var autenticacion: FirebaseAuth
    private lateinit var baseDatos: FirebaseDatabase
    private lateinit var referenciaDatos: DatabaseReference
    private lateinit var campoDepartamento: EditText
    private lateinit var campoMunicipio: EditText
    private lateinit var grupoPerfilSuelo: RadioGroup
    private lateinit var grupoUso: RadioGroup
    private lateinit var campoSistemaEstructural: EditText
    private lateinit var campoNumeroPisos: EditText
    private lateinit var campoAlturaTotal: EditText
    private lateinit var grupoRegularidad: RadioGroup
    private lateinit var grupoSistemaPeriodo: RadioGroup
    private lateinit var grupoDireccionAnalisis: RadioGroup
    private lateinit var campoPesosNiveles: EditText
    private lateinit var campoAlturasNiveles: EditText
    private lateinit var botonCalcularFuerza: Button
    private lateinit var textoResultadoFuerza: TextView
    private lateinit var campoAa: EditText
    private lateinit var campoAv: EditText
    private lateinit var campoPeriodo: EditText
    private lateinit var casillaModoNoFundamental: CheckBox
    private lateinit var botonGuardar: Button
    private lateinit var botonCalcularEspectro: Button
    private lateinit var textoEstado: TextView
    private lateinit var textoResultadoEspectro: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_analisis_sismico)
        autenticacion=FirebaseAuth.getInstance()
        baseDatos=FirebaseDatabase.getInstance(
            "https://ingeniero-currambero-default-rtdb.firebaseio.com/"
        )
        val idProyecto=intent.getStringExtra("projectId")

        if (idProyecto.isNullOrBlank()) {
            mostrarMensaje("No se encontró el proyecto.")
            finish()
            return
        }
        val usuario=autenticacion.currentUser
        if (usuario == null) {
            mostrarMensaje("Debes iniciar sesión.")
            finish()
            return
        }
        referenciaDatos=baseDatos.reference
            .child("users")
            .child(usuario.uid)
            .child("projects")
            .child(idProyecto)
            .child("resultadosCalculo")
            .child("analisisSismico")

        campoDepartamento=findViewById(R.id.etDepartamentoSismico)
        campoMunicipio=findViewById(R.id.etMunicipioSismico)
        grupoPerfilSuelo=findViewById(R.id.rgPerfilSuelo)
        grupoUso=findViewById(R.id.rgGrupoUso)
        campoSistemaEstructural=findViewById(R.id.etSistemaEstructural)
        campoNumeroPisos=findViewById(R.id.etNumeroPisosSismico)
        campoAlturaTotal=findViewById(R.id.etAlturaTotalSismico)
        grupoRegularidad=findViewById(R.id.rgRegularidad)
        campoAa=findViewById(R.id.etAaSismico)
        campoAv=findViewById(R.id.etAvSismico)
        campoPeriodo=findViewById(R.id.etPeriodoSismico)
        casillaModoNoFundamental=findViewById(R.id.cbModoNoFundamental)
        botonGuardar=findViewById(R.id.btnGuardarDatosSismicos)
        botonCalcularEspectro=findViewById(R.id.btnCalcularEspectro)
        textoEstado=findViewById(R.id.tvEstadoSismico)
        textoResultadoEspectro=findViewById(R.id.tvResultadoEspectro)
        grupoSistemaPeriodo=findViewById(R.id.rgSistemaPeriodo)
        grupoDireccionAnalisis=findViewById(R.id.rgDireccionAnalisis)
        campoPesosNiveles=findViewById(R.id.etPesosSismicosNiveles)
        campoAlturasNiveles=findViewById(R.id.etAlturasNiveles)
        botonCalcularFuerza=findViewById(R.id.btnCalcularFuerzaEquivalente)
        textoResultadoFuerza=findViewById(R.id.tvResultadoFuerzaEquivalente)

        botonCalcularFuerza.setOnClickListener {
            calcularFuerzaHorizontalEquivalente()
        }
        cargarDatosSismicos()

        botonGuardar.setOnClickListener {
            guardarDatosSismicos()
        }
        botonCalcularEspectro.setOnClickListener {
            calcularEspectro()
        }
    }

    private fun cargarDatosSismicos() {
        referenciaDatos.get()
            .addOnSuccessListener { datos ->
                if (!datos.exists()) {
                    textoEstado.text="Todavía no existen datos sísmicos guardados."
                    textoResultadoEspectro.text="Sin resultados de espectro."
                    textoResultadoFuerza.text="Sin resultados de fuerza horizontal equivalente."
                    return@addOnSuccessListener
                }

                campoDepartamento.setText(obtenerTexto(datos, "departamento"))
                campoMunicipio.setText(obtenerTexto(datos, "municipio"))
                seleccionarRadio(grupoPerfilSuelo, obtenerTexto(datos, "perfilSuelo"))
                seleccionarRadio(grupoUso, obtenerTexto(datos, "grupoUso"))
                campoSistemaEstructural.setText(obtenerTexto(datos, "sistemaEstructural"))
                campoNumeroPisos.setText(obtenerTexto(datos, "numeroPisos"))
                campoAlturaTotal.setText(obtenerTexto(datos, "alturaTotal"))
                seleccionarRadio(grupoRegularidad, obtenerTexto(datos, "regularidad"))
                campoAa.setText(obtenerTexto(datos, "aa"))
                campoAv.setText(obtenerTexto(datos, "av"))
                campoPeriodo.setText(obtenerTexto(datos, "periodo"))

                casillaModoNoFundamental.isChecked=
                    datos.child("modoNoFundamental").getValue(Boolean::class.java) ?: false

                textoResultadoEspectro.text=
                    obtenerTexto(datos, "resultadoEspectroTexto")
                        .ifBlank { "Sin resultados de espectro." }

                seleccionarRadio(grupoSistemaPeriodo, obtenerTexto(datos, "sistemaPeriodo"))
                seleccionarRadio(grupoDireccionAnalisis, obtenerTexto(datos, "direccionAnalisis"))
                campoPesosNiveles.setText(obtenerTexto(datos, "pesosNivelesTexto"))
                campoAlturasNiveles.setText(obtenerTexto(datos, "alturasNivelesTexto"))

                textoResultadoFuerza.text=
                    datos.child("fuerzaHorizontalEquivalente")
                        .child("resultadoTexto")
                        .getValue(String::class.java)
                        ?: "Sin resultados de fuerza horizontal equivalente."

                textoEstado.text=obtenerTexto(datos, "estadoEspectro")
                    .ifBlank { "Datos sísmicos recuperados." }
            }
            .addOnFailureListener { error ->
                mostrarMensaje("Error al recuperar información: ${error.message}")
            }
    }

    private fun crearDatosCaracterizacion(): HashMap<String, Any>? {
        val departamento=campoDepartamento.text.toString().trim()
        val municipio=campoMunicipio.text.toString().trim()
        val perfilSuelo=obtenerSeleccion(grupoPerfilSuelo)
        val grupoUsoSeleccionado=obtenerSeleccion(grupoUso)
        val sistemaEstructural=campoSistemaEstructural.text.toString().trim()
        val pisosTexto=campoNumeroPisos.text.toString().trim()
        val alturaTexto=campoAlturaTotal.text.toString()
            .trim()
            .replace(",", ".")
        val regularidad=obtenerSeleccion(grupoRegularidad)

        if (departamento.isEmpty() || municipio.isEmpty()) {
            mostrarMensaje("Completa el departamento y municipio.")
            return null
        }

        if (perfilSuelo == null) {
            mostrarMensaje("Selecciona el perfil de suelo.")
            return null
        }

        if (grupoUsoSeleccionado == null) {
            mostrarMensaje("Selecciona el grupo de uso.")
            return null
        }

        val numeroPisos=if (pisosTexto.isEmpty()) {
            null
        } else {
            pisosTexto.toIntOrNull()
        }

        if (
            pisosTexto.isNotEmpty() &&
            (numeroPisos == null || numeroPisos <= 0)
        ) {
            mostrarMensaje("Verifica el número de pisos.")
            return null
        }

        val altura=if (alturaTexto.isEmpty()) {
            null
        } else {
            alturaTexto.toDoubleOrNull()
        }

        if (
            alturaTexto.isNotEmpty() &&
            (
                    altura == null ||
                            !altura.isFinite() ||
                            altura <= 0
                    )
        ) {
            mostrarMensaje("Ingresa una altura válida.")
            return null
        }

        val importancia=obtenerImportancia(grupoUsoSeleccionado)

        val datos=hashMapOf<String, Any>(
            "departamento" to departamento,
            "municipio" to municipio,
            "perfilSuelo" to perfilSuelo,
            "grupoUso" to grupoUsoSeleccionado,
            "coeficienteImportancia" to importancia,
            "sistemaEstructural" to sistemaEstructural,
            "numeroPisos" to (numeroPisos ?: "").toString(),
            "alturaTotal" to (altura ?: "").toString(),
            "regularidad" to (regularidad ?: "")
        )

        return datos
    }

    private fun guardarDatosSismicos() {
        val datos=crearDatosCaracterizacion() ?: return
        datos["estadoDatos"]="Caracterización guardada"
        datos["estadoEspectro"]="Pendiente de calcular o recalcular"
        datos["ultimaActualizacion"]=ServerValue.TIMESTAMP
        datos["resultadoEspectroTexto"]=
            "La caracterización fue actualizada. " +
                    "Vuelve a calcular el espectro para actualizar los resultados."

        botonGuardar.isEnabled=false

        referenciaDatos.updateChildren(datos)
            .addOnSuccessListener {
                botonGuardar.isEnabled=true
                textoEstado.text="Caracterización sísmica guardada correctamente."
                textoResultadoEspectro.text=
                    datos["resultadoEspectroTexto"].toString()

                mostrarMensaje("Datos sísmicos guardados correctamente.")
            }
            .addOnFailureListener { error ->
                botonGuardar.isEnabled=true
                mostrarMensaje("Error al guardar: ${error.message}")
            }
    }
    private fun calcularEspectro() {
        val datos=crearDatosCaracterizacion() ?: return

        val aa=leerNumero(campoAa)
        val av=leerNumero(campoAv)
        val periodo=leerNumero(campoPeriodo)

        if (aa == null || av == null || periodo == null) {
            mostrarMensaje("Completa Aa, Av y período de vibración.")
            return
        }

        if (
            aa !in 0.05..0.50 ||
            av !in 0.05..0.50 ||
            periodo <= 0
        ) {
            mostrarMensaje("Verifica Aa, Av y el período ingresado.")
            return
        }
        val perfil=datos["perfilSuelo"].toString()
        val grupo=datos["grupoUso"].toString()

        if (perfil == "F") {
            mostrarMensaje(
                "El perfil F requiere análisis geotécnico particular. " +
                        "No se puede calcular Fa ni Fv automáticamente."
            )
            return
        }
        val factorFa=obtenerFa(perfil, aa)
        val factorFv=obtenerFv(perfil, av)

        if (factorFa == null || factorFv == null) {
            mostrarMensaje("No se pudieron obtener los factores del suelo.")
            return
        }
        val factorImportancia=obtenerImportancia(grupo)
        val periodoCero=0.1 * av * factorFv / (aa * factorFa)
        val periodoC=0.48 * av * factorFv / (aa * factorFa)
        val periodoL=2.4 * factorFv
        if (
            !periodoCero.isFinite() ||
            !periodoC.isFinite() ||
            !periodoL.isFinite() ||
            periodoCero <= 0 ||
            periodoC <= 0 ||
            periodoL <= 0
        ) {
            mostrarMensaje("No fue posible calcular los períodos característicos.")
            return
        }
        val modoNoFundamental=casillaModoNoFundamental.isChecked
        var tramoEspectro=""
        var ecuacionUtilizada=""

        val aceleracionEspectral=when {
            modoNoFundamental && periodo < periodoCero -> {
                tramoEspectro="Rama ascendente para modo diferente al fundamental"
                ecuacionUtilizada="A.2.6-7"
                2.5 * aa * factorFa * factorImportancia *
                        (0.4 + 0.6 * periodo / periodoCero)
            }
            periodo <= periodoC -> {
                tramoEspectro="Meseta del espectro"
                ecuacionUtilizada="A.2.6-3"
                2.5 * aa * factorFa * factorImportancia
            }
            periodo <= periodoL -> {
                tramoEspectro="Rama descendente de períodos intermedios"
                ecuacionUtilizada="A.2.6-1"
                1.2 * av * factorFv *
                        factorImportancia / periodo
            }
            else -> {
                tramoEspectro="Rama de períodos largos"
                ecuacionUtilizada="A.2.6-5"
                1.2 * av * factorFv *
                        periodoL * factorImportancia /
                        (periodo * periodo)
            }
        }
        if (
            !aceleracionEspectral.isFinite() ||
            aceleracionEspectral <= 0
        ) {
            mostrarMensaje("El resultado del espectro no es válido.")
            return
        }

        val resultados=buildString {
            appendLine("ESPECTRO ELÁSTICO DE ACELERACIONES")
            appendLine("NSR-10 - TÍTULO A")
            appendLine("Amortiguamiento: 5 %")
            appendLine()

            appendLine("DATOS DE ENTRADA")
            appendLine("Aa: ${formatear(aa)}")
            appendLine("Av: ${formatear(av)}")
            appendLine("Perfil de suelo: $perfil")
            appendLine("Grupo de uso: $grupo")
            appendLine("Período T: ${formatear(periodo)} s")
            appendLine()

            appendLine("FACTORES")
            appendLine("Fa: ${formatear(factorFa)}")
            appendLine("Fv: ${formatear(factorFv)}")
            appendLine("I: ${formatear(factorImportancia)}")
            appendLine()

            appendLine("PERÍODOS CARACTERÍSTICOS")
            appendLine("T₀: ${formatear(periodoCero)} s")
            appendLine("Tc: ${formatear(periodoC)} s")
            appendLine("TL: ${formatear(periodoL)} s")
            appendLine()

            appendLine("PROCEDIMIENTO")
            appendLine(tramoEspectro)
            appendLine("Ecuación: $ecuacionUtilizada")
            appendLine()

            appendLine("ACELERACIÓN ESPECTRAL")
            appendLine("Sa = ${formatear(aceleracionEspectral)} g")
            appendLine()
            appendLine("Resultado expresado como fracción de la gravedad.")
            appendLine("Este resultado corresponde al espectro elástico.")
        }
        datos["aa"]=aa
        datos["av"]=av
        datos["periodo"]=periodo
        datos["fa"]=factorFa
        datos["fv"]=factorFv
        datos["factorImportancia"]=factorImportancia
        datos["periodoT0"]=periodoCero
        datos["periodoTc"]=periodoC
        datos["periodoTL"]=periodoL
        datos["aceleracionEspectralSa"]=aceleracionEspectral
        datos["tramoEspectro"]=tramoEspectro
        datos["ecuacionEspectro"]=ecuacionUtilizada
        datos["modoNoFundamental"]=modoNoFundamental
        datos["amortiguamientoPorcentaje"]=5.0
        datos["resultadoEspectroTexto"]=resultados
        datos["estadoDatos"]="Caracterización y espectro calculados"
        datos["estadoEspectro"]="Resultado preliminar calculado"
        datos["ultimaActualizacion"]=ServerValue.TIMESTAMP

        botonCalcularEspectro.isEnabled=false

        referenciaDatos.updateChildren(datos)
            .addOnSuccessListener {
                botonCalcularEspectro.isEnabled=true
                textoResultadoEspectro.text=resultados
                textoEstado.text="Espectro elástico calculado y guardado."
                mostrarMensaje("Espectro calculado correctamente.")
            }
            .addOnFailureListener { error ->
                botonCalcularEspectro.isEnabled=true
                mostrarMensaje("Error al guardar el resultado: ${error.message}")
            }
    }
    private fun obtenerFa(
        perfil: String,
        aa: Double
    ): Double? {
        val valores=when (perfil) {
            "A" -> listOf(0.8, 0.8, 0.8, 0.8, 0.8)
            "B" -> listOf(1.0, 1.0, 1.0, 1.0, 1.0)
            "C" -> listOf(1.2, 1.2, 1.1, 1.0, 1.0)
            "D" -> listOf(1.6, 1.4, 1.2, 1.1, 1.0)
            "E" -> listOf(2.5, 1.7, 1.2, 0.9, 0.9)
            else -> return null
        }
        return interpolarCoeficiente(aa, valores)
    }
    private fun obtenerFv(
        perfil: String,
        av: Double
    ): Double? {
        val valores=when (perfil) {
            "A" -> listOf(0.8, 0.8, 0.8, 0.8, 0.8)
            "B" -> listOf(1.0, 1.0, 1.0, 1.0, 1.0)
            "C" -> listOf(1.7, 1.6, 1.5, 1.4, 1.3)
            "D" -> listOf(2.4, 2.0, 1.8, 1.6, 1.5)
            "E" -> listOf(3.5, 3.2, 2.8, 2.4, 2.4)
            else -> return null
        }
        return interpolarCoeficiente(av, valores)
    }
    private fun interpolarCoeficiente(
        valor: Double,
        valoresTabla: List<Double>
    ): Double {
        val puntos=listOf(0.1, 0.2, 0.3, 0.4, 0.5)
        if (valor <= puntos.first()) {
            return valoresTabla.first()
        }
        if (valor >= puntos.last()) {
            return valoresTabla.last()
        }
        for (indice in 0 until puntos.size - 1) {
            val puntoInferior=puntos[indice]
            val puntoSuperior=puntos[indice + 1]

            if (valor <= puntoSuperior) {
                val factorInterpolacion=
                    (valor - puntoInferior) /
                            (puntoSuperior - puntoInferior)
                val valorInferior=valoresTabla[indice]
                val valorSuperior=valoresTabla[indice + 1]
                return valorInferior +
                        factorInterpolacion *
                        (valorSuperior - valorInferior)
            }
        }
        return valoresTabla.last()
    }
    private fun obtenerImportancia(grupo: String): Double {
        return when (grupo.uppercase(Locale.ROOT)) {
            "I" -> 1.00
            "II" -> 1.10
            "III" -> 1.25
            "IV" -> 1.50
            else -> 1.00
        }
    }
    private fun obtenerSeleccion(
        grupo: RadioGroup
    ): String? {
        val idSeleccionado=grupo.checkedRadioButtonId
        if (idSeleccionado == -1) {
            return null
        }
        return findViewById<RadioButton>(idSeleccionado)
            .text.toString().trim()
    }
    private fun seleccionarRadio(
        grupo: RadioGroup,
        valor: String?
    ) {
        grupo.clearCheck()

        if (valor.isNullOrBlank()) return
        for (indice in 0 until grupo.childCount) {
            val boton=grupo.getChildAt(indice) as? RadioButton
            if (
                boton?.text?.toString()
                    ?.equals(valor, ignoreCase = true) == true
            ) {
                grupo.check(boton.id)
                return
            }
        }
    }
    private fun leerNumero(
        campo: EditText
    ): Double? {
        val texto=campo.text.toString()
            .trim()
            .replace(",", ".")
        if (texto.isBlank()) return null
        return texto.toDoubleOrNull()
            ?.takeIf { numero -> numero.isFinite() }
    }
    private fun obtenerTexto(
        instantanea: com.google.firebase.database.DataSnapshot,
        clave: String
    ): String {
        return instantanea.child(clave)
            .value?.toString() ?: ""
    }
    private fun formatear(
        numero: Double
    ): String {
        return String.format(
            Locale("es", "CO"),
            "%.4f",
            numero
        )
    }
    private fun mostrarMensaje(
        mensaje: String
    ) {
        Toast.makeText(
            this,
            mensaje,
            Toast.LENGTH_LONG
        ).show()
    }
    private fun calcularFuerzaHorizontalEquivalente() {
        val datosCaracterizacion=crearDatosCaracterizacion() ?: return
        val aa=leerNumero(campoAa)
        val av=leerNumero(campoAv)

        if (aa == null || av == null || aa <= 0 || av <= 0) {
            mostrarMensaje("Verifica los valores de Aa y Av.")
            return
        }
        val perfilSuelo=datosCaracterizacion["perfilSuelo"].toString()
        val grupoUsoSeleccionado=datosCaracterizacion["grupoUso"].toString()

        if (perfilSuelo == "F") {
            mostrarMensaje("El perfil F requiere investigación geotécnica particular.")
            return
        }
        val factorFa=obtenerFa(perfilSuelo, aa)
        val factorFv=obtenerFv(perfilSuelo, av)

        if (factorFa == null || factorFv == null) {
            mostrarMensaje("No se pudieron obtener Fa y Fv.")
            return
        }
        val factorImportancia=obtenerImportancia(grupoUsoSeleccionado)

        val parametrosSistema=when (grupoSistemaPeriodo.checkedRadioButtonId) {
            R.id.rbSistemaConcreto -> Pair(0.047, 0.90)
            R.id.rbSistemaAcero -> Pair(0.072, 0.80)
            R.id.rbSistemaArriostrado -> Pair(0.073, 0.75)
            R.id.rbSistemaMuros -> Pair(0.049, 0.75)
            else -> {
                mostrarMensaje("Selecciona el sistema estructural.")
                return
            }
        }
        val coeficienteCt=parametrosSistema.first
        val exponenteAlfa=parametrosSistema.second
        val sistemaSeleccionado=obtenerSeleccion(grupoSistemaPeriodo)!!
        val direccion=obtenerSeleccion(grupoDireccionAnalisis)

        if (direccion == null) {
            mostrarMensaje("Selecciona la dirección X o Y.")
            return
        }
        val alturaEdificacion=leerNumero(campoAlturaTotal)

        if (alturaEdificacion == null || alturaEdificacion <= 0) {
            mostrarMensaje("Ingresa una altura total válida.")
            return
        }
        val numeroPisos=campoNumeroPisos.text.toString()
            .trim()
            .toIntOrNull()
        if (numeroPisos == null || numeroPisos <= 0) {
            mostrarMensaje("Ingresa un número de pisos válido.")
            return
        }
        val pesosNiveles=leerListaNumerica(campoPesosNiveles)

        if (pesosNiveles == null) {
            mostrarMensaje("Ingresa los pesos sísmicos positivos, uno por línea.")
            return
        }
        val alturasNiveles=leerListaNumerica(campoAlturasNiveles)

        if (alturasNiveles == null) {
            mostrarMensaje("Ingresa las alturas positivas, una por línea.")
            return
        }
        if (
            pesosNiveles.size != numeroPisos ||
            alturasNiveles.size != numeroPisos
        ) {
            mostrarMensaje(
                "La cantidad de pesos y alturas debe coincidir " +
                        "con el número de pisos."
            )
            return
        }
        for (indice in 1 until alturasNiveles.size) {
            if (alturasNiveles[indice] <= alturasNiveles[indice - 1]) {
                mostrarMensaje("Las alturas deben aumentar desde el primer piso.")
                return
            }
        }
        if (alturasNiveles.last() > alturaEdificacion + 0.01) {
            mostrarMensaje("La altura de un nivel supera la altura total.")
            return
        }
        val periodoTa=coeficienteCt *
                Math.pow(alturaEdificacion, exponenteAlfa)

        if (!periodoTa.isFinite() || periodoTa <= 0) {
            mostrarMensaje("El período aproximado no es válido.")
            return
        }
        val coeficienteCu=maxOf(
            1.20,
            1.75 - 1.20 * av * factorFv
        )
        val limitePeriodo=coeficienteCu * periodoTa
        val periodoT0=0.10 * av * factorFv / (aa * factorFa)
        val periodoTc=0.48 * av * factorFv / (aa * factorFa)
        val periodoTL=2.40 * factorFv

        val aceleracionSa=when {
            periodoTa <= periodoTc -> {
                2.5 * aa * factorFa * factorImportancia
            }
            periodoTa <= periodoTL -> {
                1.2 * av * factorFv *
                        factorImportancia / periodoTa
            }
            else -> {
                1.2 * av * factorFv *
                        periodoTL * factorImportancia /
                        (periodoTa * periodoTa)
            }
        }
        if (!aceleracionSa.isFinite() || aceleracionSa <= 0) {
            mostrarMensaje("La aceleración espectral no es válida.")
            return
        }
        val exponenteK=when {
            periodoTa <= 0.5 -> 1.0
            periodoTa <= 2.5 -> 0.75 + 0.5 * periodoTa
            else -> 2.0
        }
        val pesoTotal=pesosNiveles.sum()

        if (!pesoTotal.isFinite() || pesoTotal <= 0) {
            mostrarMensaje("El peso sísmico total no es válido.")
            return
        }
        val cortanteBasal=aceleracionSa * pesoTotal
        val sumaDistribucion=pesosNiveles.indices.sumOf { indice ->
            pesosNiveles[indice] *
                    Math.pow(alturasNiveles[indice], exponenteK)
        }
        if (!sumaDistribucion.isFinite() || sumaDistribucion <= 0) {
            mostrarMensaje("No se pudo calcular la distribución de fuerzas.")
            return
        }
        val resultadosNiveles=mutableListOf<HashMap<String, Any>>()

        val resultadosTexto=buildString {
            appendLine("FUERZA HORIZONTAL EQUIVALENTE")
            appendLine("NSR-10 - CAPÍTULO A.4")
            appendLine()
            appendLine("Dirección: $direccion")
            appendLine("Sistema: $sistemaSeleccionado")
            appendLine()

            appendLine("PERÍODO FUNDAMENTAL APROXIMADO")
            appendLine("Ct: ${formatear(coeficienteCt)}")
            appendLine("Alfa: ${formatear(exponenteAlfa)}")
            appendLine("Altura total: ${formatear(alturaEdificacion)} m")
            appendLine("Ta: ${formatear(periodoTa)} s")
            appendLine("Cu: ${formatear(coeficienteCu)}")
            appendLine("CuTa: ${formatear(limitePeriodo)} s")

            appendLine()
            appendLine("PARÁMETROS SÍSMICOS")
            appendLine("Fa: ${formatear(factorFa)}")
            appendLine("Fv: ${formatear(factorFv)}")
            appendLine("I: ${formatear(factorImportancia)}")
            appendLine("T0: ${formatear(periodoT0)} s")
            appendLine("Tc: ${formatear(periodoTc)} s")
            appendLine("TL: ${formatear(periodoTL)} s")
            appendLine("Sa(Ta): ${formatear(aceleracionSa)} g")

            appendLine()
            appendLine("PESO Y CORTANTE BASAL")
            appendLine("Peso sísmico total: ${formatear(pesoTotal)} kN")
            appendLine("Vs: ${formatear(cortanteBasal)} kN")
            appendLine("Exponente k: ${formatear(exponenteK)}")

            appendLine()
            appendLine("DISTRIBUCIÓN DE FUERZAS")

            for (indice in pesosNiveles.indices) {
                val peso=pesosNiveles[indice]
                val altura=alturasNiveles[indice]
                val cvx=peso * Math.pow(altura, exponenteK) /
                        sumaDistribucion
                val fuerza=cvx * cortanteBasal
                val datosNivel=hashMapOf<String, Any>(
                    "nivel" to (indice + 1),
                    "pesoSismico" to peso,
                    "altura" to altura,
                    "coeficienteDistribucion" to cvx,
                    "fuerzaHorizontal" to fuerza
                )
                resultadosNiveles.add(datosNivel)

                appendLine("PISO ${indice + 1}")
                appendLine("Peso: ${formatear(peso)} kN")
                appendLine("Altura: ${formatear(altura)} m")
                appendLine("Cvx: ${formatear(cvx)}")
                appendLine("Fx: ${formatear(fuerza)} kN")
                appendLine()
            }
            appendLine("Suma de fuerzas: ${formatear(cortanteBasal)} kN")
            appendLine("Procedimiento: A.4.2 y A.4.3.")
            appendLine()
            appendLine(
                "Estimación mediante el período aproximado Ta. " +
                        "La aplicabilidad del método debe verificarse."
            )
        }
        val datosFuerza=hashMapOf<String, Any>(
            "direccion" to direccion,
            "sistemaEstructural" to sistemaSeleccionado,
            "coeficienteCt" to coeficienteCt,
            "exponenteAlfa" to exponenteAlfa,
            "alturaTotal" to alturaEdificacion,
            "periodoTa" to periodoTa,
            "coeficienteCu" to coeficienteCu,
            "limitePeriodoCuTa" to limitePeriodo,
            "fa" to factorFa,
            "fv" to factorFv,
            "factorImportancia" to factorImportancia,
            "periodoT0" to periodoT0,
            "periodoTc" to periodoTc,
            "periodoTL" to periodoTL,
            "aceleracionSa" to aceleracionSa,
            "pesoTotal" to pesoTotal,
            "cortanteBasal" to cortanteBasal,
            "exponenteK" to exponenteK,
            "niveles" to resultadosNiveles,
            "resultadoTexto" to resultadosTexto,
            "estado" to "Resultado preliminar"
        )
        val datosFirebase=HashMap<String, Any>()
        datosFirebase.putAll(datosCaracterizacion)
        datosFirebase["sistemaPeriodo"]=sistemaSeleccionado
        datosFirebase["direccionAnalisis"]=direccion
        datosFirebase["pesosNivelesTexto"]=campoPesosNiveles.text.toString()
        datosFirebase["alturasNivelesTexto"]=campoAlturasNiveles.text.toString()
        datosFirebase["fuerzaHorizontalEquivalente"]=datosFuerza
        datosFirebase["estadoDatos"]="Fuerza horizontal equivalente calculada"
        datosFirebase["ultimaActualizacion"]=ServerValue.TIMESTAMP

        botonCalcularFuerza.isEnabled=false
        referenciaDatos.updateChildren(datosFirebase)
            .addOnSuccessListener {
                botonCalcularFuerza.isEnabled=true
                textoResultadoFuerza.text=resultadosTexto
                textoEstado.text="Fuerza horizontal equivalente guardada."
                mostrarMensaje("Cálculo realizado y guardado correctamente.")
            }
            .addOnFailureListener { error ->
                botonCalcularFuerza.isEnabled=true
                mostrarMensaje("Error al guardar: ${error.message}")
            }
    }
    private fun leerListaNumerica(
        campo: EditText
    ): List<Double>? {
        val lineas=campo.text.toString()
            .trim()
            .split(Regex("[\\r\\n;]+"))
            .filter { it.isNotBlank() }
        if (lineas.isEmpty()) return null
        val valores=mutableListOf<Double>()
        for (linea in lineas) {
            val numero=linea.trim()
                .replace(",", ".")
                .toDoubleOrNull()
            if (
                numero == null ||
                !numero.isFinite() ||
                numero <= 0
            ) {
                return null
            }
            valores.add(numero)
        }
        return valores
    }
}
