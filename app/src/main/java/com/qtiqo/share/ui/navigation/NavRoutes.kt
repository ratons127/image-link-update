package com.qtiqo.share.ui.navigation

sealed class Route(val path: String) {
    data object Login : Route("auth/login")
    data object SignUp : Route("auth/signup")
    data object Forgot : Route("auth/forgot")
    data object Gallery : Route("gallery")
    data object Upload : Route("upload")
    data object Profile : Route("profile")
    data object Admin : Route("admin")
    data object FileDetail : Route("detail/{fileId}") {
        fun create(fileId: String) = "detail/$fileId"
    }
    data object Public : Route("public/{token}") {
        fun create(token: String) = "public/$token"
    }
}

val bottomRoutes = setOf(Route.Gallery.path, Route.Upload.path, Route.Profile.path, Route.Admin.path)
