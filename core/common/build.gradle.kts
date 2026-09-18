plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    // M1 将在此实现 WeekSpecParser 与时间工具
    testImplementation(libs.junit)
    testImplementation(libs.truth)
}
