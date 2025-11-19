package com.example.fondodepantalla.ui.viewmodel // O tu paquete de ViewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.snapshots
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Esta es la "estructura de datos" que usará tu UI (MonitorScreen).
 * Combina la información de 'locations' y 'usuarios'.
 */
data class EmpleadoConUbicacion(
    val id: String,
    val nombre: String,
    val ubicacion: GeoPoint, // El GeoPoint de Firebase
    val ultimaActualizacion: Timestamp? = null
)

/**
 * Este es el ViewModel (el "cerebro").
 * Su trabajo es escuchar los datos de Firebase y prepararlos para la pantalla.
 */
class MonitorViewModel : ViewModel() {

    private val db = Firebase.firestore

    // 1. Un flow que escucha la colección 'locations' en tiempo real.
    //    Solo trae los documentos donde 'isTracking' sea true.
    private val empleadosActivosFlow = db.collection("locations")
        .whereEqualTo("isTracking", true)
        .snapshots() // ¡Esto es lo que da el tiempo real!
        .map { snapshot ->
            // Convierte los documentos de Firebase a una lista simple de datos
            snapshot.documents.mapNotNull { doc ->
                val geoPoint = doc.getGeoPoint("currentLocation")
                val timestamp = doc.getTimestamp("lastUpdate")
                if (geoPoint != null) {
                    Triple(doc.id, geoPoint, timestamp) // (Id, Ubicación, Hora)
                } else {
                    null
                }
            }
        }

    // 2. El flow principal que la UI (MonitorScreen) escuchará.
    //    Usa 'flatMapLatest' y 'combine' para hacer la magia de
    //    juntar los datos de 'locations' con los de 'usuarios'.
    val empleadosConNombres: StateFlow<List<EmpleadoConUbicacion>> = empleadosActivosFlow.flatMapLatest { listaDeActivos ->

        // Si no hay nadie activo, emite una lista vacía
        if (listaDeActivos.isEmpty()) {
            flowOf(emptyList())
        } else {
            // Para cada empleado activo, necesitamos buscar su nombre en 'usuarios'
            val flowsDeUsuarios = listaDeActivos.map { (id, geoPoint, timestamp) ->

                // Crea un flow individual que escucha el documento de CADA usuario
                db.collection("usuarios").document(id).snapshots()
                    .map { userDoc ->
                        // Cuando el documento del usuario llega, saca el nombre
                        val nombre = userDoc.getString("nombre") ?: "Nombre Desconocido"

                        // Y crea el objeto final que usará la UI
                        EmpleadoConUbicacion(
                            id = id,
                            nombre = nombre,
                            ubicacion = geoPoint,
                            ultimaActualizacion = timestamp
                        )
                    }
            }

            // 'combine' junta los resultados de todos los flows individuales
            // en una sola lista actualizada en tiempo real.
            combine(flowsDeUsuarios) { arrayDeEmpleados ->
                arrayDeEmpleados.toList()
            }
        }
    }.stateIn( // Convierte el flow en un StateFlow para que la UI lo consuma
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000), // Empieza a escuchar cuando la app está abierta
        initialValue = emptyList() // Empieza con una lista vacía
    )
}