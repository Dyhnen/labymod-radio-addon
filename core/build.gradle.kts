import net.labymod.labygradle.common.extension.LabyModAnnotationProcessorExtension.ReferenceType

dependencies {
    labyProcessor()
    api(project(":api"))

    // JLayer is LGPL-2.1 and supplies the MP3 decoder used by the streaming player.
    addonMavenDependency("javazoom:jlayer:1.0.1")

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.11.4")
}

labyModAnnotationProcessor {
    referenceType = ReferenceType.DEFAULT
}

tasks.test {
    useJUnitPlatform()
}

tasks.named<JavaCompile>("compileTestJava") {
    // AddonProcessor is only meaningful for production sources. Without the
    // production addon options it intentionally rejects a test compilation.
    options.compilerArgs.add("-proc:none")
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-Xlint:deprecation")
}
