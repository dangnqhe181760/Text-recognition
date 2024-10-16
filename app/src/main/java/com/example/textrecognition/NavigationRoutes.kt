package com.example.textrecognition

sealed class NavigationRoutes(val route: String) {
    object Login : NavigationRoutes("login")
    object Home : NavigationRoutes("home")

    // Route with a dynamic argument (like contactId)
//    object ApiDetailContact : NavigationRoutes("api_detail_contact/{contactId}") {
//        fun createRoute(contactId: String): String = "api_detail_contact/$contactId"
//    }
}
