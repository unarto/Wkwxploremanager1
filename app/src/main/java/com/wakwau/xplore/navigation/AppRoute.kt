// [Jalur Class/Modul]: app/src/main/java/com/wakwau/xplore/navigation/AppRoute.kt
// [Penjelasan]: Mendefinisikan object routes untuk Compose Navigation agar terhindar dari hardcoded string secara eksplisit, tanpa menambah dependency serialization karena dilarang mengedit gradle.
package com.wakwau.xplore.navigation

sealed class AppRoute(val route: String) {
    object Permission : AppRoute("permission_screen")
    object DualPane : AppRoute("dual_pane_screen")
    object Settings : AppRoute("settings_screen")
}
