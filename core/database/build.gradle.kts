plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.lengcs.fkwakeup.core.database"
    compileSdk = 35
    useLibrary("android.test.runner")
    useLibrary("android.test.base")

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "android.test.InstrumentationTestRunner"
        // 平台 runner 全量扫描依赖 dex 在 API 35 上可能启动超时，显式指定本模块测试。
        testInstrumentationRunnerArguments["class"] = "com.lengcs.fkwakeup.core.database.SectionPersistenceTest"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    api(project(":core:model"))
    implementation(project(":core:common"))
    implementation(project(":core:datastore"))

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)
}
