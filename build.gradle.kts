import java.util.*

plugins {
  `java-library`
  `maven-publish`
  id("io.papermc.paperweight.userdev") version "2.0.0-beta.18"
  id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "ru.shk"
version = "1.8.0.3"

paperweight.reobfArtifactConfiguration = io.papermc.paperweight.userdev.ReobfArtifactConfiguration.MOJANG_PRODUCTION

val nexusRepository = Properties()
nexusRepository.load(file("nexus.properties").inputStream())
publishing {
  repositories {
    mavenLocal()
    maven {
      url = uri("https://nexus.shoker.su/repository/maven-releases/")
      credentials {
        username = "${nexusRepository["user"]}"
        password = "${nexusRepository["password"]}"
      }
    }
  }
  publications {
    create<MavenPublication>("maven") {
      groupId = "${group}"
      artifactId = "${project.name}"
      version = "${version}"
      artifact(tasks.shadowJar)
    }
  }
}
java {
  toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}
repositories {
  mavenLocal()
  mavenCentral()
  maven("https://jitpack.io")
  maven("https://nexus.prgm.in/repository/maven-public/")
  maven("https://repo.md5lukas.de/public")
  maven { url = uri("https://repo.codemc.io/repository/maven-releases/") }
  maven {
    url = uri("https://nexus.shoker.su/repository/maven-releases/")
  }
  maven {
    url = uri("https://repo.papermc.io/repository/maven-public/")
  }
  maven {
    url = uri("https://repo.papermc.io/repository/maven-snapshots/")
  }
  maven {
    url = uri("https://oss.sonatype.org/content/groups/public/")
  }
  maven {
    url = uri("https://libraries.minecraft.net/")
  }
  maven {
    url = uri("https://repo.codemc.io/repository/maven-snapshots/")
  }
  maven {
    url = uri("https://mvn.exceptionflug.de/repository/exceptionflug-public/")
  }
  maven {
    url = uri("https://maven.enginehub.org/repo/")
  }
  maven {
    url = uri("https://nexus.shoker.su/repository/maven-shield/")
    credentials {
      username = "${nexusRepository["user"]}"
      password = "${nexusRepository["password"]}"
    }
  }
  maven {
    url = uri("https://simonsator.de/repo")
  }
}
dependencies {
  paperweight.paperDevBundle("1.21.8-R0.1-SNAPSHOT")
  implementation("com.github.Carleslc.Simple-YAML:Simple-Yaml:1.8.4")
  compileOnly("com.github.retrooper:packetevents-spigot:2.5.0")
  compileOnly(files("E:\\IdeaProjects\\commons-lang\\target\\commons-lang3-3.13.0-SNAPSHOT.jar"))
  compileOnly("com.sk89q.worldedit:worldedit-bukkit:7.2.9")
  compileOnly("com.sk89q.worldedit:worldedit-core:7.2.0-SNAPSHOT")
  implementation("commons-io:commons-io:2.14.0")
  compileOnly(files("E:/Libraries/protocolize-velocity.jar"))
  implementation("de.md5lukas:anvilgui:2.0.0-SNAPSHOT") {
    isTransitive = false
  }
  compileOnly("org.apache.logging.log4j:log4j-core:2.17.1")
  implementation("org.apache.commons:commons-lang3:3.18.0")
  compileOnly("com.mojang:authlib:6.0.54")
  compileOnly("de.simonsator:BungeecordPartyAndFriends:1.0.92")
  compileOnly("de.simonsator:VelocityPoweredPartyAndFriends:1.0.91")
  implementation("net.kyori:adventure-platform-bungeecord:4.3.2")

  compileOnly("io.github.waterfallmc:waterfall-api:1.20-R0.3-SNAPSHOT")
  implementation("org.projectlombok:lombok:1.18.36")
  annotationProcessor("org.projectlombok:lombok:1.18.36")
  implementation("redis.clients:jedis:5.2.0")

  compileOnly("land.shield:PlayerAPI:1.8.4")
  compileOnly("ru.shk:MySQLAPI:3.4.0")

  compileOnly(files("D:/Libraries/ProtocolLib.jar"))
  compileOnly("com.velocitypowered:velocity-api:3.4.0-SNAPSHOT")
  annotationProcessor("com.velocitypowered:velocity-api:3.4.0-SNAPSHOT")
}

tasks {
  publish {
    dependsOn(shadowJar)
  }
  assemble {
    dependsOn(shadowJar)
  }
  compileJava {
    options.encoding = Charsets.UTF_8.name()
    options.release.set(21)
  }
  javadoc {
    options.encoding = Charsets.UTF_8.name() // We want UTF-8 for everything
  }
  processResources {
    filteringCharset = Charsets.UTF_8.name() // We want UTF-8 for everything
    val props = Pair("version", version)
    filesMatching("plugin.yml"){
      expand(props)
    }
    filesMatching("bungee.yml"){
      expand(props)
    }
    filesMatching("velocity-plugin.json"){
      expand(props)
    }
  }

  shadowJar {
    configurations = listOf(project.configurations.runtimeClasspath.get())
    exclude("META-INF/*","release-timestamp.txt","README.md","LICENSE","latestchanges.html","changelog.txt","AUTHORS", "Class50/*")
    archiveFileName = "${project.name}.jar"
    archiveClassifier = ""
  }
}
tasks.create<Delete>("deleteUnused"){
  delete("build/libs/${project.name}-${project.version}-dev.jar", "build/libs/${project.name}-${project.version}-dev-all.jar", "build/libs/${project.name}-${project.version}.jar")
}
