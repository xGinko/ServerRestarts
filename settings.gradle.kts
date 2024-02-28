enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    plugins {
        id("io.papermc.paperweight.userdev") version "1.5.11"
    }
}

plugins {
    id("com.gradle.enterprise") version "3.16.1"
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.7.0"
}

dependencyResolutionManagement {
    @Suppress("UnstableAPIUsage")
    repositories {
        maven("https://nexus.velocitypowered.com/repository/maven-public/") {
            name = "velocity-repo"
        }
        maven("https://repo.papermc.io/repository/maven-public/") {
            name = "papermc-repo"
        }
        maven("https://ci.pluginwiki.us/plugin/repository/everything/") {
            name = "configmaster-repo"
        }
        maven("https://repo.aikar.co/content/groups/aikar/") {
            name = "aikar-repo"
        }
        mavenCentral()
    }
}

rootProject.name = "ServerRestarts"

gradleEnterprise {
    buildScan {
        if (!System.getenv("CI").isNullOrEmpty()) {
            termsOfServiceUrl = "https://gradle.com/terms-of-service"
            termsOfServiceAgree = "yes"
        }
    }
}

setupSRSubproject("folia")
setupSRSubproject("common")
setupSRSubproject("paper")
setupSRSubproject("velocity")

setupSubproject("serverrestarts") {
    projectDir = file("universal")
}

fun setupSRSubproject(name: String) {
    setupSubproject("serverrestarts-$name") {
        projectDir = file(name)
    }
}

inline fun setupSubproject(name: String, block: ProjectDescriptor.() -> Unit) {
    include(name)
    project(":$name").apply(block)
}