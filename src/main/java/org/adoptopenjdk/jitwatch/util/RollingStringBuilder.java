package org.adoptopenjdk.jitwatch.util;

import java.util.LinkedList;

public class RollingStringBuilder {
    private final int maxLines;
    private final LinkedList<String> lines;
    private final StringBuilder sb;
    private int startIndex;

    public RollingStringBuilder(int maxLines) {
        if (maxLines < 1) {
            throw new IllegalArgumentException("maxLines must be at least 1");
        }
        this.maxLines = maxLines;
        this.lines = new LinkedList<>();
        this.sb = new StringBuilder();
        this.startIndex = 0;
    }

    public RollingStringBuilder append(String text) {
        if (text == null || text.isEmpty()) {
            return this; // Nothing to append
        }

        String lineSeparator = System.lineSeparator();
        String[] splitLines = text.split("\\r?\\n");

        for (int i = 0; i < splitLines.length; i++) {
            String line = splitLines[i];
            sb.append(line);

            if (i < splitLines.length - 1 || text.endsWith("\n") || text.endsWith("\r")) {
                sb.append(lineSeparator);
                line += lineSeparator;
            }

            lines.add(line);

            if (lines.size() > maxLines) {
                String oldestLine = lines.removeFirst();
                startIndex += oldestLine.length();
            }
        }

        if (startIndex > sb.capacity() / 2) {
            sb.delete(0, startIndex);
            startIndex = 0;
        }

        return this;
    }

    public String getContent() {
        return sb.substring(startIndex);
    }

    public void clear() {
        sb.setLength(0);
        lines.clear();
        startIndex = 0;
    }

    public String toString() {
        return getContent();
    }
}
