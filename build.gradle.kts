/*
 * Copyright 2021 dorkbox, llc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.time.Instant

///////////////////////////////
//////    PUBLISH TO SONATYPE / MAVEN CENTRAL
////// TESTING : (to local maven repo) <'publish and release' - 'publishToMavenLocal'>
////// RELEASE : (to sonatype/maven central), <'publish and release' - 'publishToSonatypeAndRelease'>
///////////////////////////////

gradle.startParameter.showStacktrace = ShowStacktrace.ALWAYS   // always show the stacktrace!

plugins {
    id("com.dorkbox.GradleUtils") version "2.9"
    id("com.dorkbox.Licensing") version "2.9.2"
    id("com.dorkbox.VersionUpdate") version "2.4"
    id("com.dorkbox.GradlePublish") version "1.11"

    kotlin("jvm") version "1.5.21"
}

object Extras {
    // set for the project
    const val description = "Storage system for Java"
    const val group = "com.dorkbox"
    const val version = "1.0"

    // set as project.ext
    const val name = "Storage"
    const val id = "Storage"
    const val vendor = "Dorkbox LLC"
    const val vendorUrl = "https://dorkbox.com"
    const val url = "https://git.dorkbox.com/dorkbox/Storage"

    val buildDate = Instant.now().toString()
}

/////////////////////////////
///  assign 'Extras'
/////////////////////////////
GradleUtils.load("$projectDir/../../gradle.properties", Extras)
GradleUtils.defaults()
GradleUtils.compileConfiguration(JavaVersion.VERSION_1_8)
GradleUtils.jpms(JavaVersion.VERSION_1_9)


licensing {
    license(License.APACHE_2) {
        description(Extras.description)
        author(Extras.vendor)
        url(Extras.url)
    }
}

tasks.jar.get().apply {
    manifest {
        // https://docs.oracle.com/javase/tutorial/deployment/jar/packageman.html
        attributes["Name"] = Extras.name

        attributes["Specification-Title"] = Extras.name
        attributes["Specification-Version"] = Extras.version
        attributes["Specification-Vendor"] = Extras.vendor

        attributes["Implementation-Title"] = "${Extras.group}.${Extras.id}"
        attributes["Implementation-Version"] = Extras.buildDate
        attributes["Implementation-Vendor"] = Extras.vendor

        attributes["Automatic-Module-Name"] = Extras.id
    }
}

dependencies {
//    // really fast storage
//    // https://github.com/lmdbjava/lmdbjava
//    compileOnly("org.lmdbjava:lmdbjava:0.8.1")
//
//    // https://github.com/OpenHFT/Chronicle-Map
//    compileOnly("net.openhft:chronicle-map:3.20.40")


    // https://github.com/MicroUtils/kotlin-logging
    implementation("io.github.microutils:kotlin-logging:2.0.10")
    implementation("org.slf4j:slf4j-api:1.8.0-beta4")


    implementation("com.dorkbox:ByteUtilities:1.3")
    implementation("com.dorkbox:Serializers:2.5")
    implementation("com.dorkbox:ObjectPool:3.4")
    implementation("com.dorkbox:Updates:1.1")

    implementation("com.esotericsoftware:kryo:5.2.0")


//    // really fast storage
//    // https://github.com/lmdbjava/lmdbjava
//    testImplementation("org.lmdbjava:lmdbjava:0.8.1")
//
//    // https://github.com/OpenHFT/Chronicle-Map
//    testImplementation("net.openhft:chronicle-map:3.20.40")

    testImplementation("junit:junit:4.13.1")
    testImplementation("ch.qos.logback:logback-classic:1.3.0-alpha4")
}

publishToSonatype {
    groupId = Extras.group
    artifactId = Extras.id
    version = Extras.version

    name = Extras.name
    description = Extras.description
    url = Extras.url

    vendor = Extras.vendor
    vendorUrl = Extras.vendorUrl

    issueManagement {
        url = "${Extras.url}/issues"
        nickname = "Gitea Issues"
    }

    developer {
        id = "dorkbox"
        name = Extras.vendor
        email = "email@dorkbox.com"
    }
}
