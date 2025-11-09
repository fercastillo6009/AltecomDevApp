package com.example.fondodepantalla // O tu paquete de navegación

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.fondodepantalla.screens.*

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