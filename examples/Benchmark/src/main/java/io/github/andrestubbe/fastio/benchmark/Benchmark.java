package io.github.andrestubbe.fastio.benchmark;

import io.github.andrestubbe.fastio.FastCSVReader;
import io.github.andrestubbe.fastio.FastFile;
import io.github.andrestubbe.fastio.FastIO;
import io.github.andrestubbe.fastio.FastTextReader;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Standard OpenJDK JMH Microbenchmark Suite for FastIO.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 2, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class Benchmark {

    private static final String BENCH_DIR = "jmh_bench_tmp";
    private static final String TEST_FILE = BENCH_DIR + "/bench_data.dat";
    private static final String CSV_FILE = BENCH_DIR + "/bench_data.csv";
    private static final String TXT_FILE = BENCH_DIR + "/bench_data.txt";

    private ByteBuffer readBuffer;
    private ByteBuffer writeBuffer;

    @Setup
    public void setup() throws Exception {
        FastIO.init();
        Files.createDirectories(Path.of(BENCH_DIR));

        // Create 10MB test file for buffer reads
        byte[] data = new byte[64 * 1024];
        new Random(42).nextBytes(data);
        try (FileOutputStream fos = new FileOutputStream(TEST_FILE)) {
            for (int i = 0; i < 160; i++) {
                fos.write(data);
            }
        }

        // Create CSV file (10,000 rows)
        try (PrintWriter pw = new PrintWriter(CSV_FILE)) {
            pw.println("id,name,value,category,active");
            for (int i = 0; i < 10000; i++) {
                pw.printf("%d,item_%d,%.2f,CAT_%d,true%n", i, i, i * 1.5, i % 5);
            }
        }

        // Create text file (10,000 lines)
        try (PrintWriter pw = new PrintWriter(TXT_FILE)) {
            for (int i = 0; i < 10000; i++) {
                pw.printf("[INFO] 2026-09-17 Operation completed for task_%d%n", i);
            }
        }

        readBuffer = FastFile.allocateAlignedBuffer(64 * 1024);
        writeBuffer = FastFile.allocateAlignedBuffer(64 * 1024);
        for (int i = 0; i < writeBuffer.capacity(); i++) {
            writeBuffer.put((byte) (i & 0xFF));
        }
        writeBuffer.flip();
    }

    @TearDown
    public void tearDown() {
        try {
            Files.walk(Path.of(BENCH_DIR))
                .sorted((a, b) -> -a.compareTo(b))
                .map(Path::toFile)
                .forEach(File::delete);
        } catch (Exception ignored) {
        }
    }

    @org.openjdk.jmh.annotations.Benchmark
    public void benchmarkFastIOInit() {
        FastIO.init();
    }

    @org.openjdk.jmh.annotations.Benchmark
    public void benchmarkOptimalBufferSize(Blackhole bh) {
        bh.consume(FastIO.getOptimalBufferSize());
    }

    @org.openjdk.jmh.annotations.Benchmark
    public void benchmarkFileRead(Blackhole bh) throws Exception {
        readBuffer.clear();
        try (FastFile file = FastIO.openRead(TEST_FILE)) {
            int read = file.read(readBuffer);
            bh.consume(read);
        }
    }

    @org.openjdk.jmh.annotations.Benchmark
    public void benchmarkSequentialWrite(Blackhole bh) throws Exception {
        writeBuffer.position(0);
        String outPath = BENCH_DIR + "/write_test.dat";
        try (FastFile file = FastIO.openWrite(outPath)) {
            int written = file.write(writeBuffer);
            bh.consume(written);
        }
    }

    @org.openjdk.jmh.annotations.Benchmark
    public void benchmarkCSVScan(Blackhole bh) throws Exception {
        int count = 0;
        try (FastCSVReader reader = new FastCSVReader(CSV_FILE)) {
            while (reader.nextRow()) {
                count++;
            }
        }
        bh.consume(count);
    }

    @org.openjdk.jmh.annotations.Benchmark
    public void benchmarkTextScan(Blackhole bh) throws Exception {
        int lines = 0;
        try (FastTextReader reader = new FastTextReader(TXT_FILE)) {
            while (reader.readLine() != null) {
                lines++;
            }
        }
        bh.consume(lines);
    }
}
