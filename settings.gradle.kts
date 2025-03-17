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
    }
}

rootProject.name = "ThreeInRowWithHistory"
include(":app")

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            version("agp", "8.1.4")
            version("kotlin", "1.9.22")
            
            plugin("android.application", "com.android.application").version("agp")
            plugin("kotlin.android", "org.jetbrains.kotlin.android").version("kotlin")
            
            library("androidx.core.ktx", "androidx.core:core-ktx:1.12.0")
            library("androidx.appcompat", "androidx.appcompat:appcompat:1.6.1")
            library("material", "com.google.android.material:material:1.9.0")
            library("junit", "junit:junit:4.13.2")
            library("androidx.junit", "androidx.test.ext:junit:1.1.5")
            library("androidx.espresso.core", "androidx.test.espresso:espresso-core:3.5.1")
        }
    }
}
 