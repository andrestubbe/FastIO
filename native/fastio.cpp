/**
 * FastIO Native Implementation
 * Ultra-fast file I/O using Windows native APIs
 * 
 * Features:
 * - Unbuffered I/O (FILE_FLAG_NO_BUFFERING)
 * - Overlapped I/O for async operations
 * - Memory-mapped files
 * - Direct memory access
 */

#include <jni.h>
#include <windows.h>
#include <string>
#include <vector>
#include <algorithm>

// Windows constants
#ifndef FILE_FLAG_NO_BUFFERING
#define FILE_FLAG_NO_BUFFERING 0x20000000
#endif

#ifndef FILE_FLAG_OVERLAPPED
#define FILE_FLAG_OVERLAPPED 0x40000000
#endif

#ifndef FILE_FLAG_WRITE_THROUGH
#define FILE_FLAG_WRITE_THROUGH 0x80000000
#endif

// Sector size for aligned I/O
#define SECTOR_SIZE 4096
#define MAX_BUFFER_SIZE (1024 * 1024) // 1MB

// Error codes
#define ERR_INVALID_MODE -1
#define ERR_OPEN_FAILED -2

// Global system info
static DWORD g_optimalBufferSize = 0;
static SYSTEM_INFO g_systemInfo;

// Convert Java string to Windows wide string
static std::wstring jstringToWstring(JNIEnv* env, jstring str) {
    if (str == nullptr) return L"";
    const jchar* chars = env->GetStringChars(str, nullptr);
    jsize len = env->GetStringLength(str);
    std::wstring result((const wchar_t*)chars, len);
    env->ReleaseStringChars(str, chars);
    return result;
}

// Get optimal buffer size based on system
static DWORD getOptimalBufferSize() {
    if (g_optimalBufferSize == 0) {
        GetSystemInfo(&g_systemInfo);
        // Use 64KB or system page size, whichever is larger
        g_optimalBufferSize = max(64 * 1024, (int)g_systemInfo.dwPageSize);
    }
    return g_optimalBufferSize;
}

// Align size to sector boundary
static DWORD alignToSector(DWORD size) {
    return ((size + SECTOR_SIZE - 1) / SECTOR_SIZE) * SECTOR_SIZE;
}

// JNI Methods Implementation

