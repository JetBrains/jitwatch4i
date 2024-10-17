package org.adoptopenjdk.jitwatch.ui.code;

import org.adoptopenjdk.jitwatch.core.JITWatchConstants;
import org.adoptopenjdk.jitwatch.journal.AbstractJournalVisitable;
import org.adoptopenjdk.jitwatch.journal.JournalUtil;
import org.adoptopenjdk.jitwatch.model.*;
import org.adoptopenjdk.jitwatch.treevisitor.ITreeVisitable;
import org.adoptopenjdk.jitwatch.util.ParseUtil;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class InlineAnalyzer implements ITreeVisitable
{
    private final IReadOnlyJITDataModel model;
    private final Predicate<IMetaMember> filter;
    private final List<InlineFailureInfo> failures = new ArrayList<>();
    private List<InlineFailureGroup> failureGroups;

    public InlineAnalyzer(IReadOnlyJITDataModel model, Predicate<IMetaMember> filter)
    {
        this.model = model;
        this.filter = filter;
    }

    public List<InlineFailureInfo> getFailures()
    {
        return failures;
    }

    public List<InlineFailureGroup> getFailureGroups()
    {
        if (failureGroups == null)
        {
            Map<IMetaMember, List<InlineFailureInfo>> groupedFailures = failures.stream()
                    .filter(info -> !"not inlineable".equals(info.getReason()) && !"no static binding".equals(info.getReason()))
                    .collect(Collectors.groupingBy(InlineFailureInfo::getCallee));

            failureGroups = groupedFailures.entrySet().stream()
                    .map(entry ->
                    {
                        IMetaMember callee = entry.getKey();
                        List<InlineFailureInfo> infos = entry.getValue();
                        Set<String> reasons = infos.stream()
                                .map(InlineFailureInfo::getReason)
                                .collect(Collectors.toSet());
                        List<InlineCallSite> callSites = infos.stream()
                                .map(info -> new InlineCallSite(info.getCallSite(), info.getBci()))
                                .collect(Collectors.toList());
                        InlineFailureInfo firstInfo = infos.get(0);
                        return new InlineFailureGroup(
                                callee,
                                firstInfo.getCalleeSize(),
                                firstInfo.getCalleeInvocationCount(),
                                reasons,
                                callSites
                        );
                    })
                    .sorted(Comparator.comparingInt((InlineFailureGroup group) -> group.getCallSites().size()).reversed())
                    .collect(Collectors.toList());
        }
        return failureGroups;
    }

    @Override
    public void visit(IMetaMember mm)
    {
        if (mm == null || !mm.isCompiled())
        {
            return;
        }
        try
        {
            JournalUtil.visitParseTagsOfLastTask(mm.getJournal(), new InlineJournalVisitor(model, mm, failures, filter));
        }
        catch (LogParseException e)
        {
        }
    }

    @Override
    public void reset()
    {
        // No implementation needed for reset in this context
    }

    private static class InlineJournalVisitor extends AbstractJournalVisitable
    {
        private final IReadOnlyJITDataModel model;
        private final IMetaMember callSite;
        private final List<InlineFailureInfo> failures;
        private final Predicate<IMetaMember> filter;

        public InlineJournalVisitor(IReadOnlyJITDataModel model, IMetaMember callSite,
                                    List<InlineFailureInfo> failures, Predicate<IMetaMember> filter)
        {
            this.model = model;
            this.callSite = callSite;
            this.failures = failures;
            this.filter = filter;
        }

        @Override
        public void visitTag(Tag toVisit, IParseDictionary parseDictionary)
        {
            processParseTag(toVisit, parseDictionary, null);
        }

        private void processParseTag(Tag toVisit, IParseDictionary parseDictionary, Integer bci)
        {
            String methodID = null;
            int currentBCI = 0;

            for (Tag child : toVisit.getChildren())
            {
                String tagName = child.getName();
                Map<String, String> tagAttrs = child.getAttributes();

                switch (tagName)
                {
                    case JITWatchConstants.TAG_METHOD:
                        methodID = tagAttrs.get(JITWatchConstants.ATTR_ID);
                        break;

                    case JITWatchConstants.TAG_BC:
                        String newBCI = tagAttrs.get(JITWatchConstants.ATTR_BCI);
                        if (newBCI != null)
                        {
                            currentBCI = Integer.parseInt(newBCI);
                        }
                        break;

                    case JITWatchConstants.TAG_CALL:
                        methodID = tagAttrs.get(JITWatchConstants.ATTR_METHOD);
                        break;

                    case JITWatchConstants.TAG_INLINE_FAIL:
                        String reason = tagAttrs.get(JITWatchConstants.ATTR_REASON);
                        Tag methodTag = parseDictionary.getMethod(methodID);
                        IMetaMember metaMember = ParseUtil.lookupMember(methodID, parseDictionary, model);

                        if (metaMember != null && filter.test(metaMember))
                        {
                            int calleeSize = -1;
                            Integer calleeInvocationCount = null;
                            if (methodTag != null)
                            {
                                String bytesStr = methodTag.getAttributes().get(JITWatchConstants.ATTR_BYTES);
                                if (bytesStr != null)
                                {
                                    calleeSize = Integer.parseInt(bytesStr);
                                }
                                String iicountStr = methodTag.getAttributes().get(JITWatchConstants.ATTR_IICOUNT);
                                if (iicountStr != null)
                                {
                                    calleeInvocationCount = Integer.parseInt(iicountStr);
                                }
                            }
                            String processedReason = (reason != null)
                                    ? reason.replace("&lt;", "<").replace("&gt;", ">")
                                    : "Unknown";
                            failures.add(new InlineFailureInfo(
                                    callSite,
                                    (bci != null) ? bci : currentBCI,
                                    metaMember,
                                    calleeSize,
                                    calleeInvocationCount,
                                    processedReason
                            ));
                        }
                        methodID = null;
                        break;

                    case JITWatchConstants.TAG_PARSE:
                        processParseTag(child, parseDictionary, (bci != null) ? bci : currentBCI);
                        break;

                    case JITWatchConstants.TAG_PHASE:
                        String phaseName = tagAttrs.get(JITWatchConstants.ATTR_NAME);
                        if (JITWatchConstants.S_PARSE_HIR.equals(phaseName))
                        {
                            visitTag(child, parseDictionary);
                        }
                        break;

                    default:
                        handleOther(child);
                        break;
                }
            }
        }
    }
}
