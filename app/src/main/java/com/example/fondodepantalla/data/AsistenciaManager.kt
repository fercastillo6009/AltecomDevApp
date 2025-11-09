package com.example.fondodepantalla.data

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions // 2. Importar SetOptions para 'merge'
import java.util.Calendar

// 3. Constructor primario de Kotlin. 'db' y 'uid' se vuelven propiedades
// privadas de la clase automáticamente.
class AsistenciaManager(
    private val db: FirebaseFirestore,
    private val uid: String
) {

    // 4. OnSuccessListener y OnFailureListener reemplazados por lambdas:
    // () -> Unit  (no recibe nada, no devuelve nada)
    // (Exception) -> Unit (recibe una Excepción, no devuelve nada)
    fun registrarEntrada(
        data: Map<String, Any>, // 'Object' de Java es 'Any' en Kotlin
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val fecha = data["fecha"].toString() // Acceso al mapa en Kotlin
        db.collection("asistencias")
            .document(uid)
            .collection("asistencias")
            .document(fecha)
            .set(data)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener(onFailure) // Pasa 'it' (la excepción) implícitamente
    }

    fun actualizarAsistencia(
        fecha: String,
        data: Map<String, Any>,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.collection("asistencias")
            .document(uid)
            .collection("asistencias")
            .document(fecha)
            .update(data)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener(onFailure)
    }

    // 5. 'AsistenciaResultListener' reemplazado por (DocumentSnapshot?) -> Unit
    // Un lambda que acepta un DocumentSnapshot nulable.
    fun verificarAsistenciaHoy(
        fecha: String,
        listener: (DocumentSnapshot?) -> Unit
    ) {
        db.collection("asistencias")
            .document(uid)
            .collection("asistencias")
            .document(fecha)
            .get()
            .addOnSuccessListener { snapshot -> listener(snapshot) }
            .addOnFailureListener { listener(null) } // Devuelve 'null' en caso de fallo
    }

    fun actualizarResumenSemanal(fecha: String) {
        val cal = Calendar.getInstance()
        val semana = cal.get(Calendar.WEEK_OF_YEAR)
        val año = cal.get(Calendar.YEAR)
        // 6. Interpolación de strings en Kotlin (más limpio)
        val idSemana = "$año-W$semana"

        val refResumen = db.collection("asistencias")
            .document(uid)
            .collection("resumenSemanal")
            .document(idSemana)

        // 7. 'verificarAsistenciaHoy' ahora usa un bloque lambda
        verificarAsistenciaHoy(fecha) { snapshot ->

            // 8. Verificación de nulos segura
            if (snapshot == null || !snapshot.exists()) {
                return@verificarAsistenciaHoy // Sale solo de este lambda
            }

            val estado = snapshot.getString("estado")

            db.runTransaction { transaction ->
                val snap = transaction.get(refResumen)

                // 9. Lógica de transacción simplificada (como sugeriste en tu comentario de Java)
                // '?: 0L' es el "operador Elvis": si lo de la izquierda es nulo, usa '0L'.
                var asistencias = if (snap.exists()) snap.getLong("asistencias") ?: 0L else 0L
                var retardos = if (snap.exists()) snap.getLong("retardos") ?: 0L else 0L

                if (estado == "retardo") {
                    retardos++
                }
                // Contamos 'sitio_cliente' como asistencia
                if (estado == "puntual" || estado == "sitio_cliente") {
                    asistencias++
                }

                // 10. Creación de mapas en Kotlin
                val datos = mapOf(
                    "asistencias" to asistencias,
                    "retardos" to retardos
                )

                // .set con SetOptions.merge() es la forma más segura de actualizar
                // o crear un documento sin sobrescribir campos no intencionados.
                transaction.set(refResumen, datos, SetOptions.merge())

                // Requerido por la transacción de Firestore en Kotlin
                null
            }
        }
    }
}