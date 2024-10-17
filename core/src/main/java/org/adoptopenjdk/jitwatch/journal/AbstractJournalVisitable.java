package org.adoptopenjdk.jitwatch.journal;

import org.adoptopenjdk.jitwatch.model.Tag;

import java.util.HashSet;
import java.util.Set;

public abstract class AbstractJournalVisitable implements IJournalVisitable {
    protected Set<String> ignoreTags = new HashSet();

    public AbstractJournalVisitable() {
    }

    protected void handleOther(Tag tag) {
        if (!this.ignoreTags.contains(tag.getName())) {
            JournalUtil.unhandledTag(this, tag);
        }

    }
}
