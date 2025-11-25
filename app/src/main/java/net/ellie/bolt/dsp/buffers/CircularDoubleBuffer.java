package net.ellie.bolt.dsp.buffers;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class CircularDoubleBuffer {
    private final double[] buffer;
    private final int capacity;
    private int writeIndex = 0;
    private int readIndex = 0;
    private int size = 0;
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notEmpty = lock.newCondition();
    private final Condition notFull = lock.newCondition();

    public CircularDoubleBuffer(int capacity) {
        this.capacity = capacity;
        this.buffer = new double[capacity];
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
                for (int i = 0; i < toWrite; i++) {
                    buffer[writeIndex + i] = data[srcPos + i];
                }
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

    public void write(double[] data, int offset, int length) throws InterruptedException {
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
                for (int i = 0; i < toRead; i++) {
                    dest[destPos + i] = (float) buffer[readIndex + i];
                }
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

    public void read(double[] dest, int offset, int length) throws InterruptedException {
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
