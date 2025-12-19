package net.ellie.bolt.decoder.buffer;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class CircularCharBuffer {
    private final char[] buffer;
    private final int capacity;
    private int writeIndex = 0;
    private int readIndex = 0;
    private int size = 0;
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notEmpty = lock.newCondition();
    private final Condition notFull = lock.newCondition();

    public CircularCharBuffer(int capacity) {
        this.capacity = capacity;
        this.buffer = new char[capacity];
    }

    public void write(char[] data, int offset, int length) throws InterruptedException {
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

    public void read(char[] dest, int offset, int length) throws InterruptedException {
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

    public int readNonBlocking(char[] dest, int offset, int length) {
        lock.lock();
        try {
            int toReadTotal = Math.min(length, size);
            int remaining = toReadTotal;
            int destPos = offset;

            while (remaining > 0) {
                int toRead = Math.min(remaining, capacity - readIndex);
                System.arraycopy(buffer, readIndex, dest, destPos, toRead);
                destPos += toRead;
                readIndex = (readIndex + toRead) % capacity;
                remaining -= toRead;
            }

            size -= toReadTotal;
            if (toReadTotal > 0) {
                notFull.signalAll();
            }
            return toReadTotal;
        } finally {
            lock.unlock();
        }
    }

    public int writeNonBlocking(char[] data, int offset, int length) {
        lock.lock();
        try {
            int free = capacity - size;
            int toWriteTotal = Math.min(length, free);
            int remaining = toWriteTotal;
            int srcPos = offset;

            while (remaining > 0) {
                int toWrite = Math.min(remaining, capacity - writeIndex);
                System.arraycopy(data, srcPos, buffer, writeIndex, toWrite);
                srcPos += toWrite;
                writeIndex = (writeIndex + toWrite) % capacity;
                remaining -= toWrite;
            }

            size += toWriteTotal;
            if (toWriteTotal > 0) {
                notEmpty.signalAll();
            }
            return toWriteTotal;
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
