# Building from Source

## Prerequisites

- JDK 17+
- Maven 3.9+

## Build

```bash
mvn clean package
```

## Run Demo

```bash
mvn exec:java -Dexec.mainClass="io.github.andrestubbe.fastio.Demo"
```

## Run Benchmark

```bash
mvn exec:java -Dexec.mainClass="io.github.andrestubbe.fastio.Benchmark"
```

## Installation

### JitPack (Recommended)

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.github.andrestubbe</groupId>
        <artifactId>fastio</artifactId>
        <version>v1.0.0</version>
    </dependency>
</dependencies>
```

### Gradle (JitPack)

```groovy
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.andrestubbe:fastio:v1.0.0'
}
```

## Download Pre-built JAR

See [Releases Page](https://github.com/andrestubbe/FastIO/releases)
