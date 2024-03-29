import java.util.*

plugins {
  `java-library`
  `maven-publish`
  id("io.papermc.paperweight.userdev") version "1.5.5"
  id("com.github.johnrengelman.shadow") version "7.1.0"
}

group = "ru.shk"
version = "1.5.5"

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

      artifact("/build/libs/${project.name}.jar")
    }
  }
}
java {
  toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}
repositories {
  mavenLocal()
  mavenCentral()
  maven { url = uri("https://repo.codemc.io/repository/maven-releases/") }
  maven {
    url = uri("https://nexus.shoker.su/repository/maven-releases/")
  }
  maven {
    url = uri("https://maven.enginehub.org/repo/")
  }
  maven {
    url = uri("https://repo.papermc.io/repository/maven-public/")
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
  maven { url = uri("https://jitpack.io") }
}


dependencies {
  paperDevBundle("1.20.1-R0.1-SNAPSHOT")
  compileOnly("com.github.Carleslc.Simple-YAML:Simple-Yaml:1.8.4")
  compileOnly("com.github.retrooper.packetevents:spigot:2.0.2")
  compileOnly(files("E:\\IdeaProjects\\commons-lang\\target\\commons-lang3-3.13.0-SNAPSHOT.jar"))
  compileOnly("com.sk89q.worldedit:worldedit-bukkit:7.2.9")
  compileOnly("com.sk89q.worldedit:worldedit-core:7.2.0-SNAPSHOT")
  implementation("commons-io:commons-io:2.11.0")
  compileOnly("dev.simplix:protocolize-api:2.3.3")
  compileOnly("io.github.waterfallmc:waterfall-api:1.20-R0.2-SNAPSHOT")
  implementation("net.wesjd:anvilgui:1.9.0-SNAPSHOT")
  compileOnly("org.apache.logging.log4j:log4j-core:2.17.1")
  implementation("org.apache.commons:commons-lang3:3.14.0")
  compileOnly("com.mojang:authlib:1.5.21")
  compileOnly("de.simonsator:BungeecordPartyAndFriends:1.0.86")

  implementation("org.projectlombok:lombok:1.18.22")
  annotationProcessor("org.projectlombok:lombok:1.18.22")

  compileOnly("land.shield:PlayerAPI:1.5.1")
  compileOnly("ru.shk:MySQLAPI:3.1.2")

  compileOnly(files("D:/Libraries/ProtocolLib.jar"))
  compileOnly("com.velocitypowered:velocity-api:3.0.1")
  annotationProcessor("com.velocitypowered:velocity-api:3.0.1")
}

tasks {
  assemble {
    dependsOn(reobfJar)
    dependsOn(shadowJar)
  }
  compileJava {
    options.encoding = Charsets.UTF_8.name()
    options.release.set(17)
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
    exclude("META-INF/*","release-timestamp.txt","README.md","LICENSE","latestchanges.html","changelog.txt","AUTHORS", "Class50/*")
  }

  reobfJar {
    // This is an example of how you might change the output location for reobfJar. It's recommended not to do this
    // for a variety of reasons, however it's asked frequently enough that an example of how to do it is included here.
    outputJar.set(layout.buildDirectory.file("libs/${project.name}.jar"))
  }
}
//task("deleteUnused") {
//  delete("build/libs/*-dev*.jar")
//}
tasks.create<Delete>("deleteUnused"){
  delete("build/libs/${project.name}-${project.version}-dev.jar", "build/libs/${project.name}-${project.version}-dev-all.jar")
}
