package org.adoptopenjdk.jitwatch.journal;

import org.adoptopenjdk.jitwatch.model.IParseDictionary;
import org.adoptopenjdk.jitwatch.model.LogParseException;
import org.adoptopenjdk.jitwatch.model.Tag;

public interface IJournalVisitable {
    void visitTag(Tag var1, IParseDictionary var2) throws LogParseException;
}
