import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import net.minecrell.pluginyml.bukkit.BukkitPluginDescription

plugins {
    java
    id("dev.clojurephant.clojure") version "0.5.0-alpha.5"
    id("net.minecrell.licenser") version "0.4.1"
    id("net.minecrell.plugin-yml.bukkit") version "0.3.0"
    id("com.github.johnrengelman.shadow") version "5.0.0"
}

group = "eu.mikroskeem"
version = "0.0.1-SNAPSHOT"

val paperApiVersion = "1.14.3-SNAPSHOT"
val throwingFunctionVersion = "1.5.0"
val clojureVersion = "1.10.1"

repositories {
    mavenCentral()
    mavenLocal()
    maven("https://clojars.org/repo")

    maven("https://papermc.io/repo/repository/maven-public")
    maven("https://repo.wut.ee/repository/vapeout-repo")
}

dependencies {
    implementation("org.clojure:clojure:$clojureVersion")
    implementation("com.pivovarit:throwing-function:$throwingFunctionVersion")

    compileOnly("com.destroystokyo.paper:paper-api:$paperApiVersion")
}

apply(from = "clojure.gradle")

license {
    header = rootProject.file("etc/HEADER")
    filter.include("**/*.java")
}

bukkit {
    name = "BukkitClj"
    main = "eu.mikroskeem.bukkitclj.BukkitClj"
    authors = listOf("mikroskeem")
    description = "Clojure scripting on Bukkit"
    website = "https://mikroskeem.eu"
    apiVersion = "1.14"

    permissions {
        create("bukkitclj.admin") {
            default = BukkitPluginDescription.Permission.Default.OP
        }
    }

    commands {
        create("bukkitclj") {
            permission = "bukkitclj.admin"
        }
    }
}

val shadowJar by tasks.getting(ShadowJar::class) {
    val target = "eu.mikroskeem.bukkitclj.lib"
    val relocations = listOf(
            //"clojure",
            "com.pivovarit.function"
    )
    relocations.forEach {
        relocate(it, "$target.$it")
    }

    exclude("META-INF/maven/**")
}

tasks["build"].dependsOn(shadowJar)