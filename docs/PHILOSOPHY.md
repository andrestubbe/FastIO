# FastIO Design Philosophy

`FastIO` is designed around three zero-overhead storage principles:

1. **Unbuffered Direct I/O**: Direct disk sector access bypassing OS cache layers (`FILE_FLAG_NO_BUFFERING`) for predictable sub-millisecond latencies.
2. **Off-Heap Direct Memory Allocation**: Directly maps disk contents into off-heap direct byte buffers to completely eliminate JVM Garbage Collection pauses.
3. **Hardware SIMD Tokenization**: Uses 256-bit AVX2 SIMD registers to scan lines and CSV delimiters at multi-gigabyte per second speeds.
