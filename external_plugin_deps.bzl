load("//tools/bzl:maven_jar.bzl", "maven_jar")

GOOGLE_GENAI_VER = "1.44.0"
JACKSON_VER = "2.17.2"
KOTLIN_VER = "1.9.10"
OKIO_VER = "3.6.0"

def external_plugin_deps():
    maven_jar(
        name = "google-gemini",
        artifact = "com.google.genai:google-genai:" + GOOGLE_GENAI_VER,
        sha1 = "d700033330a3e0662a2dab867952b0c974fb73d4",
    )

    maven_jar(
        name = "google-api-common",
        artifact = "com.google.api:api-common:2.45.0",
        sha1 = "de482ba30f33a918c5782e24de38438aaba4fda1",
    )

    maven_jar(
        name = "jackson-core",
        artifact = "com.fasterxml.jackson.core:jackson-core:" + JACKSON_VER,
        sha1 = "969a35cb35c86512acbadcdbbbfb044c877db814",
    )

    maven_jar(
        name = "jackson-databind",
        artifact = "com.fasterxml.jackson.core:jackson-databind:" + JACKSON_VER,
        sha1 = "e6deb029e5901e027c129341fac39e515066b68c",
    )

    maven_jar(
        name = "jackson-datatype-jdk8",
        artifact = "com.fasterxml.jackson.datatype:jackson-datatype-jdk8:" + JACKSON_VER,
        sha1 = "efd3dd0e1d0db8bc72abbe71c15e697bb83b4b45",
    )

    maven_jar(
        name = "jackson-datatype-jsr310",
        artifact = "com.fasterxml.jackson.datatype:jackson-datatype-jsr310:" + JACKSON_VER,
        sha1 = "267b85e9ba2892a37be6d80aa9ca1438a0d8c210",
    )

    maven_jar(
        name = "jackson-annotations",
        artifact = "com.fasterxml.jackson.core:jackson-annotations:" + JACKSON_VER,
        sha1 = "147b7b9412ffff24339f8aba080b292448e08698",
    )

    maven_jar(
        name = "okhttp",
        artifact = "com.squareup.okhttp3:okhttp:4.12.0",
        sha1 = "2f4525d4a200e97e1b87449c2cd9bd2e25b7e8cd",
    )

    maven_jar(
        name = "okio",
        artifact = "com.squareup.okio:okio:" + OKIO_VER,
        sha1 = "8bf9683c80762d7dd47db12b68e99abea2a7ae05",
    )

    maven_jar(
        name = "okio-jvm",
        artifact = "com.squareup.okio:okio-jvm:" + OKIO_VER,
        sha1 = "5600569133b7bdefe1daf9ec7f4abeb6d13e1786",
    )

    maven_jar(
        name = "kotlin-stdlib-jdk8",
        artifact = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:" + KOTLIN_VER,
        sha1 = "c7510d64a83411a649c76f2778304ddf71d7437b",
    )

    maven_jar(
        name = "kotlin-stdlib",
        artifact = "org.jetbrains.kotlin:kotlin-stdlib:" + KOTLIN_VER,
        sha1 = "72812e8a368917ab5c0a5081b56915ffdfec93b7",
    )
