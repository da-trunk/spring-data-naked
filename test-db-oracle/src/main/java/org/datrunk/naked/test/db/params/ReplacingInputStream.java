package org.datrunk.naked.test.db.params;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.LinkedList;

import liquibase.parser.core.xml.XMLChangeLogSAXParser;
import liquibase.util.BomAwareInputStream;

/**
 * Taken from <a href=
 * "https://stackoverflow.com/questions/7743534/filter-search-and-replace-array-of-bytes-in-an-inputstream/11158499">filter-search-and-replace-array-of-bytes-in-an-inputstream</a>
 *
 */
public class ReplacingInputStream extends PushbackInputStream {
    LinkedList<Integer> inQueue = new LinkedList<Integer>();
    LinkedList<Integer> outQueue = new LinkedList<Integer>();
    final byte[] search, replacement;

    protected ReplacingInputStream(InputStream in, String search, String replacement) throws UnsupportedEncodingException {
        super(in);
        this.search = search.getBytes("UTF-8");
        this.replacement = replacement.getBytes("UTF-8");
    }

    private boolean isMatchFound() {
        Iterator<Integer> inIter = inQueue.iterator();
        int i = 0;
        for (; i < search.length; i++)
            if (!inIter.hasNext() || search[i] != inIter.next())
                return false;
        if (i > 0)
            System.out.println("matched to " + i);
        return true;
    }

    private void readAhead() throws IOException {
        // Work up some look-ahead.
        while (inQueue.size() < search.length) {
            int next = super.read();
            inQueue.offer(next);
            if (next == -1)
                break;
        }
    }

    @Override
    public int read() throws IOException {
        // Next byte already determined.
        if (outQueue.isEmpty()) {
            readAhead();

            if (isMatchFound()) {
                for (int i = 0; i < search.length; i++)
                    inQueue.remove();

                for (byte b : replacement)
                    outQueue.offer((int) b);
            } else
                outQueue.add(inQueue.remove());
        }

        return outQueue.remove();
    }

    /**
     * {@link XMLChangeLogSAXParser#parseToNode} wraps us in a {@link BomAwareInputStream}. That sometimes fails to call {@link #read()}.
     * This causes bytes to be skipped and XML parsing to fail. So, I copied the implementation from {@link InputStream#read} here. This
     * prevents {@link PushbackInputStream#read(byte[], int, int)} from working properly, but it also avoids skipping any bytes.
     * 
     */
    @Override
    public synchronized int read(byte b[], int off, int len) throws IOException {
        if (b == null) {
            throw new NullPointerException();
        } else if (off < 0 || len < 0 || len > b.length - off) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return 0;
        }

        int c = read();
        if (c == -1) {
            return -1;
        }
        b[off] = (byte) c;

        int i = 1;
        try {
            for (; i < len; i++) {
                c = read();
                if (c == -1) {
                    break;
                }
                b[off + i] = (byte) c;
            }
        } catch (IOException ee) {}
        return i;
    }

    @Override
    public synchronized long skip(long n) throws IOException {
        return super.skip(n);
    }

    @Override
    public boolean markSupported() {
        return false;
    }

    @Override
    public synchronized void reset() throws IOException {
        super.reset();
    }
}
