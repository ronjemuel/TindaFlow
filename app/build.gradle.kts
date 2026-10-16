import java.net.URI
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Do not read server secrets or print configuration values. Empty config builds safely.
val supabaseProperties = Properties().apply {
    val configFile = rootProject.file("supabase.local.properties")
    if (configFile.exists()) configFile.inputStream().use { load(it) }
}
check(supabaseProperties.stringPropertyNames().all {
    it in setOf("SUPABASE_URL", "SUPABASE_PUBLISHABLE_KEY")
}) { "Supabase local configuration may contain only SUPABASE_URL and SUPABASE_PUBLISHABLE_KEY." }
val supabaseUrl = supabaseProperties.getProperty("SUPABASE_URL", "").trim()
val supabaseKey = supabaseProperties.getProperty("SUPABASE_PUBLISHABLE_KEY", "").trim()
check(supabaseKey.isEmpty() || Regex("sb_publishable_[A-Za-z0-9_-]+").matches(supabaseKey)) {
    "Use only a Supabase publishable key (sb_publishable_). Server keys and legacy JWT keys are rejected."
}
check(supabaseUrl.isEmpty() || runCatching {
    val uri = URI(supabaseUrl)
    uri.scheme == "https" && !uri.host.isNullOrBlank() && uri.userInfo == null &&
        uri.port == -1 && uri.rawQuery == null && uri.rawFragment == null &&
        (uri.path.isNullOrEmpty() || uri.path == "/")
}.getOrDefault(false)) { "SUPABASE_URL must be an HTTPS project origin, without credentials, port, query or API path." }
fun buildConfigString(value: String): String = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

android {
    namespace = "com.tindaflow.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.tindaflow.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0-demo"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "SUPABASE_URL", buildConfigString(supabaseUrl))
        buildConfigField("String", "SUPABASE_PUBLISHABLE_KEY", buildConfigString(supabaseKey))
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true; buildConfig = true }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.06.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.core:core-ktx:1.16.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test:runner:1.5.2")
    // supabase-kt 3.2.3 is built with Kotlin 2.2.20 and Ktor 3.2.3.
    implementation(platform("io.github.jan-tennert.supabase:bom:3.2.3"))
    implementation("io.github.jan-tennert.supabase:auth-kt")
    implementation("io.ktor:ktor-client-okhttp:3.2.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
}
