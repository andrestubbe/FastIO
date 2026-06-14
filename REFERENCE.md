# FastIO Reference

## 1. Core Classes

### `FastIO` — Static Utility
| Method | Description |
|--------|-------------|
| `FastIO.init()` | Initialize native library via FastCore |
| `FastIO.openRead(path)` | Open file handle for unbuffered reading |
| `FastIO.openWrite(path)` | Open file handle for unbuffered writing |
| `FastIO.mapFile(path, size)` | Memory-map a file (0 = entire file) |
| `FastIO.readAllBytes(path)` | Read entire file into DirectByteBuffer |
| `FastIO.fastCopy(src, dst)` | Native file copy (CopyFileEx) |

### `FastFile` — High-Performance File Handle
| Method | Description |
|--------|-------------|
| `read(ByteBuffer)` | Unbuffered read into direct buffer |
| `write(ByteBuffer)` | Unbuffered write from direct buffer |
| `seek(position)` | Random access position |
| `size()` | Get file size in bytes |
| `sync()` | Force writes to disk (FlushFileBuffers) |
| `close()` | Release native file handle |

### `FastCSVReader` — Optimized CSV Parser
| Method | Description |
|--------|-------------|
| `nextRow()` | Advance to next row |
| `getString(col)` | Get column as String |
| `getInt(col)` | Get column as int |
| `getDouble(col)` | Get column as double |
| `getColumnCount()` | Number of columns in current row |

### `FastJSONReader` — Fast JSON Loader
| Method | Description |
|--------|-------------|
| `readObject()` | Parse JSON object |
| `readArray()` | Parse JSON array |
| `get(path)` | Navigate with dot notation |

### `FastTextReader` — Fast Text Scanner
| Method | Description |
|--------|-------------|
| `readLine()` | Read next line |
| `setBufferSize(size)` | Tune buffer (default 64KB) |
| `setEncoding(enc)` | Auto-detect or specify encoding |

## 2. I/O Flags & Guarantees

* **`FILE_FLAG_NO_BUFFERING`** — Bypasses OS page cache. Requires sector-aligned reads (512 or 4096 bytes).
* **`FILE_FLAG_OVERLAPPED`** — Enables async operations without thread blocking.
* **`FILE_FLAG_WRITE_THROUGH`** — Forces writes through cache to disk immediately.
* **Zero-Copy**: All paths use `DirectByteBuffer` — no Java heap allocation.
* **Thread-Safety**: All static native methods are thread-safe.

## 3. Platform Support

| Platform | Status |
|----------|--------|
| Windows 10/11 (x64) | ✅ Fully Supported |
| Linux | 🔗 Planned |
| macOS | 🔗 Planned |

---
**Part of the FastJava Ecosystem** — *Making the JVM faster.*

Made with ⚡ by Andre Stubbe
