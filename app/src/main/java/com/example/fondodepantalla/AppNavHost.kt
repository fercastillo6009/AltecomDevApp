package com.example.fondodepantalla // O tu paquete de navegación

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.fondodepantalla.screens.*

// --- ¡NUEVO! Importa la pantalla de monitoreo ---
// (Asegúrate de que esta ruta sea correcta a donde guardaste MonitorScreen.kt)
import com.example.fondodepantalla.ui.screens.MonitorScreen

/**
 * Define todas las rutas de navegación de la app.
 * Este NavHost reemplaza al 'FrameLayout' y a todas las 'FragmentTransactions'.
 */
@Composable
fun AppNavHost(
    navController: NavHostController,
    startRoute: String // La ruta inicial (dependerá del rol del usuario)
) {
    NavHost(navController = navController, startDestination = startRoute) {

        // --- Rutas de Admin ---
        composable("inicio_admin") {
            InicioAdminScreen()
        }
        composable("crear_usuario") {
            CrearUsuarioScreen()
        }
        composable("asignar_tarea") {
            AsignarTareaScreen()
        }
        composable("resumen_asistencias") {
            ResumenScreen()
        }

        // --- ¡NUEVO! Añade la ruta para el monitor ---
        // La MainActivity se encarga de que solo el admin pueda
        // navegar aquí.
        composable("monitor_empleados") {
            MonitorScreen()
        }

        // --- Rutas de Empleado ---
        // (Usan las mismas pantallas, pero con rutas de inicio diferentes)
        composable("inicio_empleado") {
            InicioAdminScreen() // Reutiliza la pantalla de inicio
        }
        composable("asistencia_empleado") {
            AsistenciaScreen()
        }
        composable("lista_empleado") {
            ListaAdminScreen(
                onTaskNavigate = { taskId ->
                    // Navega a la pantalla de registro con el ID
                    navController.navigate("registrar_admin/$taskId")
                }
            )
        }

        // --- ¡AQUÍ ESTÁ EL CAMBIO! ---
        // --- Ruta de Soporte ---
        composable("lista_soporte") {
            SoporteScreen(
                onServiceNavigate = { serviceId ->
                    // El rol "soporte" usa la MISMA pantalla de detalle que "empleado".
                    // Asumimos que 'serviceId' (tu 'Numero_servicio') es el ID
                    // del documento que 'RegistrarAdminScreen' espera.
                    navController.navigate("registrar_admin/$serviceId")
                }
            )
        }
        // --- FIN DEL CAMBIO ---

        // --- Rutas Compartidas (Admin y Empleado) ---
        composable("perfil") { // 'PerfilAdmin' ahora es solo 'perfil'
            PerfilAdminScreen()
        }
        composable("asistencia") { // 'AsistenciaFragment' ahora es 'asistencia'
            AsistenciaScreen()
        }
        composable("lista_admin") { // Tareas del Admin (tu 'ListaAdmin')
            ListaAdminScreen(
                onTaskNavigate = { taskId ->
                    navController.navigate("registrar_admin/$taskId")
                }
            )
        }

        // --- Ruta con Argumentos (para 'RegistrarAdmin') ---
        composable(
            route = "registrar_admin/{taskId}", // Define un parámetro
            arguments = listOf(navArgument("taskId") { type = NavType.StringType })
        ) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getString("taskId")
            if (taskId != null) {
                RegistrarAdminScreen(
                    taskId = taskId,
                    onTaskCompleted = {
                        // Al completar, regresa a la pantalla anterior
                        navController.popBackStack()
                    }
                )
            } else {
                // Maneja el error (ej. regresa)
                navController.popBackStack()
            }
        }
    }
}