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
            "-std=c11"
        ))
    }
}

tasks.withType<LinkSharedLibrary>().configureEach {
    linkerArgs.addAll(listOf("-lportaudio"))
}

tasks.withType<CppCompile>().configureEach {
    dependsOn(":app:generateJniHeaders")
}
