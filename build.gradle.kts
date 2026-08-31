import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ksp)
    alias(libs.plugins.protobuf)
}

android {
    namespace = "com.yoyo.launcher"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.yoyo.launcher"
        minSdk = 33
        targetSdk = 37
        versionCode = 102
        versionName = "1.0.2stable"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        javaCompileOptions {
            annotationProcessorOptions {
                arguments += mapOf("dagger.fastInit" to "enabled")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    sourceSets {
        getByName("main") {
            java.directories.clear()
            java.directories.add("src")
            java.directories.add("src_no_quickstep")
            java.directories.add("src_build_config")
            java.directories.add("shared/src")

            kotlin.directories.clear()
            kotlin.directories.add("src")
            kotlin.directories.add("src_no_quickstep")
            kotlin.directories.add("src_build_config")
            kotlin.directories.add("shared/src")

            res.directories.clear()
            res.directories.add("res")
        }
    }

    buildFeatures {
        viewBinding = true
        buildConfig = false
    }
}

androidComponents {
    onVariants { variant ->
        variant.outputs.forEach { output ->
            output.outputFileName.set("YoyoLauncher-v${android.defaultConfig.versionName}-${variant.name}.apk")
        }
    }
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
        freeCompilerArgs.add("-Xjvm-default=all")
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.viewpager2)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.common.java8)
    implementation(libs.androidx.window)
    implementation(libs.kotlinx.coroutines.android)
    
    implementation(libs.dagger)
    ksp(libs.dagger.compiler)
    
    implementation(libs.protobuf.javalite)
    implementation(libs.guava)
    
    implementation(libs.androidx.annotation)
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.preference)
    implementation(libs.androidx.dynamicanimation)
    implementation(libs.androidx.slice.view)
    implementation(libs.androidx.cardview)
    implementation(libs.androidx.graphics.shapes)

    // Local JARs
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:${libs.versions.protobuf.get()}"
    }
    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                create("java") {
                    option("lite")
                }
            }
        }
    }
}
