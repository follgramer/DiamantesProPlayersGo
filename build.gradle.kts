// ==============================================================================
//      Buildscript a Nivel de Proyecto: Solo declara los plugins
// ==============================================================================

plugins {
    // Usa los alias del archivo libs.versions.toml
    // El "apply false" indica que la configuración se hará en el módulo de la app
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.jetbrains.kotlin.android) apply false
    alias(libs.plugins.google.gms.services) apply false
}