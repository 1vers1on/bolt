package net.ellie.bolt.dsp.buffers;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class CircularFloatBuffer {
    private final float[] buffer;
    private final int capacity;
    private int writeIndex = 0;
    private int readIndex = 0;
    private int size = 0;
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notEmpty = lock.newCondition();
    private final Condition notFull = lock.newCondition();

    public CircularFloatBuffer(int capacity) {
        this.capacity = capacity;
        this.buffer = new float[capacity];
    }

    public void write(float[] data, int offset, int length) throws InterruptedException {
        lock.lock();
        try {
            while (size + length > capacity) {
                notFull.await();
            }
            int remaining = length;
            int srcPos = offset;
            while (remaining > 0) {
                int toWrite = Math.min(remaining, capacity - writeIndex);
                System.arraycopy(data, srcPos, buffer, writeIndex, toWrite);
                srcPos += toWrite;
                writeIndex = (writeIndex + toWrite) % capacity;
                remaining -= toWrite;
            }
            size += length;
            notEmpty.signalAll();
        } finally {
            lock.unlock();
        }
    }

    public void read(float[] dest, int offset, int length) throws InterruptedException {
        lock.lock();
        try {
            while (size < length) {
                notEmpty.await();
            }
            int remaining = length;
            int destPos = offset;
            while (remaining > 0) {
                int toRead = Math.min(remaining, capacity - readIndex);
                System.arraycopy(buffer, readIndex, dest, destPos, toRead);
                destPos += toRead;
                readIndex = (readIndex + toRead) % capacity;
                remaining -= toRead;
            }
            size -= length;
            notFull.signalAll();
        } finally {
            lock.unlock();
        }
    }

    public int available() {
        lock.lock();
        try {
            return size;
        } finally {
            lock.unlock();
        }
    }
}
