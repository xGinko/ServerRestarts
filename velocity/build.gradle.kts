plugins {
    id("sr.project-conventions")
    alias(libs.plugins.shadow)
    alias(libs.plugins.runvelocity)
}

tasks {
    runVelocity {
        version(libs.versions.runvelocityversion.get())
    }
}

dependencies {
    implementation(projects.serverrestartsCommon)
    implementation(libs.configmaster)

    compileOnly(libs.velocity)
    annotationProcessor(libs.velocity)
}

tasks.build.configure {
    dependsOn("shadowJar")
}

tasks.shadowJar {
    archiveFileName = "${rootProject.name}-${project.version}-velocity.jar"
}