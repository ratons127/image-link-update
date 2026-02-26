# Admin Module (Qtiqo Share)

## FakeBackend
- Uses your existing `useFakeBackend` flag (via adapter in `feature/admin/di/AdminModule.kt`).
- When enabled, `SwitchingAdminRepository` routes to `FakeAdminRepository` + `FakeAdminBackend`.

## Navigation Integration
- Route: `admin`
- Plug into existing NavHost:
  - `com.qtiqo.share.feature.admin.ui.AdminNavGraph`

Example:
```kotlin
navController.graph {
    AdminNavGraph(
        onBack = { navController.popBackStack() },
        onOpenShareLink = { link -> navController.navigate("public/${link.substringAfterLast("/s/")}") }
    )
}
```

## Conditional Bottom Nav
- Use `adminBottomNavItemOrNull(session)` from `feature/admin/ui/AdminNavigation.kt`
- Returns `null` for non-admin users.

## Required Dependencies (snippet)
```kotlin
implementation("androidx.media3:media3-exoplayer:1.4.1")
implementation("androidx.media3:media3-ui:1.4.1")
```
