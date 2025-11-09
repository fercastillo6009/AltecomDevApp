package com.example.fondodepantalla.Utils // 1. Mismo paquete que LocationHelper

/**
 * Objeto singleton para mantener constantes de la aplicación.
 * En Kotlin, 'object' reemplaza la necesidad de 'public class' con 'static final'.
 */
object Constants {

    // 2. 'public static final' se convierte en 'const val'
    // 'const' significa que es una constante de tiempo de compilación.
    const val COMPANY_LATITUDE = 25.427673
    const val COMPANY_LONGITUDE = -103.278833
    const val ALLOWED_RADIUS_METERS = 200.0

    //TEST
    // const val COMPANY_LATITUDE = 25.510174
    // const val COMPANY_LONGITUDE = -103.385471
    // const val ALLOWED_RADIUS_METERS = 200.0
}