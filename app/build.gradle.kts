import java.util.Properties

plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
val releaseProperties = Properties().apply {
    rootProject.file(".local/release-signing.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}
fun signingValue(env: String, property: String): String? = System.getenv(env) ?: releaseProperties.getProperty(property)
val releaseStore = signingValue("PADBOARD_KEYSTORE_PATH", "storeFile")
android {
    namespace = "org.padboard.keyboard"
    compileSdk = 35
    defaultConfig {
        applicationId = "org.padboard.keyboard"
        minSdk = 26
        targetSdk = 35
        versionCode = 101
        versionName = "1.0.1"
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.all {
            it.systemProperty("java.io.tmpdir", layout.buildDirectory.dir("test-temp").get().asFile.apply { mkdirs() }.absolutePath)
            it.systemProperty("user.home", layout.buildDirectory.dir("test-home").get().asFile.apply { mkdirs() }.absolutePath)
            it.systemProperty("robolectric.dependency.repo.url", "https://repo.maven.apache.org/maven2")
            it.systemProperty("maven.repo.local", layout.buildDirectory.dir("test-maven").get().asFile.absolutePath)
            it.jvmArgs("--add-opens=java.base/java.lang=ALL-UNNAMED", "--add-opens=java.base/java.util=ALL-UNNAMED")
        }
    }
    signingConfigs.getByName("debug") {
        storeFile = rootProject.file(".local/debug.keystore")
    }
    if (releaseStore != null) {
        signingConfigs.create("release") {
            storeFile = rootProject.file(releaseStore)
            storePassword = signingValue("PADBOARD_STORE_PASSWORD", "storePassword")
            keyAlias = signingValue("PADBOARD_KEY_ALIAS", "keyAlias")
            keyPassword = signingValue("PADBOARD_KEY_PASSWORD", "keyPassword")
        }
    }
    buildTypes.getByName("release") {
        isDebuggable = false
        isMinifyEnabled = false
        if (releaseStore != null) signingConfig = signingConfigs.getByName("release")
    }
}
dependencies { testImplementation("junit:junit:4.13.2"); testImplementation("org.robolectric:robolectric:4.14.1") }
val createLocalDebugKey by tasks.registering(Exec::class) {
    val key = rootProject.file(".local/debug.keystore")
    onlyIf { !key.exists() }
    doFirst { key.parentFile.mkdirs() }
    commandLine(
        "${System.getProperty("java.home")}/bin/keytool", "-genkeypair", "-noprompt",
        "-keystore", key.absolutePath, "-storepass", "android", "-keypass", "android",
        "-alias", "androiddebugkey", "-dname", "CN=Android Debug,O=Android,C=US",
        "-keyalg", "RSA", "-keysize", "2048", "-validity", "10000"
    )
}
tasks.configureEach { if (name == "validateSigningDebug") dependsOn(createLocalDebugKey) }
