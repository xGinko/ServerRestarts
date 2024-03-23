plugins {
    id("sr.project-conventions")
    alias(libs.plugins.shadow)
    alias(libs.plugins.runvelocity)
}

dependencies {
    implementation(projects.serverrestartsCommon)
    implementation(libs.configmaster)

    compileOnly(libs.velocity)
    annotationProcessor(libs.velocity)
}

tasks {
    runVelocity {
        version(libs.versions.runvelocityversion.get())
    }

    build.configure {
        dependsOn("shadowJar")
    }

    shadowJar {
        archiveFileName = "${rootProject.name}-${project.version}-velocity.jar"
    }
}