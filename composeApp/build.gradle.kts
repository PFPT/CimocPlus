plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.android.library")
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }
    jvm("desktop") {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":shared"))
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material)
                implementation(compose.material3)
                implementation(compose.materialIconsExtended)
                implementation(compose.ui)
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
                implementation("cafe.adriel.voyager:voyager-navigator:1.0.0")
                implementation("cafe.adriel.voyager:voyager-tab-navigator:1.0.0")
                implementation("io.insert-koin:koin-core:3.5.6")
                implementation("io.insert-koin:koin-compose:1.1.5")
            }
        }
        val androidMain by getting {
            dependencies {
                implementation("io.coil-kt.coil3:coil-compose:3.0.0")
                implementation("io.coil-kt.coil3:coil-network-okhttp:3.0.0")
                implementation("com.davemorrissey.labs:subsampling-scale-image-view-androidx:3.10.0")
            }
        }
        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation("io.coil-kt.coil3:coil-compose:3.0.0")
                implementation("io.coil-kt.coil3:coil-network-okhttp:3.0.0")
            }
        }
    }
}

android {
    namespace = "com.haleydu.cimoc.compose"
    compileSdk = 35
    defaultConfig {
        minSdk = 21
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions.jvmTarget = "17"
}
