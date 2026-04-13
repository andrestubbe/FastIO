package io.github.andrestubbe.fastio;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;

/**
 * FastIO Performance Benchmark
 * 
 * Compares FastIO against Java NIO/FileChannel for:
 * - Sequential read/write
 * - Random access
 * - Small file operations
 * - CSV/JSON/text parsing
 * 
 * Run: mvn exec:java -Dexec.mainClass="io.github.andrestubbe.fastio.Benchmark"
 */
public class Benchmark {
    
    private static final int WARMUP_RUNS = 3;
    private static final int BENCHMARK_RUNS = 5;
    private static final long GB = 1024 * 1024 * 1024;
    private static final long MB = 1024 * 1024;
    
    private static final String TEST_DIR = "benchmark_test_files";
    private static final String LARGE_FILE = TEST_DIR + "/large_file.dat";
    private static final String CSV_FILE = TEST_DIR + "/test_data.csv";
    private static final String JSON_FILE = TEST_DIR + "/test_data.json";
    private static final String TEXT_FILE = TEST_DIR + "/test_data.txt";
    
    public static void main(String[] args) throws Exception {
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║           FastIO Performance Benchmark                       ║");
        System.out.println("║           FastIO vs Java NIO/FileChannel                     ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
        System.out.println();
        
        // Create test directory
        Files.createDirectories(Paths.get(TEST_DIR));
        
        // Initialize FastIO
        System.out.println("Initializing FastIO...");
        FastIO.init();
        System.out.println("✓ FastIO initialized");
        System.out.println();
        
        // Generate test files
        System.out.println("Generating test files...");
        generateTestFiles();
        System.out.println("✓ Test files ready");
        System.out.println();
        
        // Run benchmarks
        benchmarkSequentialRead();
        benchmarkSequentialWrite();
        benchmarkRandomRead();
        benchmarkSmallFileRead();
        benchmarkCSVRead();
        benchmarkTextRead();
        
        // Cleanup
        System.out.println("Cleaning up...");
        cleanup();
        
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║           Benchmark Complete!                                ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
    }
    
    // ═════════════════════════════════════════════════════════════════
    // SEQUENTIAL READ BENCHMARK
    // ═════════════════════════════════════════════════════════════════
    private static void benchmarkSequentialRead() throws Exception {
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│ SEQUENTIAL READ (1 GB file, 64KB chunks)                      │");
        System.out.println("└─────────────────────────────────────────────────────────────┘");
        
        // Warmup
        for (int i = 0; i < WARMUP_RUNS; i++) {
            javaNIOSRead(LARGE_FILE);
            fastIORead(LARGE_FILE);
        }
        
        // Benchmark Java NIO
        long[] nioTimes = new long[BENCHMARK_RUNS];
        for (int i = 0; i < BENCHMARK_RUNS; i++) {
            long start = System.nanoTime();
            long bytesRead = javaNIOSRead(LARGE_FILE);
            long end = System.nanoTime();
            nioTimes[i] = end - start;
            double mbps = (bytesRead / MB) / (nioTimes[i] / 1_000_000_000.0);
            System.out.printf("  [JavaNIO] Run %d: %.1f MB/s%n", i + 1, mbps);
        }
        double nioAvg = avg(nioTimes);
        double nioMBps = (1 * GB / MB) / (nioAvg / 1_000_000_000.0);
        
        // Benchmark FastIO
        long[] fastIOTimes = new long[BENCHMARK_RUNS];
        for (int i = 0; i < BENCHMARK_RUNS; i++) {
            long start = System.nanoTime();
            long bytesRead = fastIORead(LARGE_FILE);
            long end = System.nanoTime();
            fastIOTimes[i] = end - start;
            double mbps = (bytesRead / MB) / (fastIOTimes[i] / 1_000_000_000.0);
            System.out.printf("  [FastIO]  Run %d: %.1f MB/s%n", i + 1, mbps);
        }
        double fastIOAvg = avg(fastIOTimes);
        double fastIOMBps = (1 * GB / MB) / (fastIOAvg / 1_000_000_000.0);
        
        // Results
        double speedup = fastIOMBps / nioMBps;
        System.out.println();
        System.out.printf("  Java NIO Average: %.1f MB/s%n", nioMBps);
        System.out.printf("  FastIO Average:   %.1f MB/s%n", fastIOMBps);
        System.out.printf("  ⚡ SPEEDUP: %.2f×%n", speedup);
        System.out.println();
    }
    
    private static long javaNIOSRead(String path) throws Exception {
        long totalRead = 0;
        try (FileChannel channel = FileChannel.open(Paths.get(path), StandardOpenOption.READ)) {
            ByteBuffer buffer = ByteBuffer.allocateDirect(64 * 1024);
            while (channel.read(buffer) > 0) {
                totalRead += buffer.position();
                buffer.clear();
            }
        }
        return totalRead;
    }
    
    private static long fastIORead(String path) throws Exception {
        long totalRead = 0;
        FastFile file = FastIO.openRead(path);
        ByteBuffer buffer = FastFile.allocateAlignedBuffer(64 * 1024);
        int read;
        while ((read = file.read(buffer)) > 0) {
            totalRead += read;
            buffer.clear();
        }
        file.close();
        return totalRead;
    }
    
    // ═════════════════════════════════════════════════════════════════
    // SEQUENTIAL WRITE BENCHMARK
    // ═════════════════════════════════════════════════════════════════
    private static void benchmarkSequentialWrite() throws Exception {
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│ SEQUENTIAL WRITE (500 MB, 64KB chunks)                       │");
        System.out.println("└─────────────────────────────────────────────────────────────┘");
        
        String writeFile1 = TEST_DIR + "/write_test_nio.dat";
        String writeFile2 = TEST_DIR + "/write_test_fastio.dat";
        int sizeMB = 500;
        byte[] data = new byte[64 * 1024];
        
        // Warmup
        for (int i = 0; i < WARMUP_RUNS; i++) {
            javaNIOWrite(writeFile1 + "_w", data, sizeMB);
            fastIOWrite(writeFile2 + "_w", data, sizeMB);
        }
        
        // Benchmark Java NIO
        long[] nioTimes = new long[BENCHMARK_RUNS];
        for (int i = 0; i < BENCHMARK_RUNS; i++) {
            String f = writeFile1 + "_" + i;
            long start = System.nanoTime();
            javaNIOWrite(f, data, sizeMB);
            long end = System.nanoTime();
            nioTimes[i] = end - start;
            double mbps = sizeMB / (nioTimes[i] / 1_000_000_000.0);
            System.out.printf("  [JavaNIO] Run %d: %.1f MB/s%n", i + 1, mbps);
            Files.deleteIfExists(Paths.get(f));
        }
        double nioAvg = avg(nioTimes);
        double nioMBps = sizeMB / (nioAvg / 1_000_000_000.0);
        
        // Benchmark FastIO
        long[] fastIOTimes = new long[BENCHMARK_RUNS];
        for (int i = 0; i < BENCHMARK_RUNS; i++) {
            String f = writeFile2 + "_" + i;
            long start = System.nanoTime();
            fastIOWrite(f, data, sizeMB);
            long end = System.nanoTime();
            fastIOTimes[i] = end - start;
            double mbps = sizeMB / (fastIOTimes[i] / 1_000_000_000.0);
            System.out.printf("  [FastIO]  Run %d: %.1f MB/s%n", i + 1, mbps);
            Files.deleteIfExists(Paths.get(f));
        }
        double fastIOAvg = avg(fastIOTimes);
        double fastIOMBps = sizeMB / (fastIOAvg / 1_000_000_000.0);
        
        // Results
        double speedup = fastIOMBps / nioMBps;
        System.out.println();
        System.out.printf("  Java NIO Average: %.1f MB/s%n", nioMBps);
        System.out.printf("  FastIO Average:   %.1f MB/s%n", fastIOMBps);
        System.out.printf("  ⚡ SPEEDUP: %.2f×%n", speedup);
        System.out.println();
    }
    
    private static void javaNIOWrite(String path, byte[] data, int sizeMB) throws Exception {
        try (FileChannel channel = FileChannel.open(Paths.get(path), 
                StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
            ByteBuffer buffer = ByteBuffer.wrap(data);
            long written = 0;
            long target = sizeMB * MB;
            while (written < target) {
                buffer.clear();
                channel.write(buffer);
                written += data.length;
            }
        }
    }
    
    private static void fastIOWrite(String path, byte[] data, int sizeMB) throws Exception {
        FastFile file = FastIO.openWrite(path);
        ByteBuffer buffer = ByteBuffer.wrap(data);
        long written = 0;
        long target = sizeMB * MB;
        while (written < target) {
            buffer.clear();
            int w = file.write(buffer);
            written += w;
        }
        file.close();
    }
    
    // ═════════════════════════════════════════════════════════════════
    // RANDOM READ BENCHMARK
    // ═════════════════════════════════════════════════════════════════
    private static void benchmarkRandomRead() throws Exception {
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│ RANDOM READ (1GB file, 4KB random blocks)                    │");
        System.out.println("└─────────────────────────────────────────────────────────────┘");
        
        int blockSize = 4 * 1024;
        int numBlocks = 10000;
        long fileSize = 1 * GB;
        
        // Warmup
        for (int i = 0; i < WARMUP_RUNS; i++) {
            javaNIORandomRead(LARGE_FILE, blockSize, numBlocks, fileSize);
            fastIORandomRead(LARGE_FILE, blockSize, numBlocks, fileSize);
        }
        
        // Benchmark Java NIO
        long[] nioTimes = new long[BENCHMARK_RUNS];
        for (int i = 0; i < BENCHMARK_RUNS; i++) {
            long start = System.nanoTime();
            javaNIORandomRead(LARGE_FILE, blockSize, numBlocks, fileSize);
            long end = System.nanoTime();
            nioTimes[i] = end - start;
            double mbps = ((blockSize * numBlocks) / MB) / (nioTimes[i] / 1_000_000_000.0);
            System.out.printf("  [JavaNIO] Run %d: %.1f MB/s%n", i + 1, mbps);
        }
        double nioAvg = avg(nioTimes);
        double nioMBps = ((blockSize * numBlocks) / MB) / (nioAvg / 1_000_000_000.0);
        
        // Benchmark FastIO
        long[] fastIOTimes = new long[BENCHMARK_RUNS];
        for (int i = 0; i < BENCHMARK_RUNS; i++) {
            long start = System.nanoTime();
            fastIORandomRead(LARGE_FILE, blockSize, numBlocks, fileSize);
            long end = System.nanoTime();
            fastIOTimes[i] = end - start;
            double mbps = ((blockSize * numBlocks) / MB) / (fastIOTimes[i] / 1_000_000_000.0);
            System.out.printf("  [FastIO]  Run %d: %.1f MB/s%n", i + 1, mbps);
        }
        double fastIOAvg = avg(fastIOTimes);
        double fastIOMBps = ((blockSize * numBlocks) / MB) / (fastIOAvg / 1_000_000_000.0);
        
        double speedup = fastIOMBps / nioMBps;
        System.out.println();
        System.out.printf("  Java NIO Average: %.1f MB/s%n", nioMBps);
        System.out.printf("  FastIO Average:   %.1f MB/s%n", fastIOMBps);
        System.out.printf("  ⚡ SPEEDUP: %.2f×%n", speedup);
        System.out.println();
    }
    
    private static void javaNIORandomRead(String path, int blockSize, int numBlocks, long fileSize) throws Exception {
        try (FileChannel channel = FileChannel.open(Paths.get(path), StandardOpenOption.READ)) {
            ByteBuffer buffer = ByteBuffer.allocateDirect(blockSize);
            java.util.Random rand = new java.util.Random(42);
            for (int i = 0; i < numBlocks; i++) {
                long pos = Math.abs(rand.nextLong()) % (fileSize - blockSize);
                channel.position(pos);
                channel.read(buffer);
                buffer.clear();
            }
        }
    }
    
    private static void fastIORandomRead(String path, int blockSize, int numBlocks, long fileSize) throws Exception {
        FastFile file = FastIO.openRead(path);
        ByteBuffer buffer = FastFile.allocateAlignedBuffer(blockSize);
        java.util.Random rand = new java.util.Random(42);
        for (int i = 0; i < numBlocks; i++) {
            long pos = Math.abs(rand.nextLong()) % (fileSize - blockSize);
            file.seek(pos);
            file.read(buffer);
            buffer.clear();
        }
        file.close();
    }
    
    // ═════════════════════════════════════════════════════════════════
    // SMALL FILE READ BENCHMARK
    // ═════════════════════════════════════════════════════════════════
    private static void benchmarkSmallFileRead() throws Exception {
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│ SMALL FILE READ (1KB files, 10,000 files)                    │");
        System.out.println("└─────────────────────────────────────────────────────────────┘");
        
        int numFiles = 10000;
        int fileSize = 1024;
        String smallDir = TEST_DIR + "/small_files";
        Files.createDirectories(Paths.get(smallDir));
        
        // Create small files
        byte[] data = new byte[fileSize];
        for (int i = 0; i < numFiles; i++) {
            Path p = Paths.get(smallDir, "file_" + i + ".dat");
            Files.write(p, data);
        }
        
        // Warmup
        for (int i = 0; i < 2; i++) {
            javaNIOSmallFileRead(smallDir, numFiles);
            fastIOSmallFileRead(smallDir, numFiles);
        }
        
        // Benchmark
        long startNIO = System.nanoTime();
        javaNIOSmallFileRead(smallDir, numFiles);
        long nioTime = System.nanoTime() - startNIO;
        
        long startFast = System.nanoTime();
        fastIOSmallFileRead(smallDir, numFiles);
        long fastTime = System.nanoTime() - startFast;
        
        double nioPerFile = (nioTime / 1000.0) / numFiles; // microseconds
        double fastPerFile = (fastTime / 1000.0) / numFiles; // microseconds
        
        System.out.printf("  [JavaNIO] Per file: %.1f μs%n", nioPerFile);
        System.out.printf("  [FastIO]  Per file: %.1f μs%n", fastPerFile);
        System.out.printf("  ⚡ SPEEDUP: %.2f×%n", nioPerFile / fastPerFile);
        System.out.println();
    }
    
    private static void javaNIOSmallFileRead(String dir, int numFiles) throws Exception {
        for (int i = 0; i < numFiles; i++) {
            Path p = Paths.get(dir, "file_" + i + ".dat");
            Files.readAllBytes(p);
        }
    }
    
    private static void fastIOSmallFileRead(String dir, int numFiles) throws Exception {
        for (int i = 0; i < numFiles; i++) {
            String p = dir + "/file_" + i + ".dat";
            FastIO.readAllBytes(p);
        }
    }
    
    // ═════════════════════════════════════════════════════════════════
    // CSV READ BENCHMARK
    // ═════════════════════════════════════════════════════════════════
    private static void benchmarkCSVRead() throws Exception {
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│ CSV PARSE (1 million rows, 5 columns)                        │");
        System.out.println("└─────────────────────────────────────────────────────────────┘");
        
        // Warmup
        for (int i = 0; i < 2; i++) {
            javaParseCSV(CSV_FILE);
            fastParseCSV(CSV_FILE);
        }
        
        long startNIO = System.nanoTime();
        int nioRows = javaParseCSV(CSV_FILE);
        long nioTime = System.nanoTime() - startNIO;
        
        long startFast = System.nanoTime();
        int fastRows = fastParseCSV(CSV_FILE);
        long fastTime = System.nanoTime() - startFast;
        
        double nioSecs = nioTime / 1_000_000_000.0;
        double fastSecs = fastTime / 1_000_000_000.0;
        
        System.out.printf("  [JavaNIO] Parsed %,d rows in %.2f s (%,.0f rows/s)%n", 
            nioRows, nioSecs, nioRows / nioSecs);
        System.out.printf("  [FastIO]  Parsed %,d rows in %.2f s (%,.0f rows/s)%n", 
            fastRows, fastSecs, fastRows / fastSecs);
        System.out.printf("  ⚡ SPEEDUP: %.2f×%n", nioSecs / fastSecs);
        System.out.println();
    }
    
    private static int javaParseCSV(String path) throws Exception {
        int rows = 0;
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(path))) {
            String line;
            reader.readLine(); // skip header
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                rows++;
            }
        }
        return rows;
    }
    
    private static int fastParseCSV(String path) throws Exception {
        int rows = 0;
        FastCSVReader csv = new FastCSVReader(path);
        csv.setHasHeader(true);
        while (csv.nextRow()) {
            rows++;
        }
        csv.close();
        return rows;
    }
    
    // ═════════════════════════════════════════════════════════════════
    // TEXT FILE READ BENCHMARK
    // ═════════════════════════════════════════════════════════════════
    private static void benchmarkTextRead() throws Exception {
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│ TEXT FILE SCAN (100MB log file, line-by-line)                │");
        System.out.println("└─────────────────────────────────────────────────────────────┘");
        
        // Warmup
        for (int i = 0; i < 2; i++) {
            javaReadText(TEXT_FILE);
            fastReadText(TEXT_FILE);
        }
        
        long startNIO = System.nanoTime();
        int nioLines = javaReadText(TEXT_FILE);
        long nioTime = System.nanoTime() - startNIO;
        
        long startFast = System.nanoTime();
        int fastLines = fastReadText(TEXT_FILE);
        long fastTime = System.nanoTime() - startFast;
        
        double nioSecs = nioTime / 1_000_000_000.0;
        double fastSecs = fastTime / 1_000_000_000.0;
        long fileSize = Files.size(Paths.get(TEXT_FILE));
        
        double nioMBps = (fileSize / MB) / nioSecs;
        double fastMBps = (fileSize / MB) / fastSecs;
        
        System.out.printf("  [JavaNIO] Read %,d lines in %.2f s (%.1f MB/s)%n", 
            nioLines, nioSecs, nioMBps);
        System.out.printf("  [FastIO]  Read %,d lines in %.2f s (%.1f MB/s)%n", 
            fastLines, fastSecs, fastMBps);
        System.out.printf("  ⚡ SPEEDUP: %.2f×%n", nioSecs / fastSecs);
        System.out.println();
    }
    
    private static int javaReadText(String path) throws Exception {
        int lines = 0;
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(path))) {
            while (reader.readLine() != null) {
                lines++;
            }
        }
        return lines;
    }
    
    private static int fastReadText(String path) throws Exception {
        int lines = 0;
        FastTextReader reader = new FastTextReader(path);
        reader.setBufferSize(256 * 1024);
        while (reader.readLine() != null) {
            lines++;
        }
        reader.close();
        return lines;
    }
    
    // ═════════════════════════════════════════════════════════════════
    // HELPER METHODS
    // ═════════════════════════════════════════════════════════════════
    
    private static double avg(long[] times) {
        long sum = 0;
        for (long t : times) sum += t;
        return (double) sum / times.length;
    }
    
    private static void generateTestFiles() throws Exception {
        // Large file for sequential/random tests
        System.out.println("  Creating 1GB test file...");
        byte[] buffer = new byte[64 * 1024];
        java.util.Random rand = new java.util.Random(42);
        
        try (FileOutputStream fos = new FileOutputStream(LARGE_FILE)) {
            long written = 0;
            long target = 1 * GB;
            while (written < target) {
                rand.nextBytes(buffer);
                fos.write(buffer);
                written += buffer.length;
            }
        }
        
        // CSV file with 1M rows
        System.out.println("  Creating CSV file (1M rows)...");
        try (PrintWriter writer = new PrintWriter(CSV_FILE)) {
            writer.println("id,name,value,score,timestamp");
            for (int i = 0; i < 1_000_000; i++) {
                writer.printf("%d,Item%d,%.2f,%d,%d%n",
                    i, i, rand.nextDouble() * 1000, 
                    rand.nextInt(100), System.currentTimeMillis());
            }
        }
        
        // JSON file (100MB)
        System.out.println("  Creating JSON file (100MB)...");
        try (PrintWriter writer = new PrintWriter(JSON_FILE)) {
            writer.println("{");
            writer.println("  \"data\": [");
            int items = 0;
            while (Files.size(Paths.get(JSON_FILE)) < 100 * MB) {
                if (items > 0) writer.println(",");
                writer.printf("    {\"id\":%d,\"val\":%.4f,\"name\":\"item%d\"}",
                    items, rand.nextDouble(), items);
                items++;
                if (items % 10000 == 0) writer.flush();
            }
            writer.println();
            writer.println("  ]");
            writer.println("}");
        }
        
        // Text file (100MB log)
        System.out.println("  Creating text file (100MB)...");
        try (PrintWriter writer = new PrintWriter(TEXT_FILE)) {
            String[] levels = {"INFO", "WARN", "ERROR", "DEBUG"};
            while (Files.size(Paths.get(TEXT_FILE)) < 100 * MB) {
                String level = levels[rand.nextInt(levels.length)];
                writer.printf("[%s] %s - Operation completed in %d ms, result=%.4f%n",
                    level, java.time.LocalDateTime.now(), 
                    rand.nextInt(1000), rand.nextDouble());
            }
        }
        
        System.out.println("  ✓ All test files created");
    }
    
    private static void cleanup() throws Exception {
        deleteDirectory(Paths.get(TEST_DIR));
    }
    
    private static void deleteDirectory(Path dir) throws Exception {
        if (!Files.exists(dir)) return;
        Files.walk(dir)
            .sorted((a, b) -> -a.compareTo(b))
            .forEach(p -> {
                try { Files.delete(p); } catch (Exception e) {}
            });
    }
}
