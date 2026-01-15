import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import net.minecrell.pluginyml.bukkit.BukkitPluginDescription

plugins {
    java
    id("dev.clojurephant.clojure") version "0.8.0"
    id("dev.yumi.gradle.licenser") version "2.2.1"
    id("net.minecrell.plugin-yml.bukkit") version "0.6.0"
    id("com.gradleup.shadow") version "9.3.0"
}

group = "eu.mikroskeem"
version = "0.0.1-SNAPSHOT"

val paperApiVersion = "1.21.11-R0.1-SNAPSHOT"
val clojureVersion = "1.12.4"
val clojureAsyncVersion = "1.8.741"
val pomegranateVersion = "1.2.25"

repositories {
    mavenCentral()
    maven("https://clojars.org/repo")

    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    implementation("org.clojure:clojure:$clojureVersion")
    implementation("org.clojure:core.async:$clojureAsyncVersion")
    implementation("clj-commons:pomegranate:$pomegranateVersion")

    compileOnly("io.papermc.paper:paper-api:$paperApiVersion")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

apply(from = "clojure.gradle")

license {
    rule(file("etc/HEADER"))

    include("**/*.java")
}

bukkit {
    name = "BukkitClj"
    main = "eu.mikroskeem.bukkitclj.BukkitClj"
    authors = listOf("mikroskeem")
    description = "Clojure scripting on Bukkit"
    website = "https://mikroskeem.eu"
    apiVersion = "1.21.10"

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
    val relocations = listOf<String>(
            //"clojure",
    )
    relocations.forEach {
        relocate(it, "$target.$it")
    }

    exclude("META-INF/maven/**")
    // Do not include pprint AOTed classes, they break classloader chain
    // clojure/pprint/proxy$java.io.Writer*.class to be exact
    exclude("clojure/pprint/**/*.class")

    manifest {
        attributes["paperweight-mappings-namespace"] = "mojang"
    }
}

tasks["build"].dependsOn(shadowJar)