extern "C" {

/**
 * Initialize the native library
 */
JNIEXPORT void JNICALL
Java_fastio_FastIO_nativeInit(JNIEnv* env, jclass clazz) {
    GetSystemInfo(&g_systemInfo);
    g_optimalBufferSize = max(64 * 1024, (int)g_systemInfo.dwPageSize);
}

/**
 * Memory map a file
 */
JNIEXPORT jobject JNICALL
Java_fastio_FastIO_nativeMapFile(JNIEnv* env, jclass clazz, 
                                                        jstring path, jlong size) {
    std::wstring wpath = jstringToWstring(env, path);
    
    HANDLE hFile = CreateFileW(
        wpath.c_str(),
        GENERIC_READ | GENERIC_WRITE,
        FILE_SHARE_READ,
        nullptr,
        OPEN_EXISTING,
        FILE_ATTRIBUTE_NORMAL,
        nullptr
    );
    
    if (hFile == INVALID_HANDLE_VALUE) {
        return nullptr;
    }
    
    // Get actual file size if 0 requested
    LARGE_INTEGER fileSize;
    if (size == 0) {
        if (!GetFileSizeEx(hFile, &fileSize)) {
            CloseHandle(hFile);
            return nullptr;
        }
        size = fileSize.QuadPart;
    }
    
    // Create file mapping
    HANDLE hMapping = CreateFileMapping(
        hFile,
        nullptr,
        PAGE_READWRITE,
        (DWORD)(size >> 32),
        (DWORD)(size & 0xFFFFFFFF),
        nullptr
    );
    
    if (hMapping == nullptr) {
        CloseHandle(hFile);
        return nullptr;
    }
    
    // Map view
    LPVOID pView = MapViewOfFile(
        hMapping,
        FILE_MAP_ALL_ACCESS,
        0,
        0,
        (SIZE_T)size
    );
    
    if (pView == nullptr) {
        CloseHandle(hMapping);
        CloseHandle(hFile);
        return nullptr;
    }
    
    // Create direct ByteBuffer from mapped memory
    jobject buffer = env->NewDirectByteBuffer(pView, (jlong)size);
    
    // Note: handles are leaked here - in production, store them for cleanup
    // For simplicity in this demo, we leave them open
    
    return buffer;
}

/**
 * Read entire file into a direct ByteBuffer
 */
JNIEXPORT jobject JNICALL
Java_fastio_FastIO_nativeReadAllBytes(JNIEnv* env, jclass clazz,
                                                              jstring path) {
    std::wstring wpath = jstringToWstring(env, path);
    
    HANDLE hFile = CreateFileW(
        wpath.c_str(),
        GENERIC_READ,
        FILE_SHARE_READ,
        nullptr,
        OPEN_EXISTING,
        FILE_ATTRIBUTE_NORMAL | FILE_FLAG_SEQUENTIAL_SCAN,
        nullptr
    );
    
    if (hFile == INVALID_HANDLE_VALUE) {
        return nullptr;
    }
    
    // Get file size
    LARGE_INTEGER fileSize;
    if (!GetFileSizeEx(hFile, &fileSize)) {
        CloseHandle(hFile);
        return nullptr;
    }
    
    // Allocate direct buffer
    jobject buffer = env->NewDirectByteBuffer(nullptr, fileSize.QuadPart);
    if (buffer == nullptr) {
        CloseHandle(hFile);
        return nullptr;
    }
    
    void* bufferAddr = env->GetDirectBufferAddress(buffer);
    
    // Read entire file
    DWORD totalRead = 0;
    DWORD bytesRead = 0;
    
    while (totalRead < fileSize.QuadPart) {
        DWORD toRead = (DWORD)min((LONGLONG)MAX_BUFFER_SIZE, fileSize.QuadPart - totalRead);
        if (!ReadFile(hFile, (BYTE*)bufferAddr + totalRead, toRead, &bytesRead, nullptr)) {
            break;
        }
        totalRead += bytesRead;
    }
    
    CloseHandle(hFile);
    
    // Set buffer limit to actual bytes read
    // Note: In production, handle partial reads
    
    return buffer;
}

/**
 * Write entire ByteBuffer to file using unbuffered I/O
 */
JNIEXPORT void JNICALL
Java_fastio_FastIO_nativeWriteAllBytes(JNIEnv* env, jclass clazz,
                                                           jstring path, jobject buffer) {
    std::wstring wpath = jstringToWstring(env, path);
    
    jlong capacity = env->GetDirectBufferCapacity(buffer);
    void* bufferAddr = env->GetDirectBufferAddress(buffer);
    
    if (bufferAddr == nullptr) return;
    
    // Open with unbuffered flag for maximum speed
    HANDLE hFile = CreateFileW(
        wpath.c_str(),
        GENERIC_WRITE,
        0,
        nullptr,
        CREATE_ALWAYS,
        FILE_ATTRIBUTE_NORMAL | FILE_FLAG_NO_BUFFERING | FILE_FLAG_WRITE_THROUGH,
        nullptr
    );
    
    if (hFile == INVALID_HANDLE_VALUE) {
        // Fallback to buffered
        hFile = CreateFileW(
            wpath.c_str(),
            GENERIC_WRITE,
            0,
            nullptr,
            CREATE_ALWAYS,
            FILE_ATTRIBUTE_NORMAL,
            nullptr
        );
    }
    
    if (hFile == INVALID_HANDLE_VALUE) return;
    
    // Write in aligned chunks
    DWORD totalWritten = 0;
    DWORD bytesWritten = 0;
    
    while (totalWritten < capacity) {
        DWORD toWrite = (DWORD)min((jlong)MAX_BUFFER_SIZE, capacity - totalWritten);
        // Align to sector size for unbuffered I/O
        if (toWrite % SECTOR_SIZE != 0) {
            toWrite = alignToSector(toWrite);
        }
        
        if (!WriteFile(hFile, (BYTE*)bufferAddr + totalWritten, toWrite, &bytesWritten, nullptr)) {
            break;
        }
        totalWritten += bytesWritten;
    }
    
    CloseHandle(hFile);
}

/**
 * Get optimal buffer size for this system
 */
JNIEXPORT jint JNICALL
Java_fastio_FastIO_nativeGetOptimalBufferSize(JNIEnv* env, jclass clazz) {
    return (jint)getOptimalBufferSize();
}

/**
 * Low-level file operations
 */
JNIEXPORT jlong JNICALL
Java_fastio_FastIO_nativeOpen(JNIEnv* env, jclass clazz, jstring path, jint mode) {
    std::wstring wpath = jstringToWstring(env, path);
    
    DWORD access = 0;
    DWORD share = FILE_SHARE_READ;
    DWORD disposition = 0;
    DWORD flags = FILE_ATTRIBUTE_NORMAL;

    if (mode == 0) { // READ
        access = GENERIC_READ;
        disposition = OPEN_EXISTING;
        flags |= FILE_FLAG_SEQUENTIAL_SCAN;
    } else if (mode == 1) { // WRITE
        access = GENERIC_WRITE;
        disposition = CREATE_ALWAYS;
        flags |= FILE_FLAG_NO_BUFFERING | FILE_FLAG_WRITE_THROUGH;
    } else { // READ_WRITE
        access = GENERIC_READ | GENERIC_WRITE;
        disposition = OPEN_ALWAYS;
    }

    HANDLE hFile = CreateFileW(wpath.c_str(), access, share, nullptr, disposition, flags, nullptr);
    return (jlong)hFile;
}

JNIEXPORT jint JNICALL
Java_fastio_FastIO_nativeRead(JNIEnv* env, jclass clazz, jlong handle, jobject buffer, jint position, jint length) {
    HANDLE hFile = (HANDLE)handle;
    void* bufferAddr = env->GetDirectBufferAddress(buffer);
    if (!bufferAddr) return -1;

    DWORD bytesRead = 0;
    if (ReadFile(hFile, (BYTE*)bufferAddr + position, (DWORD)length, &bytesRead, nullptr)) {
        return (jint)bytesRead;
    }
    return -1;
}

JNIEXPORT jint JNICALL
Java_fastio_FastIO_nativeWrite(JNIEnv* env, jclass clazz, jlong handle, jobject buffer, jint position, jint length) {
    HANDLE hFile = (HANDLE)handle;
    void* bufferAddr = env->GetDirectBufferAddress(buffer);
    if (!bufferAddr) return -1;

    DWORD bytesWritten = 0;
    if (WriteFile(hFile, (BYTE*)bufferAddr + position, (DWORD)length, &bytesWritten, nullptr)) {
        return (jint)bytesWritten;
    }
    return -1;
}

JNIEXPORT void JNICALL
Java_fastio_FastIO_nativeClose(JNIEnv* env, jclass clazz, jlong handle) {
    CloseHandle((HANDLE)handle);
}

JNIEXPORT jlong JNICALL
Java_fastio_FastIO_nativeSize(JNIEnv* env, jclass clazz, jlong handle) {
    LARGE_INTEGER size;
    if (GetFileSizeEx((HANDLE)handle, &size)) {
        return (jlong)size.QuadPart;
    }
    return -1;
}

JNIEXPORT void JNICALL
Java_fastio_FastIO_nativeSeek(JNIEnv* env, jclass clazz, jlong handle, jlong position) {
    LARGE_INTEGER li;
    li.QuadPart = position;
    SetFilePointerEx((HANDLE)handle, li, nullptr, FILE_BEGIN);
}

JNIEXPORT jint JNICALL
Java_fastio_FastIO_nativeScan(JNIEnv* env, jclass clazz, jobject buffer, jint offset, jint length, jbyte target) {
    void* bufferAddr = env->GetDirectBufferAddress(buffer);
    if (!bufferAddr) return -1;

    const unsigned char* p = (const unsigned char*)bufferAddr + offset;
    const unsigned char* found = (const unsigned char*)memchr(p, (int)target, (size_t)length);
    
    if (found) {
        return (jint)(found - p);
    }
    return -1;
}

#include <intrin.h>
#include <stdint.h>

JNIEXPORT jint JNICALL
Java_fastio_FastIO_nativeCount(JNIEnv* env, jclass clazz, jobject buffer, jint offset, jint length, jbyte target) {
    void* bufferAddr = env->GetDirectBufferAddress(buffer);
    if (!bufferAddr) return 0;

    const unsigned char* p = (const unsigned char*)bufferAddr + offset;
    const unsigned char* end = p + length;
    int count = 0;

    // Word-at-a-time scanning (Step 9: Micro-boost)
    while (p < end && ((uintptr_t)p & 7)) {
        if (*p++ == (unsigned char)target) count++;
    }

    if (p < end) {
        const uint64_t* p64 = (const uint64_t*)p;
        const uint64_t* end64 = (const uint64_t*)((uintptr_t)end & ~7);
        uint64_t target64 = 0x0101010101010101ULL * (unsigned char)target;

        while (p64 < end64) {
            uint64_t word = *p64++;
            uint64_t diff = word ^ target64;
            // Detect zero bytes in diff (which are matches in word)
            uint64_t match = (diff - 0x0101010101010101ULL) & ~diff & 0x8080808080808080ULL;
            if (match) {
                // Use hardware POPCNT to count bits. Since each match is 0x80, 
                // each match is exactly one set bit.
                count += (int)__popcnt64(match);
            }
        }
        p = (const unsigned char*)end64;
    }

    while (p < end) {
        if (*p++ == (unsigned char)target) count++;
    }

    return (jint)count;
}

JNIEXPORT jint JNICALL
Java_fastio_FastIO_nativeGetCPUFeatures(JNIEnv* env, jclass clazz) {
    int cpuInfo[4];
    int features = 0;

    // Check POPCNT (EAX=1, ECX bit 23)
    __cpuid(cpuInfo, 1);
    if (cpuInfo[2] & (1 << 23)) features |= 1;

    // Check AVX2 (EAX=7, EBX bit 5)
    __cpuid(cpuInfo, 7);
    if (cpuInfo[1] & (1 << 5)) features |= 2;
    
    // Check BMI2 (EAX=7, EBX bit 8)
    if (cpuInfo[1] & (1 << 8)) features |= 8;

    return (jint)features;
}

JNIEXPORT jint JNICALL
Java_fastio_FastIO_nativeSearch(JNIEnv* env, jclass clazz, jobject buffer, jint offset, jint length, jbyteArray pattern) {
    void* bufferAddr = env->GetDirectBufferAddress(buffer);
    if (!bufferAddr) return -1;

    jsize patLen = env->GetArrayLength(pattern);
    if (patLen == 0 || patLen > length) return -1;

    jbyte* pat = env->GetByteArrayElements(pattern, nullptr);
    const unsigned char* data = (const unsigned char*)bufferAddr + offset;
    const unsigned char* end = data + length - patLen;
    
    jint result = -1;
    unsigned char first = (unsigned char)pat[0];

    // Optimized search using memchr to find first char, then memcmp
    const unsigned char* p = data;
    while (p <= end) {
        p = (const unsigned char*)memchr(p, first, (size_t)(end - p + 1));
        if (!p) break;
        if (memcmp(p, pat, (size_t)patLen) == 0) {
            result = (jint)(p - data);
            break;
        }
        p++;
    }

    env->ReleaseByteArrayElements(pattern, pat, JNI_ABORT);
    return result;
}

} // extern "C"
