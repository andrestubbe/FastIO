package fastio.benchmark;

import io.github.andrestubbe.fastio.FastIO;
import org.openjdk.jmh.annotations.*;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Thread)
@Warmup(iterations = 1, time = 1)
@Measurement(iterations = 2, time = 1)
@Fork(1)
public class JMH_IO {

    private File tempFile;

    @Setup
    public void setup() throws Exception {
        tempFile = File.createTempFile("fastio_test", ".txt");
        tempFile.deleteOnExit();
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            for (int i = 0; i < 1000; i++) {
                fos.write("123,456.789,item_name_sample,2026-08-14,active\n".getBytes());
            }
        }
    }

    @Benchmark
    public Object benchmarkFastIONativeRead() {
        return FastIO.readFile(tempFile.getAbsolutePath());
    }

    @Benchmark
    public Object benchmarkJavaFilesRead() throws Exception {
        return Files.readAllBytes(tempFile.toPath());
    }
}
