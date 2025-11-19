package com.example.fondodepantalla.ui.screens

import android.graphics.Bitmap // Import nuevo
import android.graphics.Canvas // Import nuevo
import android.graphics.drawable.BitmapDrawable // Import nuevo
import android.graphics.drawable.Drawable
import android.preference.PreferenceManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fondodepantalla.R // Importa tu R
import com.example.fondodepantalla.ui.viewmodel.EmpleadoConUbicacion
import com.example.fondodepantalla.ui.viewmodel.MonitorViewModel
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonitorScreen(
    viewModel: MonitorViewModel = viewModel()
) {
    val empleadosState by viewModel.empleadosConNombres.collectAsState()
    val bottomSheetState = rememberStandardBottomSheetState(
        initialValue = SheetValue.PartiallyExpanded
    )
    val scaffoldState = rememberBottomSheetScaffoldState(bottomSheetState)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var mapViewRef by remember { mutableStateOf<MapView?>(null) }

    // --- ¡NUEVO! Tu API Key de Maptiler ---
    val maptilerApiKey = "vENZNrm3EIvvppOrqZ60"

    // --- ¡CAMBIO DE ESTILO! ---
    // "dataviz" es un estilo gris y muy limpio, justo como la imagen que enviaste.
    val mapStyleId = "dataviz"

    val maptilerSource = remember(maptilerApiKey) {
        object : OnlineTileSourceBase(
            "MapTiler $mapStyleId", 0, 20, 256, ".png",
            arrayOf("https://api.maptiler.com/maps/$mapStyleId/")
        ) {
            override fun getTileURLString(pMapTileIndex: Long): String {
                return baseUrl +
                        "${MapTileIndex.getZoom(pMapTileIndex)}/" +
                        "${MapTileIndex.getX(pMapTileIndex)}/" +
                        "${MapTileIndex.getY(pMapTileIndex)}.png" +
                        "?key=$maptilerApiKey"
            }
        }
    }

    // --- ¡CAMBIO DE TAMAÑO DE ICONO! ---
    val customIcon: Drawable? = remember(context) {
        try {
            val originalDrawable = ContextCompat.getDrawable(context, R.drawable.ic_marker)
            if (originalDrawable == null) return@remember null

            // Define el nuevo tamaño en píxeles (ej. 36dp)
            // Cambia el '36' si quieres un tamaño diferente
            val density = context.resources.displayMetrics.density
            val sizeInPx = (36 * density).toInt()

            // Escala el drawable a un bitmap del tamaño correcto
            val bitmap = Bitmap.createBitmap(sizeInPx, sizeInPx, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            originalDrawable.setBounds(0, 0, canvas.width, canvas.height)
            originalDrawable.draw(canvas)

            // Devuelve el bitmap redimensionado como un Drawable
            BitmapDrawable(context.resources, bitmap)
        } catch (e: Exception) {
            null // Si falla (ej. no existe el drawable), usará el default
        }
    }


    LaunchedEffect(empleadosState) {
        mapViewRef?.let { map ->
            map.overlays.clear()

            empleadosState.forEach { empleado ->
                val marker = Marker(map)
                marker.position = GeoPoint(empleado.ubicacion.latitude, empleado.ubicacion.longitude)
                marker.title = empleado.nombre
                val fechaString = empleado.ultimaActualizacion?.toDate()?.toString() ?: "N/A"
                marker.snippet = "Última act: $fechaString"

                // --- Asigna el icono redimensionado ---
                if (customIcon != null) {
                    marker.icon = customIcon
                }

                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER) // Centrado, como en tu imagen
                map.overlays.add(marker)
            }
            map.invalidate()
        }
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 120.dp,
        sheetContent = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 8.dp, bottom = 12.dp)
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                )

                Text(
                    "Empleados Activos (${empleadosState.size})",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyColumn(modifier = Modifier.fillMaxHeight(0.6f)) {
                    items(empleadosState, key = { it.id }) { empleado ->
                        EmpleadoEnListaCard(empleado = empleado, onClick = {
                            mapViewRef?.controller?.let { controller ->
                                val punto = GeoPoint(empleado.ubicacion.latitude, empleado.ubicacion.longitude)
                                controller.setCenter(punto)
                                controller.setZoom(18.0)
                                scope.launch { scaffoldState.bottomSheetState.partialExpand() }
                            }
                        })
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            AndroidView(
                factory = { ctx ->
                    Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx))
                    MapView(ctx).apply {
                        // --- ¡CAMBIO! Usa tu nueva fuente de mapa Maptiler ---
                        setTileSource(maptilerSource)

                        setMultiTouchControls(true)
                        controller.setZoom(10.0)
                        controller.setCenter(GeoPoint(19.4326, -99.1332))
                        mapViewRef = this
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun EmpleadoEnListaCard(
    empleado: EmpleadoConUbicacion,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(empleado.nombre, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                val fechaString = empleado.ultimaActualizacion?.toDate()?.toString() ?: "N/A"
                Text(
                    "Actualizado: $fechaString",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}