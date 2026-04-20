plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.chinese_game"
    compileSdk = 36

    sourceSets {
        getByName("main") {
            res.srcDirs("src/main/res", "src/main/res_ui_icons")
        }
    }


    defaultConfig {
        applicationId = "com.example.chinese_game"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }


    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    // HanLP 中文分词（portable 版，适合 Android）
    implementation("com.hankcs:hanlp:portable-1.8.6")
    // 汉字转拼音（用于 sentence_words 词条标注拼音）
    implementation("com.belerweb:pinyin4j:2.5.0")
    
    // 科大讯飞SparkChain语音识别SDK
    implementation(files("libs/SparkChain.aar"))
    implementation(files("libs/Codec.aar"))
    // SparkChain SDK内部依赖Gson解析JSON
    implementation("com.google.code.gson:gson:2.8.9")
    // OkHttp用于讯飞ISE语音评测WebSocket
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.monitor)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
