plugins {
    `cpp-library`
}

library {
    targetMachines.set(listOf(machines.linux.x86_64))
    binaries.configureEach {
        compileTask.get().source.from(fileTree("src/main/c") {
            include("**/*.c")
        })
        compileTask.get().compilerArgs.addAll(listOf(
            "-I${System.getProperty("java.home")}/include",
            "-I${System.getProperty("java.home")}/include/linux",
            "-I${projectDir}/src/main/c/include",
            "-xc",
            "-std=c11",
            // Debugging and sanitizers
            "-g",
            "-O0",
            "-fno-omit-frame-pointer",
        ))
    }
}

tasks.withType<LinkSharedLibrary>().configureEach {
    linkerArgs.addAll(listOf(
        "-lportaudio",
        "-lrtlsdr",
    ))
}

tasks.withType<CppCompile>().configureEach {
    dependsOn(":app:generateJniHeaders")
}
