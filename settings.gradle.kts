pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Repositorio para la librería SweetAlert
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "Diamantes pro players Go"
include(":app")
