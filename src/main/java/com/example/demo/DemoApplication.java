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
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;

import static java.lang.foreign.ValueLayout.JAVA_INT;

@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
@Warmup(iterations = 100,time = 100, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 100,time = 100, timeUnit = TimeUnit.MILLISECONDS)
@Threads(8)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class DemoApplication {

   static class Counter{
      public int count = 0;
   }

   public static final VarHandle  segmentVarHandler = JAVA_INT.varHandle();

   public static final VarHandle counterVarHandler;

   public static final Field countField;

   Unsafe unsafe;

   long offset;

   Counter counter1;

   Counter counter2;

   Counter counter3;

   Counter counter4;

   MemorySegment segmentNative;

   MemorySegment segmentHeap;

   @Setup
   public void init() throws NoSuchFieldException, IllegalAccessException {
      counter1 = new Counter();
      counter2 = new Counter();
      counter3 = new Counter();
      counter4 = new Counter();


      Field theUnsafeF = Unsafe.class.getDeclaredField("theUnsafe");
      theUnsafeF.setAccessible(true);
      unsafe = ((Unsafe) theUnsafeF.get(null));
      offset = unsafe.objectFieldOffset(Counter.class.getDeclaredField("count"));


      segmentNative  = MemorySegment.allocateNative(JAVA_INT, SegmentScope.global());
      segmentHeap = MemorySegment.ofArray(new int[1]);


   }
   static {
      try {
         counterVarHandler = MethodHandles.lookup()
                 .findVarHandle(Counter.class, "count", int.class);

         countField  = Counter.class.getDeclaredField("count");
         countField.setAccessible(true);
      } catch (NoSuchFieldException e) {
         throw new RuntimeException(e);
      } catch (IllegalAccessException e) {
         throw new RuntimeException(e);
      }
   }



   @Benchmark
   public void testVarhandlerGetterNative(Blackhole blackhole) {
      int o = (int)segmentVarHandler.get(segmentNative);
      blackhole.consume(o);
   }

   @Benchmark
   public void testVarhandlerGetterHeap(Blackhole blackhole) {
      int o = (int)segmentVarHandler.get(segmentHeap);
      blackhole.consume(o);
   }

   @Benchmark
   public void testVarhandlerGetter(Blackhole blackhole) {
      int o = (int)counterVarHandler.get(counter1);
      blackhole.consume(o);
   }

   @Benchmark
   public void testUnsafeGetter(Blackhole blackhole) {
      int object = unsafe.getInt(counter2, offset);
      blackhole.consume(object);
   }

   @Benchmark
   public void testNormalGetter(Blackhole blackhole) {
      blackhole.consume(counter3.count);
   }

   @Benchmark
   public void testRelectionGetter(Blackhole blackhole) throws IllegalAccessException {
      blackhole.consume(countField.get(counter4));
   }



   public static void main(String[] args) throws RunnerException {
      Options opt = new OptionsBuilder()
              .include(DemoApplication.class.getSimpleName())
              .output("tmp.log")
              .build();

      new Runner(opt).run();
   }



}

