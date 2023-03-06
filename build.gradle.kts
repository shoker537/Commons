import java.net.URI
import java.util.Properties

plugins {
  `java-library`
  `maven-publish`
  id("io.papermc.paperweight.userdev") version "1.3.8"
  id("com.github.johnrengelman.shadow") version "7.1.0"
}

group = "ru.shk"
version = "1.3.88"

val nexusRepository = Properties()
nexusRepository.load(file("nexus.properties").inputStream())
publishing {
  repositories {
    maven {
      url = URI.create("https://nexus.shoker.su/repository/maven-releases/")
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
  maven {
    url = URI.create("https://nexus.shoker.su/repository/maven-releases/")
  }
  maven {
    url = URI.create("https://maven.enginehub.org/repo/")
  }
  maven {
    url = URI.create("https://papermc.io/repo/repository/maven-public/")
  }
  maven {
    url = URI.create("https://oss.sonatype.org/content/groups/public/")
  }
  maven {
    url = URI.create("https://libraries.minecraft.net/")
  }
  maven {
    url = URI.create("https://repo.codemc.io/repository/maven-snapshots/")
  }
  maven {
    url = URI.create("https://mvn.exceptionflug.de/repository/exceptionflug-public/")
  }
  maven {
    url = URI.create("https://maven.enginehub.org/repo/")
  }
  maven {
    url = URI.create("https://repo.dmulloy2.net/repository/public/")
  }
  maven {
    url = URI.create("https://nexus.shoker.su/repository/maven-shield/")
    credentials {
      username = "${nexusRepository["user"]}"
      password = "${nexusRepository["password"]}"
    }
  }
  maven {
    url = URI.create("https://simonsator.de/repo")
  }
}

dependencies {
  paperDevBundle("1.19.2-R0.1-20220926.081544-62")
  implementation("org.apache.commons:commons-lang3:3.12.0")
  compileOnly("com.sk89q.worldedit:worldedit-bukkit:7.2.9")
  compileOnly("com.sk89q.worldedit:worldedit-core:7.2.0-SNAPSHOT")
  implementation("commons-io:commons-io:2.11.0")
  compileOnly("dev.simplix:protocolize-api:2.2.2")
  compileOnly("net.md-5:bungeecord-api:1.18-R0.1-SNAPSHOT")
  implementation("net.wesjd:anvilgui:1.5.3-SNAPSHOT")
  compileOnly("org.apache.logging.log4j:log4j-core:2.17.1")
  compileOnly("com.mojang:authlib:1.5.21")
  compileOnly("de.simonsator:BungeecordPartyAndFriends:1.0.86")

  implementation("org.projectlombok:lombok:1.18.22")
  annotationProcessor("org.projectlombok:lombok:1.18.22")

  compileOnly("land.shield:PlayerAPI:1.5.1")
  compileOnly("ru.shk:MySQLAPI:2.2.1")
  compileOnly("com.comphenix.protocol:ProtocolLib:4.8.0")
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
    val props = kotlin.Pair<String, Any>("version", version)
    filesMatching("*.yml"){
      expand(props)
    }
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
