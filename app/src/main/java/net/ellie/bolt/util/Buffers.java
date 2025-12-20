package net.ellie.bolt.util;

import net.ellie.bolt.decoder.buffer.CircularByteBuffer;
import net.ellie.bolt.decoder.buffer.CircularCharBuffer;
import net.ellie.bolt.dsp.buffers.CircularDoubleBuffer;
import net.ellie.bolt.dsp.buffers.CircularFloatBuffer;

public class Buffers {
    public static final CircularByteBuffer EMPTY_BYTE_BUFFER = new CircularByteBuffer(0);
    public static final CircularCharBuffer EMPTY_CHAR_BUFFER = new CircularCharBuffer(0);
    public static final CircularFloatBuffer EMPTY_FLOAT_BUFFER = new CircularFloatBuffer(0);
    public static final CircularDoubleBuffer EMPTY_DOUBLE_BUFFER = new CircularDoubleBuffer(0);
}
