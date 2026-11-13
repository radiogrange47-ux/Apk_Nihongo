plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val releaseSigningEnvironment = mapOf(
    "NIHONGO_KEYSTORE_FILE" to providers.environmentVariable("NIHONGO_KEYSTORE_FILE").orNull,
    "NIHONGO_KEYSTORE_PASSWORD" to providers.environmentVariable("NIHONGO_KEYSTORE_PASSWORD").orNull,
    "NIHONGO_KEY_ALIAS" to providers.environmentVariable("NIHONGO_KEY_ALIAS").orNull,
    "NIHONGO_KEY_PASSWORD" to providers.environmentVariable("NIHONGO_KEY_PASSWORD").orNull
)
val releaseSigningReady = releaseSigningEnvironment.values.all { !it.isNullOrBlank() }
val missingReleaseSigningValues = releaseSigningEnvironment
    .filterValues { it.isNullOrBlank() }
    .keys

android {
    namespace = "fr.nihongo.app"
    compileSdk = 35

    signingConfigs {
        if (releaseSigningReady) {
            create("release") {
                storeFile = file(checkNotNull(releaseSigningEnvironment["NIHONGO_KEYSTORE_FILE"]))
                storePassword = checkNotNull(releaseSigningEnvironment["NIHONGO_KEYSTORE_PASSWORD"])
                keyAlias = checkNotNull(releaseSigningEnvironment["NIHONGO_KEY_ALIAS"])
                keyPassword = checkNotNull(releaseSigningEnvironment["NIHONGO_KEY_PASSWORD"])
            }
        }
    }

    defaultConfig {
        applicationId = "fr.nihongo.app"
        minSdk = 26
        targetSdk = 35
        versionCode = providers.gradleProperty("appVersionCode").orElse("1").get().toInt()
        versionName = providers.gradleProperty("appVersionName").orElse("0.1.0").get()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (releaseSigningReady) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

tasks.configureEach {
    if (name == "assembleRelease" || name == "packageRelease" || name == "bundleRelease") {
        doFirst {
            if (!releaseSigningReady) {
                throw GradleException(
                    "Release non signée : renseignez les variables d’environnement " +
                        missingReleaseSigningValues.joinToString(", ") + "."
                )
            }
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.core:core-ktx:1.15.0")

    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
