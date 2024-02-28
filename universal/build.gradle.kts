plugins {
    id("sr.project-conventions")
}

val platforms = setOf(
    rootProject.projects.serverrestartsVelocity,
    rootProject.projects.serverrestartsPaper
).map { it.dependencyProject }

tasks {
    jar {
        archiveClassifier.set("")
        archiveFileName.set("ServerRestarts-" + rootProject.version + ".jar")
        destinationDirectory.set(rootProject.projectDir.resolve("build/libs"))
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        platforms.forEach { platform ->
            val shadowJarTask = platform.tasks.named<Jar>("shadowJar").get()
            dependsOn(shadowJarTask)
            dependsOn(platform.tasks.withType<Jar>())
            from(zipTree(shadowJarTask.archiveFile))
        }
    }
}