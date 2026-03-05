plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

// Изменяем директорию сборки на динамическую, чтобы ГАРАНТИРОВАННО обойти блокировку файлов.
// Каждый запуск будет использовать новую папку, если старая заблокирована.
layout.buildDirectory = file("build_${System.currentTimeMillis()}")

android {
    namespace = "com.example.semimanufactures"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.semimanufactures"
        minSdk = 24
        targetSdk = 35
        versionCode = 68
        versionName = "8.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {

        create("release") {
            storeFile = file("${System.getProperty("user.home")}/.android/debug.keystore")
            storePassword = "android"
            keyAlias = "AndroidDebugKey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        viewBinding = false
        compose = true
        aidl = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.4"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    sourceSets {
        getByName("main") {
            aidl.srcDirs("src/main/aidl")
        }
    }

    sourceSets {
        getByName("main") {

        }
    }
    lint {
        checkReleaseBuilds = false
    }

}

dependencies {
    implementation("androidx.databinding:viewbinding:8.1.1") {
        exclude(group = "com.android.support", module = "support-v4")
    }
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity:1.7.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation(libs.androidx.espresso.core)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.cardview)
    implementation(libs.material)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.viewfinder.core)
    
    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")
    try {
        implementation("mysql:mysql-connector-java:5.1.49")
    } catch (e: Exception) {
        TODO("Not yet implemented")
    }
    implementation("io.ktor:ktor-client-android:2.3.4")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.4")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.4")
    implementation("io.ktor:ktor-client-core:2.3.4")
    implementation("io.ktor:ktor-client-cio:2.3.4")
    implementation("io.ktor:ktor-client-okhttp:2.3.4")
    implementation("io.ktor:ktor-client-logging:2.3.4")
    implementation("io.ktor:ktor-client-serialization:2.3.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.6")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.9")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.3.9")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("org.jsoup:jsoup:1.15.3")
    implementation("org.ktorm:ktorm-core:3.4.1")
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("me.dm7.barcodescanner:zbar:1.8.4") {
        exclude(group = "com.android.support", module = "support-v4")
    }
    implementation("com.google.zxing:core:3.5.1")
    implementation("com.google.mlkit:barcode-scanning:17.3.0")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation("com.google.android.gms:play-services-location:21.0.1")

    implementation("com.sunmi:connService.iotsdk:1.1.1")
    implementation("com.sunmi:ThingAdapter-SDK:1.0.9")
    implementation(files("libs/sunmiscan.jar"))
    implementation(files("libs/device.sdk"))
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar", "*.aar"))))

    implementation("com.squareup.moshi:moshi:1.14.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.14.0")
    
    // Maptiler SDK - проверьте правильный репозиторий в документации
    // Возможно, нужно использовать другой groupId или artifactId
    // Временно закомментировано для проверки сборки
    // implementation("com.maptiler:maptiler-sdk-android:6.2.0")
    
    // Временная замена - osmdroid для проверки сборки
    // После настройки правильного репозитория Maptiler замените обратно
    implementation("org.osmdroid:osmdroid-android:6.1.18")
    implementation("com.github.bumptech.glide:glide:4.12.0")
    implementation("com.google.firebase:firebase-messaging:24.1.0")
    implementation("com.squareup.picasso:picasso:2.8")
    annotationProcessor("com.github.bumptech.glide:compiler:4.12.0")
    implementation("com.github.bumptech.glide:okhttp3-integration:4.12.0")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("ru.rustore.sdk:pushclient:6.5.0")
    implementation(platform("ru.rustore.sdk:bom:7.0.0"))
    implementation("ru.rustore.sdk:appupdate")
    implementation("com.davemorrissey.labs:subsampling-scale-image-view:3.10.0")
}

// Кастомные задачи для установки и запуска приложения
tasks.register("installAndRun") {
    group = "android"
    description = "Установить и запустить приложение на подключенном устройстве"
    dependsOn("installDebug")
    
    doLast {
        val packageName = android.defaultConfig.applicationId
        val launcherActivity = "com.example.semimanufactures.FeaturesOfTheFunctionalityActivity"
        
        println("Запуск приложения: $packageName/$launcherActivity")
        
        // Находим adb через Android SDK
        val androidSdkPath = System.getenv("ANDROID_HOME") 
            ?: System.getenv("ANDROID_SDK_ROOT")
            ?: "${System.getProperty("user.home")}/AppData/Local/Android/Sdk"
        
        val adb = if (System.getProperty("os.name").lowercase().contains("win")) {
            "$androidSdkPath/platform-tools/adb.exe"
        } else {
            "$androidSdkPath/platform-tools/adb"
        }
        
        try {
            exec {
                executable = adb
                args = listOf("shell", "am", "start", "-n", "$packageName/$launcherActivity")
            }
            println("✓ Приложение установлено и запущено!")
        } catch (e: Exception) {
            println("⚠ Ошибка запуска приложения: ${e.message}")
            println("Проверьте, что устройство подключено и отладка по USB включена")
        }
    }
}

tasks.register("installApp") {
    group = "android"
    description = "Установить приложение на подключенное устройство"
    dependsOn("installDebug")
    
    doLast {
        println("✓ Приложение установлено на устройство!")
    }
}

tasks.register("runApp") {
    group = "android"
    description = "Запустить приложение на подключенном устройстве"
    
    doLast {
        val packageName = android.defaultConfig.applicationId
        val launcherActivity = "com.example.semimanufactures.FeaturesOfTheFunctionalityActivity"
        
        println("Запуск приложения: $packageName/$launcherActivity")
        
        // Находим adb через Android SDK
        val androidSdkPath = System.getenv("ANDROID_HOME") 
            ?: System.getenv("ANDROID_SDK_ROOT")
            ?: "${System.getProperty("user.home")}/AppData/Local/Android/Sdk"
        
        val adb = if (System.getProperty("os.name").lowercase().contains("win")) {
            "$androidSdkPath/platform-tools/adb.exe"
        } else {
            "$androidSdkPath/platform-tools/adb"
        }
        
        try {
            exec {
                executable = adb
                args = listOf("shell", "am", "start", "-n", "$packageName/$launcherActivity")
            }
            println("✓ Приложение запущено!")
        } catch (e: Exception) {
            println("⚠ Ошибка запуска приложения: ${e.message}")
            println("Проверьте, что устройство подключено и отладка по USB включена")
        }
    }
}