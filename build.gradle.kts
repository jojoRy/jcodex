plugins {
    id("java")
    id("com.gradleup.shadow") version "8.3.5"
}

group = "kr.jjory"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://jitpack.io")
    maven("https://repo.codemc.org/repository/maven-public/")
    maven("https://nexus.phoenixdevt.fr/repository/maven-public/")
    maven("https://maven.devs.beer/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7")
    compileOnly("io.lumine:MythicLib-dist:1.7.1-SNAPSHOT")
    compileOnly("net.Indyuce:MMOCore-API:1.13.1-SNAPSHOT")
    compileOnly("net.Indyuce:MMOItems-API:6.10.1-SNAPSHOT")
    compileOnly("dev.lone:api-itemsadder:4.0.10")

    // HikariCP for database connection pooling
    implementation("com.zaxxer:HikariCP:5.0.1")
    // Jedis for Redis connection
    implementation("io.lettuce:lettuce-core:6.5.5.RELEASE")
    // Gson for JSON serialization
    implementation("com.google.code.gson:gson:2.9.0")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.processResources {
    filesMatching("plugin.yml") {
        expand(project.properties)
    }
}

tasks.shadowJar {
    archiveClassifier.set("")
    relocate("com.zaxxer.hikari", "kr.jjory.jcodex.lib.hikaricp")
    relocate("io.lettuce.core", "kr.jjory.jcodex.lib.lettuce")
    relocate("io.netty", "kr.jjory.jcodex.lib.netty") // Lettuce dependency
    relocate("reactor.core", "kr.jjory.jcodex.lib.reactor") // Lettuce dependency
    relocate("com.google.gson", "kr.jjory.jcodex.lib.gson")
}
