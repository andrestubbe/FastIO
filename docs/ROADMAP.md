# FastIO Roadmap 🗺️

**Vision:** To provide the fastest possible native file I/O for Java by aggressively bypassing the JVM I/O stack and communicating directly with Windows storage APIs.

## 🟢 v0.1.0: Initial Release (Current)
- [x] **Core Native Engine**: JNI implementation with unbuffered I/O.
- [x] **Memory-Mapped Files**: Direct kernel-managed memory access.
- [x] **FastCSVReader**: Zero-allocation CSV parser.
- [x] **FastJSONReader**: Fast JSON loader with lazy parsing.
- [x] **FastTextReader**: High-speed line-by-line text scanning.
- [x] **Blueprint Standards**: README, Reference, Philosophy integration.

## 🟡 v0.2.0: Optimization Phase
- [ ] **Overlapped Async I/O**: True async read/write without blocking threads.
- [ ] **Scatter/Gather I/O**: `ReadFileScatter` / `WriteFileGather` for vectored operations.
- [ ] **Alignment Auto-Detection**: Auto-detect sector size for `FILE_FLAG_NO_BUFFERING`.

## 🟠 v0.5.0: Platform & Format Expansion
- [ ] **Linux io_uring Backend**: Native async I/O for Linux.
- [ ] **FastBinaryReader**: Optimized binary protocol parser.
- [ ] **Direct NVMe Access**: Bypass OS layer entirely for extreme throughput.

## 🔴 v1.0.0: Production Hardening
- [ ] **Full Stability Audit**: Long-run stress testing.
- [ ] **NUMA-Awareness**: Optimize memory placement for multi-socket systems.
- [ ] **Large Pages Support**: `MEM_LARGE_PAGES` for reduced TLB misses.

---
**Focus:** Performance is our USP. We optimize where Java stops.
