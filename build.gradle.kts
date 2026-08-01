plugins {
    // 8.7.3 вместо 8.4.2: Compose Multiplatform 1.8.0 требует компиляцию против
    // Android API 35, а AGP 8.4.2 поддерживает максимум 34.
    id("com.android.application") version "8.7.3" apply false
    id("com.android.library") version "8.7.3" apply false

    // Kotlin поднят с 1.9.24 до 2.1.20 для ВСЕХ модулей (включая старый :app) —
    // Gradle не позволяет держать две версии одного и того же плагина в одной
    // сборке, а Compose Multiplatform 1.8+ (стабильный iOS-таргет) требует Kotlin 2.1+.
    // Compose Compiler с Kotlin 2.0 — отдельный плагин (id ниже), а не часть kotlin-android.
    id("org.jetbrains.kotlin.android") version "2.1.20" apply false
    id("org.jetbrains.kotlin.multiplatform") version "2.1.20" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.20" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.1.20" apply false
    id("org.jetbrains.compose") version "1.8.0" apply false
}
