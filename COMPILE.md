# Building FastIO from Source

## Prerequisites

- **JDK 17+** — [Download](https://adoptium.net/)
- **Maven 3.9+** — [Download](https://maven.apache.org/download.cgi)
- **Visual Studio 2022** — Community/Professional/Enterprise/BuildTools

## Quick Build

```bash
# 1. Build native DLL first (Windows)
compile.bat

# 2. Build JAR
mvn clean package -DskipTests
```

## Build Commands

| Command | Purpose |
|---------|---------|
| `compile.bat` | Build native DLL (Windows) |
| `mvn clean compile` | Compile Java only |
| `mvn clean package` | Build JAR with DLL embedded |
| `mvn test` | Run unit tests |

## Native DLL Build

The `compile.bat` script:
- Auto-detects Visual Studio 2019/2022
- Auto-detects JAVA_HOME
- Compiles `src/main/native/fastio.cpp`
- Outputs to `build/fastio.dll`

The Maven `pom.xml` automatically picks up `build/fastio.dll` and bundles it inside the JAR.

## Native Source Location

Unlike the _BluePrint default, FastIO stores its native C++ source in the Maven-standard path:

```
src/main/native/fastio.cpp   ← C++ JNI implementation
src/main/native/build.bat    ← Local build script
build/fastio.dll             ← Compiled output (bundled in JAR)
```

## Troubleshooting

**"Cannot find DLL"** — Run `compile.bat` first

**"UnsatisfiedLinkError"** — Common causes:
1. DLL built but not included in JAR (check `build/` folder).
2. Wrong function name — Must match `Java_package_Class_method` exactly.
3. FastCore not on classpath — required for DLL extraction.

**"Java version mismatch"** — Ensure JDK 17+ is installed and JAVA_HOME is set.
