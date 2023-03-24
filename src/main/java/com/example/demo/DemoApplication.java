package com.example.demo;


import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import sun.misc.Unsafe;


import java.lang.foreign.MemorySegment;

import java.lang.foreign.SegmentScope;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;

import static java.lang.foreign.ValueLayout.JAVA_INT;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@Warmup(iterations = 100,time = 100, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 10,time = 1, timeUnit = TimeUnit.SECONDS)
@Threads(8)
@Fork(2)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class DemoApplication {

   static class Counter{
      public int count = 0;
   }

   VarHandle counterVarHandler;

   Unsafe unsafe;

   long offset;

   Counter counter1;

   Counter counter2;

   Counter counter3;

   MemorySegment segmentNative;

   MemorySegment segmentHeap;

   @Setup
   public void init() throws NoSuchFieldException, IllegalAccessException {
      counter1 = new Counter();
      counter2 = new Counter();
      counter3 = new Counter();


      Field theUnsafeF = Unsafe.class.getDeclaredField("theUnsafe");
      theUnsafeF.setAccessible(true);
      unsafe = ((Unsafe) theUnsafeF.get(null));
      offset = unsafe.objectFieldOffset(Counter.class.getDeclaredField("count"));

      counterVarHandler = JAVA_INT.varHandle();
      segmentNative  = MemorySegment.allocateNative(JAVA_INT, SegmentScope.global());
      segmentHeap = MemorySegment.ofArray(new int[1]);
   }



   @Benchmark
   public void testVarhandlerGetterNative(Blackhole blackhole) {
      Object o = counterVarHandler.get(segmentNative);
      blackhole.consume(o);
   }

   @Benchmark
   public void testVarhandlerGetterHeap(Blackhole blackhole) {
      Object o = counterVarHandler.get(segmentHeap);
      blackhole.consume(o);
   }

   @Benchmark
   public void testUnsafeGetter(Blackhole blackhole) {
      Object object = unsafe.getObject(counter2, offset);
      blackhole.consume(object);
   }

   @Benchmark
   public void testNormalGetter(Blackhole blackhole) {
      blackhole.consume(counter3.count);
   }



   public static void main(String[] args) throws RunnerException {
      Options opt = new OptionsBuilder()
              .include(DemoApplication.class.getSimpleName())
              .output("tmp.log")
              .build();

      new Runner(opt).run();
   }



}

