
package com.ABC.pioneer.sensor.ble;

import android.content.Context;
import android.os.PowerManager;

import com.ABC.pioneer.sensor.analysis.Sample;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
// 稳定的一秒钟计时器，用于控制BLE操作。拥有可靠的计时器来启动和停止扫描对于可靠的检测和跟踪至关重要。
// 经过测试可能会出现失败，特定情况不具体列出，详见Pioneer文档
public class Timer {
    private final Sample sample = new Sample();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final PowerManager.WakeLock wakeLock;
    private final AtomicLong now = new AtomicLong(0);
    private final Queue<TimerDelegate> delegates = new ConcurrentLinkedQueue<>();
    private final Runnable runnable = new Runnable() {
        @Override
        public void run() {
            for (TimerDelegate delegate : delegates) {
                try {
                    delegate.bleTimer(now.get());
                } catch (Throwable e) {
                }
            }
        }
    };

    public Timer(Context context) {
        final PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Sensor:BLETimer");
        wakeLock.acquire();
        final Thread timerThread = new Thread(new Runnable() {
            private long last = 0;

            @Override
            public void run() {
                while (true) {
                    now.set(System.currentTimeMillis());
                    final long elapsed = now.get() - last;
                    if (elapsed >= 1000) {
                        if (last != 0) {
                            sample.add(elapsed);
                            executorService.execute(runnable);
                        }
                        last = now.get();
                    }
                    try {
                        Thread.sleep(500);
                    } catch (Throwable e) {
                    }
                }
            }
        });
        timerThread.setPriority(Thread.MAX_PRIORITY);
        timerThread.setName("Sensor.BLETimer");
        timerThread.start();
    }

    @Override
    protected void finalize() {
        wakeLock.release();
    }

    /// 添加时间通知委托
    public void add(TimerDelegate delegate) {
        delegates.add(delegate);
    }
}
