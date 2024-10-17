package org.adoptopenjdk.jitwatch.journal;

import org.adoptopenjdk.jitwatch.logger.Logger;
import org.adoptopenjdk.jitwatch.logger.LoggerFactory;
import org.adoptopenjdk.jitwatch.model.*;
import org.adoptopenjdk.jitwatch.util.ParseUtil;
import org.adoptopenjdk.jitwatch.util.StringUtil;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

public final class JournalUtil {
    private static final Logger logger = LoggerFactory.getLogger(JournalUtil.class);
    private static int unhandledTagCount = 0;

    private JournalUtil() {
    }

    public static void visitParseTagsOfLastTask(Journal journal, IJournalVisitable visitable) throws LogParseException {
        Task lastTask = getLastTask(journal);
        if (lastTask == null) {
            if (!isJournalForCompile2NativeMember(journal)) {
                logger.warn("No Task found in Journal");
                if (journal != null && journal.getEntryList().size() > 0) {
                    logger.warn(journal.toString());
                }
            }
        } else {
            IParseDictionary parseDictionary = lastTask.getParseDictionary();
            Tag parsePhase = getParsePhase(lastTask);
            if (parsePhase != null) {
                List<Tag> parseTags = parsePhase.getNamedChildren("parse");
                Iterator var6 = parseTags.iterator();

                while (var6.hasNext()) {
                    Tag parseTag = (Tag) var6.next();
                    visitable.visitTag(parseTag, parseDictionary);
                }
            }
        }

    }

    public static void visitOptimizerTagsOfLastTask(Journal journal, IJournalVisitable visitable) throws LogParseException {
        Task lastTask = getLastTask(journal);
        if (lastTask == null) {
            if (!isJournalForCompile2NativeMember(journal)) {
                logger.warn("No Task found in Journal");
                if (journal != null && journal.getEntryList().size() > 0) {
                    logger.warn(journal.toString());
                }
            }
        } else {
            IParseDictionary parseDictionary = lastTask.getParseDictionary();
            Tag optimizerPhase = getOptimizerPhase(lastTask);
            if (optimizerPhase != null) {
                Iterator var5 = optimizerPhase.getChildren().iterator();

                while (var5.hasNext()) {
                    Tag child = (Tag) var5.next();
                    visitable.visitTag(child, parseDictionary);
                }
            }
        }

    }

    public static void visitEliminationTagsOfLastTask(Journal journal, IJournalVisitable visitable) throws LogParseException {
        Task lastTask = getLastTask(journal);
        if (lastTask == null) {
            if (!isJournalForCompile2NativeMember(journal)) {
                logger.warn("No Task found in Journal");
                if (journal != null && journal.getEntryList().size() > 0) {
                    logger.warn(journal.toString());
                }
            }
        } else {
            IParseDictionary parseDictionary = lastTask.getParseDictionary();
            Iterator var4 = lastTask.getNamedChildren("eliminate_allocation").iterator();

            while (var4.hasNext()) {
                Tag child = (Tag) var4.next();
                visitable.visitTag(child, parseDictionary);
            }
        }

    }

    public static boolean isJournalForCompile2NativeMember(Journal journal) {
        boolean result = false;
        if (journal != null) {
            List<Tag> entryList = journal.getEntryList();
            if (entryList.size() >= 1) {
                Tag tag = (Tag) entryList.get(0);
                String tagName = tag.getName();
                if ("nmethod".equals(tagName) && "c2n".equals(tag.getAttributes().get("compile_kind"))) {
                    result = true;
                }
            }
        }

        return result;
    }

    public static boolean memberMatchesKlassID(IMetaMember member, String klassID, IParseDictionary parseDictionary) {
        boolean result = false;
        String klassName = ParseUtil.lookupType(klassID, parseDictionary);
        String memberClassName = member.getMetaClass().getFullyQualifiedName();
        result = memberClassName.equals(klassName);
        return result;
    }

