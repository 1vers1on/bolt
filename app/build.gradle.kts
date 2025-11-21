plugins {
    java
    application
}

val lwjglVersion = "3.3.4"
val imguiVersion = "1.86.4"
val lwjglNatives = "natives-linux"

dependencies {
    implementation("org.openimaj:JTransforms:1.3.10")
    
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
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

application {
    mainClass = "net.ellie.bolt.Main"
}
