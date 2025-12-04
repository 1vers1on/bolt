plugins {
    java
    application
}

val lwjglVersion = "3.3.4"
val imguiVersion = "1.90.0"
val lwjglNatives = "natives-linux"

dependencies {
    implementation("org.openimaj:JTransforms:1.3.10")
    implementation("org.apache.commons:commons-math3:3.6.1")
    implementation("org.slf4j:slf4j-api:2.0.17")
    implementation("ch.qos.logback:logback-classic:1.5.21")
    
    implementation(platform("org.lwjgl:lwjgl-bom:$lwjglVersion"))
    
    implementation("org.lwjgl:lwjgl")
    implementation("org.lwjgl:lwjgl-glfw")
    implementation("org.lwjgl:lwjgl-opengl")
    
    runtimeOnly("org.lwjgl:lwjgl::$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-glfw::$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-opengl::$lwjglNatives")

    implementation("io.github.spair:imgui-java-binding:$imguiVersion")
    implementation("io.github.spair:imgui-java-lwjgl3:$imguiVersion")
    implementation("io.github.spair:imgui-java-natives-linux:$imguiVersion")

    implementation("com.google.code.gson:gson:2.13.2")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

application {
    mainClass = "net.ellie.bolt.Main"
}

tasks.register<Exec>("generateJniHeaders") {
    project.evaluationDependsOn(":native-jni")

    val jniProjectDir = project(":native-jni").projectDir
    val outputDir = file("$jniProjectDir/src/main/c/include")
    val nativeSourceDir = file("src/main/java/net/ellie/bolt/jni")
    val nativeSourceFiles = fileTree(nativeSourceDir) {
        include("**/*.java")
    }

    dependsOn(tasks.classes)
    val classesDir = sourceSets.main.get().output.classesDirs.singleFile
    
    inputs.files(nativeSourceFiles)
    outputs.dir(outputDir)

    val sourceFiles = nativeSourceFiles.files.map { it.absolutePath }

    val javaCompiler = javaToolchains.compilerFor {
        languageVersion = JavaLanguageVersion.of(21)
    }
    val javacPath = javaCompiler.get().executablePath.asFile.absolutePath

    commandLine(
        javacPath,
        "-h", outputDir.absolutePath,
        "-d", classesDir.absolutePath,
        "-cp", sourceSets.main.get().compileClasspath.asPath + ":" + classesDir.absolutePath,
        *sourceFiles.toTypedArray()
    )
}

tasks.named("assemble") {
    dependsOn(":native-jni:linkRelease")
}

tasks.named("generateJniHeaders") {
    finalizedBy(":native-jni:linkRelease")
}

tasks.named<JavaExec>("run") {
    dependsOn(":native-jni:linkRelease")
    val jniProject = project(":native-jni")
    val libDir = jniProject.layout.buildDirectory.dir("lib/main/release")
    systemProperty("java.library.path", libDir.get().asFile.absolutePath)
}

tasks.jar {
    dependsOn(":native-jni:linkRelease")
    manifest {
        attributes(
            "Main-Class" to "net.ellie.bolt.Main"
        )
    }
    
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
}
