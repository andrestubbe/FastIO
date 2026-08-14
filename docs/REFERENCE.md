# FastIO API Reference Manual

`FastIO` provides high-performance native unbuffered file I/O, memory-mapped files, and SIMD-accelerated text, CSV, and JSON parsing for Java applications.

---

## 1. Core FastIO Utility API

### `init`
```java
public static synchronized void init()
```
Initializes the native Windows `fastio.dll` library. Falls back to pure Java implementation if native bindings are unavailable.

---

### `openRead`
```java
public static FastFile openRead(String path) throws IOException
```
Opens a file handle for high-speed unbuffered native reading.

#### Parameters:
- **`path`** (`String`): Absolute or relative file path to open.

#### Returns:
- **`FastFile`**: Active unbuffered file handle instance.

---

### `openWrite`
```java
public static FastFile openWrite(String path) throws IOException
```
Opens a file handle for high-speed unbuffered native writing.

---

### `mapFile`
```java
public static ByteBuffer mapFile(String path, long size) throws IOException
```
Memory-maps a file directly into off-heap direct memory for zero-copy access.

---

## 2. FastFile Class Operations

### `read`
```java
public int read(ByteBuffer buffer) throws IOException
```
Reads unbuffered data directly into the specified off-heap direct `ByteBuffer`.

---

### `write`
```java
public int write(ByteBuffer buffer) throws IOException
```
Writes data directly from the `ByteBuffer` to disk.

---

### `seek`
```java
public void seek(long position) throws IOException
```
Seeks the file pointer to the specified byte position.

---

### `size`
```java
public long size() throws IOException
```
Returns the total size of the file in bytes.

---

## 3. Specialized Stream Parsers

### `FastCSVReader`
```java
public class FastCSVReader implements AutoCloseable
```
SIMD-accelerated zero-allocation CSV stream parser.

- `nextRow()` (`boolean`): Advances to the next CSV row.
- `getString(int col)` (`String`): Returns the cell string value at specified column index.
- `getInt(int col)` (`int`): Parses cell value as integer.
- `getDouble(int col)` (`double`): Parses cell value as double precision float.
- `getColumnCount()` (`int`): Returns the total number of columns in the current row.

---

### `FastJSONReader`
```java
public class FastJSONReader implements AutoCloseable
```
High-speed JSON loader with lazy object parsing.

- `readObject()` (`JsonObject`): Parses the JSON stream as an object.
- `readArray()` (`JsonArray`): Parses the JSON stream as an array.
- `get(String path)` (`Object`): Navigates JSON structure using dot notation paths.

---

### `FastTextReader`
```java
public class FastTextReader implements AutoCloseable
```
Ultra-fast line-by-line text scanner using AVX2 SIMD boundary detection.

- `readLine()` (`String`): Reads the next line from the stream.
- `setBufferSize(int size)` (`void`): Sets the internal read buffer size (e.g. `256 * 1024`).
- `setEncoding(String enc)` (`void`): Configures text encoding or enables auto-detection.
