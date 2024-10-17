package org.adoptopenjdk.jitwatch.model;

import org.adoptopenjdk.jitwatch.util.ParseUtil;

import java.util.*;

public class Journal {
    private List<Tag> entryList = new ArrayList();

    public Journal() {
    }

    public void addEntry(Tag entry) {
        synchronized(this.entryList) {
            this.entryList.add(entry);
        }
    }

    public List<Tag> getEntryList() {
        synchronized(this.entryList) {
            List<Tag> copy = new ArrayList(this.entryList);
            Collections.sort(copy, new Comparator<Tag>() {
                public int compare(Tag tag1, Tag tag2) {
                    long ts1 = ParseUtil.getStamp(tag1.getAttributes());
                    long ts2 = ParseUtil.getStamp(tag2.getAttributes());
                    return Long.compare(ts1, ts2);
                }
            });
            return copy;
        }
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        Iterator var2 = this.getEntryList().iterator();

        while(var2.hasNext()) {
            Tag tag = (Tag)var2.next();
            builder.append(tag.toString(true)).append('\n');
        }

        return builder.toString();
    }
}