    public static boolean memberMatchesMethodID(IMetaMember member, String methodID, IParseDictionary parseDictionary) {
        boolean result = false;
        Tag methodTag = parseDictionary.getMethod(methodID);
        if (methodTag != null) {
            Map<String, String> methodTagAttributes = methodTag.getAttributes();
            String klassID = (String) methodTagAttributes.get("holder");
            Tag klassTag = parseDictionary.getKlass(klassID);
            if (klassTag != null) {
                String klassAttrName = (String) klassTag.getAttributes().get("name");
                String methodAttrName = StringUtil.replaceXMLEntities((String) methodTagAttributes.get("name"));
                if (klassAttrName != null) {
                    klassAttrName = klassAttrName.replace('/', '.');
                }

                String returnType = ParseUtil.getMethodTagReturn(methodTag, parseDictionary);
                List<String> paramTypes = ParseUtil.getMethodTagArguments(methodTag, parseDictionary);
                boolean nameMatches;
                if ("<init>".equals(methodAttrName)) {
                    nameMatches = member.getMemberName().equals(klassAttrName);
                } else {
                    nameMatches = member.getMemberName().equals(methodAttrName);
                }

                boolean klassMatches = member.getMetaClass().getFullyQualifiedName().equals(klassAttrName);
                boolean returnMatches = member.getReturnTypeName().equals(returnType);
                boolean paramsMatch = true;
                if (member.getParamTypeNames().length == paramTypes.size()) {
                    for (int pos = 0; pos < member.getParamTypeNames().length; ++pos) {
                        String memberParamType = member.getParamTypeNames()[pos];
                        String tagParamType = (String) paramTypes.get(pos);
                        if (!memberParamType.equals(tagParamType)) {
                            paramsMatch = false;
                            break;
                        }
                    }
                } else {
                    paramsMatch = false;
                }

                result = nameMatches && klassMatches && returnMatches && paramsMatch;
            }
        }

        return result;
    }

    public static Task getLastTask(Journal journal) {
        Task lastTask = null;
        if (journal != null) {
            Iterator var2 = journal.getEntryList().iterator();

            while (var2.hasNext()) {
                Tag tag = (Tag) var2.next();
                if (tag instanceof Task) {
                    lastTask = (Task) tag;
                }
            }
        }

        return lastTask;
    }

    private static Tag getParsePhase(Task lastTask) {
        Tag parsePhase = null;
        if (lastTask != null) {
            List<Tag> phasesBuildIR = lastTask.getNamedChildrenWithAttribute("phase", "name", "buildIR");
            if (phasesBuildIR.size() == 1) {
                parsePhase = (Tag) phasesBuildIR.get(0);
            } else {
                List<Tag> phasesParse = lastTask.getNamedChildrenWithAttribute("phase", "name", "parse");
                if (phasesParse.size() == 1) {
                    parsePhase = (Tag) phasesParse.get(0);
                } else {
                    logger.warn("Unexpected parse phase count: buildIR({}), parse({})", phasesBuildIR.size(), phasesParse.size());
                    parsePhase = lastTask;
                }
            }
        }

        return (Tag) parsePhase;
    }

    private static Tag getOptimizerPhase(Task lastTask) {
        Tag optimizerPhase = null;
        if (lastTask != null) {
            List<Tag> parsePhases = lastTask.getNamedChildrenWithAttribute("phase", "name", "optimizer");
            int count = parsePhases.size();
            if (count > 1) {
                logger.warn("Unexpected optimizer phase count: {}", count);
            } else if (count == 1) {
                optimizerPhase = (Tag) parsePhases.get(0);
            }
        }

        return optimizerPhase;
    }

    public static void unhandledTag(IJournalVisitable visitable, Tag child) {
        ++unhandledTagCount;
        logger.warn("{} did not handle {}", visitable.getClass().getName(), child.toString(false));
    }

    public static int getUnhandledTagCount() {
        return unhandledTagCount;
    }
}
