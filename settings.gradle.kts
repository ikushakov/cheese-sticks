pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://artifactory-external.vkpartner.ru/artifactory/maven")
        }
        // Maptiler SDK repository - попробуем разные варианты
        maven {
            url = uri("https://repo.maptiler.com/maven")
        }
        maven {
            url = uri("https://maven.maptiler.com/releases")
        }
        maven {
            url = uri("https://repo.maptiler.com/releases")
        }
        maven {
            url = uri("https://jitpack.io")
        }
    }
}

rootProject.name = "Semimanufactures"
include(":app")
 