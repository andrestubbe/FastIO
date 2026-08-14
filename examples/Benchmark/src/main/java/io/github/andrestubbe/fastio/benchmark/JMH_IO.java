package io.github.andrestubbe.fastio.benchmark;

import io.github.andrestubbe.fastio.FastIO;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
public class JMH_IO {

    @Benchmark
    public void benchmarkFastIOInit() {
        FastIO.init();
    }
}
