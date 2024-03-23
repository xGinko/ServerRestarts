plugins {
    id("sr.project-conventions")
    alias(libs.plugins.shadow)
    alias(libs.plugins.runpaper)
}

dependencies {
    implementation(projects.serverrestartsCommon)
    implementation(projects.serverrestartsFolia)

    compileOnly(libs.paper)

    implementation(libs.bstats)
    implementation(libs.caffeine)
    implementation(libs.configmaster)
}

runPaper.folia.registerTask()

tasks {
    runServer {
        minecraftVersion(libs.versions.runpaperversion.get())
    }

    build.configure {
        dependsOn("shadowJar")
    }

    shadowJar {
        archiveFileName = "${rootProject.name}-${project.version}-paper.jar"
        exclude(
            "LICENSE",
            "META-INF/maven/**",
            "META-INF/**/module-info.class",
            "META-INF/MANIFEST.MF",
            "META-INF/LICENSE",
            "META-INF/LICENSE.txt",
            "META-INF/NOTICE.txt"
        )
        relocate("com.github.benmanes.caffeine", "me.xginko.serverrestarts.libs.caffeine")
        relocate("org.bstats", "me.xginko.serverrestarts.libs.bstats")
    }
}
