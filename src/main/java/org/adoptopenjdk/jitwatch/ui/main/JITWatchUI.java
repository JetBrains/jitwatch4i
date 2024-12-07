/*
 * Copyright (c) 2013-2022 Chris Newland.
 * Licensed under https://github.com/AdoptOpenJDK/jitwatch/blob/master/LICENSE-BSD
 * Instructions: https://github.com/AdoptOpenJDK/jitwatch/wiki
 */
package org.adoptopenjdk.jitwatch.ui.main;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import com.intellij.ui.table.JBTable;
import org.adoptopenjdk.jitwatch.compilation.codecache.CodeCacheEventWalker;
import org.adoptopenjdk.jitwatch.compilation.codecache.CodeCacheWalkerResult;
import org.adoptopenjdk.jitwatch.core.ErrorLog;
import org.adoptopenjdk.jitwatch.core.IJITListener;
import org.adoptopenjdk.jitwatch.core.JITWatchConfig;
import org.adoptopenjdk.jitwatch.core.JITWatchConstants;
import org.adoptopenjdk.jitwatch.model.*;
import org.adoptopenjdk.jitwatch.parser.ILogParseErrorListener;
import org.adoptopenjdk.jitwatch.parser.ILogParser;
import org.adoptopenjdk.jitwatch.parser.ParserFactory;
import org.adoptopenjdk.jitwatch.parser.ParserType;
import org.adoptopenjdk.jitwatch.report.Report;
import org.adoptopenjdk.jitwatch.report.comparator.ScoreComparator;
import org.adoptopenjdk.jitwatch.report.escapeanalysis.eliminatedallocation.EliminatedAllocationWalker;
import org.adoptopenjdk.jitwatch.report.locks.OptimisedLocksWalker;
import org.adoptopenjdk.jitwatch.report.suggestion.SuggestionWalker;
import org.adoptopenjdk.jitwatch.ui.code.*;
import org.adoptopenjdk.jitwatch.ui.compilechain.CompileChainPanel;
import org.adoptopenjdk.jitwatch.ui.graphing.CodeCachePanel;
import org.adoptopenjdk.jitwatch.ui.graphing.HistoPanel;
import org.adoptopenjdk.jitwatch.ui.graphing.TimeLinePanel;
import org.adoptopenjdk.jitwatch.ui.log.LogPanel;
import org.adoptopenjdk.jitwatch.ui.nmethod.codecache.CodeCacheLayoutPanel;
import org.adoptopenjdk.jitwatch.ui.nmethod.compilerthread.CompilerThreadPanel;
import org.adoptopenjdk.jitwatch.ui.parserchooser.IParserSelectedListener;
import org.adoptopenjdk.jitwatch.ui.parserchooser.ParserChooser;
import org.adoptopenjdk.jitwatch.ui.report.ReportPanel;
import org.adoptopenjdk.jitwatch.ui.report.ReportStageType;
import org.adoptopenjdk.jitwatch.ui.toplist.TopListPanel;
import org.adoptopenjdk.jitwatch.util.RollingStringBuilder;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import static org.adoptopenjdk.jitwatch.core.JITWatchConstants.DEFAULT_PACKAGE_NAME;

public class JITWatchUI implements IJITListener, ILogParseErrorListener, IStageAccessProxy,
        IMemberSelectedListener, IParserSelectedListener
{
    private static final Logger logger = Logger.getInstance(JITWatchUI.class);

    private boolean selectedProgrammatically = false;

    private final Project project;
    private final ContentManager contentManager;

    private CodeToolWindowManager codeToolWindowManager;

    private File jitLogFile = null;
    private ILogParser logParser;
    private boolean isReadingLogFile = false;

    private MetaClass selectedMetaClass;
    private IMetaMember selectedMember;

    private String lastVmCommand = null;
    private MetaClass lastSelectedClass = null;
    private IMetaMember lastSelectedMember = null;

    private JPanel mainPanel;

    private ActionToolbar actionToolbar;
    private ClassTreePanel classTree;
    private ClassMemberListPanel classMemberList;

    private JBTable compilationTable;
    private DefaultTableModel compilationTableModel;

    private Content mainContent;
    private TimeLinePanel timeLinePanel;
    private Content timeLineContent;
    private HistoPanel histoPanel;
    private Content histoContent;
    private TopListPanel topListPanel;
    private Content topListContent;
    private CodeCachePanel codeCacheTimelinePanel;
    private Content codeCacheTimelineContent;
    private CodeCacheLayoutPanel codeCacheBlocksPanel;
    private Content codeCacheBlocksContent;
    private CompilerThreadPanel compilerThreadPanel;
    private Content compilerThreadContent;
    private CompileChainPanel compileChainPanel;
    private Content compileChainContent;
    private ReportPanel ellimAllocsReportPanel;
    private Content ellimAllocsReportContent;
    private ReportPanel suggestionReportPanel;
    private Content suggestionReportContent;
    private ReportPanel optimisedLockPanel;
    private Content optimizedLockContent;

    private LogPanel logPanel;
    private Content logContent;

    private JLabel lblVmVersion;
    private JLabel lblHeap;

    private List<ICompilationChangeListener> listenerCompilationChanged = new ArrayList<>();

    private List<Report> reportListSuggestions = new ArrayList<>();
    private List<Report> reportListEliminatedAllocations = new ArrayList<>();
    private List<Report> reportListOptimisedLocks = new ArrayList<>();

    private CodeCacheWalkerResult codeCacheWalkerResult;

    private StringBuffer logBuffer = new StringBuffer();
    private RollingStringBuilder rollingLogBuffer = new RollingStringBuilder(5_000);

    private ErrorLog errorLog = new ErrorLog();
    private int errorCount = 0;

    private boolean repaintTree = false;

    private ParserChooser parserChooser;

    private long parseStartTime;

    private String[] compilationTableColumnNames = {
            "Queued",
            "Compile Start",
            "NMethod Emit",
            "Native Size",
            "Compiler",
            "Level"
    };

    public JITWatchUI(Project project, ContentManager contentManager)
    {
        this.project = project;
        this.contentManager = contentManager;
    }

    private void readLogFile()
    {
        closeAllTabs();

        if (codeToolWindowManager == null)
        {
            codeToolWindowManager = JitWatchCodeUtil.registerToolWindows(project, this);
            listenerCompilationChanged.add(codeToolWindowManager);
        }

        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Loading compilation log", false)
        {
            @Override
            public void run(@NotNull ProgressIndicator indicator)
            {
                try
                {
                    logParser.processLogFile(jitLogFile, JITWatchUI.this);
                    JitWatchModelService.getInstance(project).setParserResult(logParser);
                    SwingUtilities.invokeLater(() ->
                    {
                        openAllTabs();
                    });
                }
                catch (IOException ioe)
                {
                    log("Exception during log processing: " + ioe.toString());
                }
            }
        });
    }

    @Override
    public void handleReadStart()
    {
        parseStartTime = System.currentTimeMillis();
        isReadingLogFile = true;
        clear();

        SwingUtilities.invokeLater(new Runnable()
        {
            @Override
            public void run()
            {
                updateButtons();
            }
        });
    }

    private void clear()
    {
        lastVmCommand = logParser.getVMCommand();
        lastSelectedMember = selectedMember;
        lastSelectedClass = selectedMetaClass;

        selectedMember = null;
        codeCacheWalkerResult = null;

        errorCount = 0;
        errorLog.clear();

        reportListSuggestions.clear();
        reportListEliminatedAllocations.clear();
        reportListOptimisedLocks.clear();

        SwingUtilities.invokeLater(new Runnable()
        {
            @Override
            public void run()
            {
                compilationTableModel.setRowCount(0);
                classMemberList.clear();
                updateButtons();
                classTree.clear();
                metaClassSelectedFromClassTree(null);
                logPanel.clear();
                notifyCompilationChanged(null);
                closeAllTabs();
            }
        });
    }

    @Override
    public void handleReadComplete()
    {
        long totalParseTime = System.currentTimeMillis() - parseStartTime;
        log("Finished reading log file within=" + totalParseTime + "ms.");

        isReadingLogFile = false;
        buildSuggestions();
        buildEliminatedAllocationReport();
        buildOptimisedLocksReport();

        SwingUtilities.invokeLater(new Runnable()
        {
            @Override
            public void run()
            {
                updateButtons();
                notifyCompilationChanged(selectedMember);
            }
        });

        logParser.discardParsedLogs();
    }

    private void buildSuggestions()
    {
        log("Finding code suggestions.");
        SuggestionWalker walker = new SuggestionWalker(logParser.getModel());
        reportListSuggestions = walker.getReports(new ScoreComparator());
        log("Found " + reportListSuggestions.size() + " code suggestions.");
    }

    private void buildEliminatedAllocationReport()
    {
        log("Finding eliminated allocations");
        EliminatedAllocationWalker walker = new EliminatedAllocationWalker(logParser.getModel());
        reportListEliminatedAllocations = walker.getReports(new ScoreComparator());
        log("Found " + reportListEliminatedAllocations.size() + "  eliminated allocations.");
    }

    private void buildOptimisedLocksReport()
    {
        log("Finding optimised locks");
        OptimisedLocksWalker walker = new OptimisedLocksWalker(logParser.getModel());
        reportListOptimisedLocks = walker.getReports(new ScoreComparator());
        log("Found " + reportListOptimisedLocks.size() + " optimised locks.");
    }

    private CodeCacheWalkerResult buildCodeCacheResult()
    {
        CodeCacheEventWalker compilationWalker = new CodeCacheEventWalker(logParser.getModel());
        compilationWalker.walkCompilations();
        return compilationWalker.getResult();
    }

    public CodeCacheWalkerResult getCodeCacheWalkerResult()
    {
        if (codeCacheWalkerResult == null || codeCacheWalkerResult.getEvents().isEmpty())
        {
            codeCacheWalkerResult = buildCodeCacheResult();
        }

        return codeCacheWalkerResult;
    }

    @Override
    public void handleError(final String title, final String body)
    {
        logger.error(title);

        SwingUtilities.invokeLater(new Runnable()
        {
            @Override
            public void run()
            {
                JOptionPane.showMessageDialog(mainPanel, body, title, JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    @Override
    public JITWatchConfig getConfig()
    {
        return logParser.getConfig();
    }

    public void start()
    {
        mainPanel = new JPanel(new BorderLayout());
        parserChooser = new ParserChooser(this);

        ComboBox<ParserType> comboParser = parserChooser.getCombo();
        String parserProperty = System.getProperty("jitwatch.parser", ParserType.HOTSPOT.toString());
        comboParser.setSelectedItem(ParserType.fromString(parserProperty));

        AnAction openLogAction = new AnAction("Open the JIT log file for analysis", "Open Log", AllIcons.Actions.Menu_open)
        {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e)
            {
                stopParsing();
                SwingUtilities.invokeLater(() ->
                {
                    if (chooseJITLog(project))
                    {
                        readLogFile();
                    }
                });
            }
        };

        AnAction stopAction = new AnAction("Stop loading", "Stop", AllIcons.Actions.Suspend)
        {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e)
            {
                stopParsing();
            }
            @Override
            public void update(@NotNull AnActionEvent e)
            {
                Presentation presentation = e.getPresentation();
                presentation.setEnabled(isReadingLogFile);
            }
        };

        AnAction resetAction = new AnAction("Reset", "Reset", AllIcons.Actions.Restart)
        {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e)
            {
                logParser.reset();
                JitWatchModelService.getInstance(project).setParserResult(null);
                clear();
            }
        };

        DefaultActionGroup actionGroup = new DefaultActionGroup();

        actionGroup.add(openLogAction);
        actionGroup.add(stopAction);
        actionGroup.add(resetAction);

        DefaultActionGroup popupGroup = new DefaultActionGroup("Options", true);

        popupGroup.add(new ToggleAction("Hide Interfaces")
        {
            @Override
            public boolean isSelected(@NotNull AnActionEvent e)
            {
                return getConfig().isHideInterfaces();
            }

            @Override
            public void setSelected(@NotNull AnActionEvent e, boolean state)
            {
                getConfig().setHideInterfaces(state);
                getConfig().saveConfig();
                clearAndRefreshTreeView(true);
            }
        });

        popupGroup.add(new ToggleAction("Hide Uncompiled Classes")
        {
            @Override
            public boolean isSelected(@NotNull AnActionEvent e)
            {
                return getConfig().isShowOnlyCompiledClasses();
            }

            @Override
            public void setSelected(@NotNull AnActionEvent e, boolean state)
            {
                getConfig().setShowOnlyCompiledClasses(state);
                getConfig().saveConfig();
                clearAndRefreshTreeView(true);
            }
        });

        popupGroup.add(new ToggleAction("Hide Uncompiled Members")
        {
            @Override
            public boolean isSelected(@NotNull AnActionEvent e)
            {
                return getConfig().isShowOnlyCompiledMembers();
            }

            @Override
            public void setSelected(@NotNull AnActionEvent e, boolean state)
            {
                getConfig().setShowOnlyCompiledMembers(state);
                getConfig().saveConfig();
                if (classMemberList != null)
                {
                    classMemberList.refresh();
                }
            }
        });

        AnAction showPopupAction = new AnAction(null, "Options", AllIcons.Actions.Show)
        {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                Component component = e.getInputEvent().getComponent();
                if (component instanceof JComponent) {
                    ActionPopupMenu popupMenu = ActionManager.getInstance().createActionPopupMenu(
                            ActionPlaces.UNKNOWN, popupGroup);
                    popupMenu.getComponent().show((JComponent) component, 0, 0);
                }
            }
        };

        actionGroup.add(showPopupAction);

        actionToolbar = ActionManager.getInstance().createActionToolbar("SideToolbar", actionGroup, false);
        actionToolbar.setOrientation(SwingConstants.VERTICAL);
        actionToolbar.setTargetComponent(mainPanel);
        JComponent toolbarComponent = actionToolbar.getComponent();

        mainPanel.add(toolbarComponent, BorderLayout.WEST);

        lblHeap = new JLabel();

        lblVmVersion = new JLabel();

        StringBuilder vmBuilder = new StringBuilder();

        vmBuilder.append("VM: ");
        vmBuilder.append(System.getProperty("java.vendor"));
        vmBuilder.append(" JDK");
        vmBuilder.append(System.getProperty("java.version"));
        vmBuilder.append(" build ");
        vmBuilder.append(System.getProperty("java.runtime.version"));

        lblVmVersion.setText(vmBuilder.toString());

        compilationTableModel = new DefaultTableModel(compilationTableColumnNames, 0);
        compilationTable = new JBTable(compilationTableModel);
        compilationTable.setFillsViewportHeight(true);

        compilationTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        compilationTable.getSelectionModel().addListSelectionListener(new ListSelectionListener()
        {
            @Override
            public void valueChanged(ListSelectionEvent e)
            {
                if (!selectedProgrammatically)
                {
                    if (selectedMember != null && !e.getValueIsAdjusting())
                    {
                        int selectedIndex = compilationTable.getSelectedRow();
                        if (selectedIndex >= 0)
                        {
                            selectedMember.setSelectedCompilation(selectedIndex);
                            setSelectedMetaMemberFromCompilationTable();
                        }
                    }
                }
            }
        });

        JBSplitter spMethodInfo = new JBSplitter(true); // true for vertical split
        spMethodInfo.setProportion(0.5f); // Set initial proportion (0.0f to 1.0f)
        spMethodInfo.setDividerWidth(5); // Optional: set divider width

        classMemberList = new ClassMemberListPanel(this, getConfig());
        classMemberList.registerListener(this);

        spMethodInfo.setFirstComponent(classMemberList);
        spMethodInfo.setSecondComponent(new JBScrollPane(compilationTable));

        classTree = new ClassTreePanel(this, getConfig());

        JBSplitter spMain = new JBSplitter(false); // false for horizontal split
        spMain.setProportion(0.3f); // Adjust proportion as needed
        spMain.setDividerWidth(5);

        spMain.setFirstComponent(classTree);
        spMain.setSecondComponent(spMethodInfo);

        log("Welcome to JITWatch by Chris Newland (@chriswhocodes on Twitter) and the AdoptOpenJDK project.\n");
        log("Please report issues via GitHub (https://github.com/AdoptOpenJDK/jitwatch).\n");
        log("Includes an assembly reference from x86asm.net licenced under http://ref.x86asm.net/index.html#License\n");

        if (jitLogFile == null)
        {
            log("Choose a JIT log file");
        }
        else
        {
            log("Using JIT log file: " + jitLogFile.getAbsolutePath());
        }

        JPanel hboxBottom = new JPanel();
        hboxBottom.setLayout(new FlowLayout(FlowLayout.LEFT, 4, 4));

        JLabel labelParser = new JLabel("Select Parser");

        hboxBottom.add(labelParser);
        hboxBottom.add(comboParser);
        hboxBottom.add(lblHeap);
        hboxBottom.add(Box.createHorizontalGlue());
        hboxBottom.add(lblVmVersion);

        mainPanel.add(spMain, BorderLayout.CENTER);
        mainPanel.add(hboxBottom, BorderLayout.SOUTH);

        mainContent = contentManager.getFactory().createContent(this.mainPanel, "Main", false);
        contentManager.addContent(mainContent, 0);

        logPanel = new LogPanel();
        logContent = contentManager.getFactory().createContent(logPanel, "Log", false);
        contentManager.addContent(logContent);

        int refreshMillis = 1000;

        Timer timer = new Timer(refreshMillis, new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                refresh();
            }
        });
        timer.start();
        updateButtons();
    }

    public IReadOnlyJITDataModel getJITDataModel()
    {
        return logParser.getModel();
    }

    private void updateButtons()
    {
        actionToolbar.updateActionsImmediately();
    }

    public boolean focusTreeOnClass(MetaClass metaClass, boolean unsetSelection)
    {
        List<String> path = metaClass.getTreePath();

        clearAndRefreshTreeView(unsetSelection);

        DefaultMutableTreeNode curNode = classTree.getRootItem();

        StringBuilder builtPath = new StringBuilder();

        int pathLength = path.size();
        int pos = 0;

        int rowsAbove = 0;
        boolean found = false;

        for (String part : path)
        {
            builtPath.append(part);
            String matching;
            found = false;

            if (pos++ == pathLength - 1)
            {
                matching = part;
            }
            else
            {
                matching = builtPath.toString();
            }

            Enumeration<TreeNode> children = curNode.children();
            while (children.hasMoreElements())
            {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) children.nextElement();
                rowsAbove++;

                String nodeText = node.getUserObject().toString();

                if (matching.equals(nodeText) || (matching.isEmpty() && DEFAULT_PACKAGE_NAME.equals(nodeText)))
                {
                    builtPath.append(".");
                    curNode = node;
                    classTree.select(curNode);
                    found = true;
                    break;
                }
            }
        }

        if (found)
        {
            classTree.scrollPathToVisible(curNode);
            lastSelectedClass = null;
        }

        return found;
    }

    public void focusTreeOnMember(IMetaMember member)
    {
        if (member != null)
        {
            MetaClass metaClass = member.getMetaClass();
            boolean found = focusTreeOnClass(metaClass, true);

            if (found)
            {
                classMemberList.selectMember(member);
                selectMember(member, false, true);
                lastSelectedMember = null;
            }
        }
    }

    public void focusTreeInternal(IMetaMember member)
    {
        if (member != null)
        {
            MetaClass metaClass = member.getMetaClass();

            boolean found = focusTreeOnClass(metaClass, false);

            if (found)
            {
                classMemberList.clearClassMembers();
                selectedMetaClass = metaClass;
                classMemberList.setMetaClass(metaClass);
                classMemberList.selectMember(member);
                lastSelectedMember = null;
            }
            else
            {
                log("Could not focus tree on " + member.toStringUnqualifiedMethodName(false, true));

                if (getConfig().isShowOnlyCompiledClasses())
                {
                    log("Perhaps this class doesn't contain any compiled methods and 'Hide uncompiled classes' is selected");
                }

                classMemberList.clearClassMembers();
                classMemberList.setMetaClass(null);
                classMemberList.selectMember(null);
            }
        }
    }

    @Override
    public void openInlinedIntoReport(IMetaMember member)
    {
        if (member != null)
        {
//            log("Finding inlined into reports for " + member.toStringUnqualifiedMethodName(true, true));
//
//            InliningWalker walker = new InliningWalker(logParser.getModel(), member);
//
//            List<Report> inlinedIntoMemberList = walker.getReports(new ScoreComparator());
//
//            log("Found " + inlinedIntoMemberList.size() + " locations.");
//
//            ReportStage inlinedIntoStage = new ReportStage(JITWatchSwingUI.this,
//                    "Inlining report for callee " + member.toStringUnqualifiedMethodName(true, true), ReportStageType.INLINING,
//                    inlinedIntoMemberList);
//
//            StageManager.addAndShow(contentManager, inlinedIntoStage);
        }
    }

    public void openJournalViewer(String title, IMetaMember member)
    {
        /*
        if (member.isCompiled())
        {
            JournalViewerStage jvs = new JournalViewerStage(this, title, member.getSelectedCompilation());
            StageManager.addAndShow(contentManager, jvs);
        }
         */
    }

    private boolean chooseJITLog(Project project)
    {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Choose JIT log file");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setAcceptAllFileFilterUsed(false);
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Log Files", "log"));

        String searchDir = getConfig().getLastLogDir();
        if (searchDir == null)
        {
            searchDir = System.getProperty("user.dir");
        }

        fileChooser.setCurrentDirectory(new File(searchDir));

        int returnValue = fileChooser.showOpenDialog(mainPanel);
        if (returnValue == JFileChooser.APPROVE_OPTION)
        {
            File selectedFile = fileChooser.getSelectedFile();
            setJITLogFile(selectedFile);

            JITWatchConfig config = getConfig();

            if (JITWatchConstants.S_PROFILE_SANDBOX.equals(config.getProfileName()))
            {
                logParser.getConfig().switchFromSandbox();
            }
            return true;
        }
        return false;
    }

    private void setJITLogFile(File logFile)
    {
        jitLogFile = logFile;

        getConfig().setLastLogDir(jitLogFile.getParent());
        getConfig().saveConfig();

        logPanel.clear();
        log("Selected log file: " + jitLogFile.getAbsolutePath());

        log("\nUsing Config: " + getConfig().getProfileName());

        log("\nClick Start button to process the JIT log");
        updateButtons();

        refreshLog();
    }

    private void stopParsing()
    {
        if (isReadingLogFile)
        {
            logParser.stopParsing();
            isReadingLogFile = false;
            updateButtons();

            if (jitLogFile != null)
            {
                log("Stopped parsing " + jitLogFile.getAbsolutePath());
            }
        }
    }

    private boolean sameVmCommand()
    {
        boolean same = false;

        if (lastVmCommand != null && logParser.getVMCommand() != null)
        {
            same = lastVmCommand.equals(logParser.getVMCommand());

            if (!same)
            {
                // vm command known and not same so flush open node history
                classTree.clearOpenPackageHistory();
                lastVmCommand = null;
            }
        }

        return same;
    }

    private void setSelectedMetaMemberFromCompilationTable()
    {
        notifyCompilationChanged(selectedMember);
    }

    @Override
    public synchronized void selectMember(IMetaMember member, boolean updateTree, boolean updateTriView)
    {
        selectedProgrammatically = true;
        selectedMember = member;
        compilationTableModel.setRowCount(0);

        if (selectedMember != null)
        {
            notifyCompilationChanged(selectedMember);

            if (updateTree)
            {
                focusTreeInternal(selectedMember);
            }

            if (updateTriView)
            {
                codeToolWindowManager.openClassAtMember(member);
            }

            if (selectedMember != null)
            {
                for (Compilation compilation : selectedMember.getCompilations())
                {
                    CompilationTableRow row = new CompilationTableRow(compilation);

                    Object[] rowData = new Object[compilationTableColumnNames.length];

                    rowData[0] = row.getStampQueued();
                    rowData[1] = row.getStampCompilationStart();
                    rowData[2] = row.getStampNMethodEmitted();
                    rowData[3] = row.getNative();
                    rowData[4] = row.getCompiler();
                    rowData[5] = row.getLevel();

                    compilationTableModel.addRow(rowData);
                }

                Compilation selectedCompilation = selectedMember.getSelectedCompilation();

                if (selectedCompilation != null)
                {
                    int index = selectedCompilation.getIndex();
                    compilationTable.getSelectionModel().setSelectionInterval(index, index);
                }
            }
        }

        selectedProgrammatically = false;
    }

    @Override
    public synchronized void selectCompilation(IMetaMember member, int compilationIndex)
    {
        selectedProgrammatically = true;
        selectedMember = member;

        if (selectedMember != null)
        {
            selectedMember.setSelectedCompilation(compilationIndex);
            selectMember(selectedMember, true, true);
        }

        selectedProgrammatically = false;
    }

    private void refresh()
    {
        boolean sameVmCommandAsLastRun = sameVmCommand();

        if (repaintTree)
        {
            repaintTree = false;
            classTree.showTree(sameVmCommandAsLastRun);
        }

        if (sameVmCommandAsLastRun)
        {
            if (lastSelectedMember != null)
            {
                focusTreeOnMember(lastSelectedMember);
            }
            else if (lastSelectedClass != null)
            {
                focusTreeOnClass(lastSelectedClass, true);
            }
        }

        if (timeLinePanel != null)
        {
            timeLinePanel.repaint();
        }

        if (codeCacheTimelinePanel != null)
        {
            codeCacheTimelinePanel.repaint();
        }

        if (histoPanel != null)
        {
            histoPanel.repaint();
        }

        if (topListPanel != null)
        {
            topListPanel.repaint();
        }

        if (logBuffer.length() > 0)
        {
            refreshLog();
        }

        long totalMemory = Runtime.getRuntime().totalMemory();
        long freeMemory = Runtime.getRuntime().freeMemory();
        long usedMemory = totalMemory - freeMemory;

        long megabyte = 1024 * 1024;

        String heapString = "Heap: " + (usedMemory / megabyte) + "/" + (totalMemory / megabyte) + "M";

        lblHeap.setText(heapString);
    }

    private void refreshLog()
    {
        String newText = logBuffer.toString();
        rollingLogBuffer.append(newText);
        String logText = rollingLogBuffer.toString();
        logPanel.setText(logText);
        logPanel.setCaretPosition(logText.length() - 1);
        logBuffer.delete(0, newText.length());
    }

    public IMetaMember getSelectedMember()
    {
        return selectedMember;
    }

    void clearAndRefreshTreeView(boolean unsetSelection)
    {
        if (unsetSelection)
        {
            selectedMember = null;
            selectedMetaClass = null;
        }

        classTree.clear();
        classTree.showTree(sameVmCommand());
    }

    @Override
    public void handleJITEvent(JITEvent event)
    {
        log(event.toString());
        repaintTree = true;
    }

    @Override
    public void handleLogEntry(String entry)
    {
        log(entry);
    }

    @Override
    public void handleErrorEntry(String entry)
    {
        errorLog.addEntry(entry);
        errorCount++;
    }

    private void log(final String entry)
    {
        logBuffer.append(entry);
        logBuffer.append("\n");
    }

    void metaClassSelectedFromClassTree(MetaClass metaClass)
    {
        classMemberList.clearClassMembers();
        selectedMetaClass = metaClass;

        selectMember(null, false, true);

        classMemberList.setMetaClass(metaClass);
    }

    public PackageManager getPackageManager()
    {
        return logParser.getModel().getPackageManager();
    }

    public void notifyCompilationChanged(IMetaMember member)
    {
        for (ICompilationChangeListener listener : listenerCompilationChanged)
        {
            listener.compilationChanged(member);
        }
    }

    @Override
    public void parserSelected(ParserType parserType)
    {
        if (logParser != null)
        {
            logParser.reset();
        }

        logParser = ParserFactory.getParser(parserType, this);
    }

    private void openAllTabs()
    {
        if (optimisedLockPanel == null)
        {
            optimisedLockPanel = new ReportPanel(this, ReportStageType.ELIDED_LOCK, reportListOptimisedLocks);
            optimizedLockContent = contentManager.getFactory().createContent(optimisedLockPanel, optimisedLockPanel.getTitle(), false);
            contentManager.addContent(optimizedLockContent, 1);
        }
        if (suggestionReportPanel == null)
        {
            suggestionReportPanel = new ReportPanel(this, ReportStageType.SUGGESTION, reportListSuggestions);
            suggestionReportContent = contentManager.getFactory().createContent(suggestionReportPanel, suggestionReportPanel.getTitle(), false);
            contentManager.addContent(suggestionReportContent, 1);
        }
        if (ellimAllocsReportPanel == null)
        {
            ellimAllocsReportPanel = new ReportPanel(this, ReportStageType.ELIMINATED_ALLOCATION, reportListEliminatedAllocations);
            ellimAllocsReportContent = contentManager.getFactory().createContent(ellimAllocsReportPanel, ellimAllocsReportPanel.getTitle(), false);
            contentManager.addContent(ellimAllocsReportContent, 1);
        }
        if (compileChainPanel == null)
        {
            compileChainPanel = new CompileChainPanel(this, this, logParser.getModel());
            compileChainContent = contentManager.getFactory().createContent(compileChainPanel, compileChainPanel.getTitle(), false);
            contentManager.addContent(compileChainContent, 1);
            listenerCompilationChanged.add(compileChainPanel);
        }
        if (codeCacheBlocksPanel == null)
        {
            codeCacheBlocksPanel = new CodeCacheLayoutPanel(this);
            codeCacheBlocksContent = contentManager.getFactory().createContent(codeCacheBlocksPanel, codeCacheBlocksPanel.getTitle(), false);
            contentManager.addContent(codeCacheBlocksContent, 1);
            listenerCompilationChanged.add(codeCacheBlocksPanel);
        }
        if (codeCacheTimelinePanel == null)
        {
            codeCacheTimelinePanel = new CodeCachePanel(this);
            codeCacheTimelineContent = contentManager.getFactory().createContent(codeCacheTimelinePanel, codeCacheTimelinePanel.getTitle(), false);
            contentManager.addContent(codeCacheTimelineContent, 1);
        }
        if (histoPanel == null)
        {
            histoPanel = new HistoPanel(this);
            histoContent = contentManager.getFactory().createContent(histoPanel, histoPanel.getTitle(), false);
            contentManager.addContent(histoContent, 1);
        }
        if (timeLinePanel == null)
        {
            timeLinePanel = new TimeLinePanel(this);
            timeLineContent = contentManager.getFactory().createContent(timeLinePanel, timeLinePanel.getTitle(), false);
            contentManager.addContent(timeLineContent, 1);
        }
        if (compilerThreadPanel == null)
        {
            compilerThreadPanel = new CompilerThreadPanel(this);
            compilerThreadContent = contentManager.getFactory().createContent(compilerThreadPanel, compilerThreadPanel.getTitle(), false);
            contentManager.addContent(compilerThreadContent, 1);
            listenerCompilationChanged.add(compilerThreadPanel);
        }
        if (topListPanel == null)
        {
            topListPanel = new TopListPanel(this, getJITDataModel());
            topListContent = contentManager.getFactory().createContent(topListPanel, topListPanel.getTitle(), false);
            contentManager.addContent(topListContent, 1);
        }


        {
            if (selectedMember == null && selectedMetaClass != null)
            {
                selectedMember = selectedMetaClass.getFirstConstructor();
            }
            //openTriView(selectedMember);
        }
    }

    private void closeAllTabs()
    {
        if (topListContent != null)
        {
            contentManager.removeContent(topListContent, true);
            topListPanel = null;
            topListContent = null;
        }
        if (compilerThreadContent != null)
        {
            contentManager.removeContent(compilerThreadContent, true);
            compilerThreadPanel = null;
            compilerThreadContent = null;
        }
        if (timeLineContent != null)
        {
            contentManager.removeContent(timeLineContent, true);
            timeLinePanel = null;
            timeLineContent = null;
        }
        if (histoContent != null)
        {
            contentManager.removeContent(histoContent, true);
            histoPanel = null;
            histoContent = null;
        }
        if (codeCacheTimelineContent != null)
        {
            contentManager.removeContent(codeCacheTimelineContent, true);
            codeCacheTimelinePanel = null;
            codeCacheTimelineContent = null;
        }
        if (codeCacheBlocksContent != null)
        {
            contentManager.removeContent(codeCacheBlocksContent, true);
            codeCacheBlocksPanel = null;
            codeCacheBlocksContent = null;
        }
        if (compileChainContent != null)
        {
            contentManager.removeContent(compileChainContent, true);
            compileChainPanel = null;
            compileChainContent = null;
        }
        if (ellimAllocsReportContent != null)
        {
            contentManager.removeContent(ellimAllocsReportContent, true);
            ellimAllocsReportPanel = null;
            ellimAllocsReportContent = null;
        }
        if (suggestionReportContent != null)
        {
            contentManager.removeContent(suggestionReportContent, true);
            suggestionReportPanel = null;
            suggestionReportContent = null;
        }
        if (optimizedLockContent != null)
        {
            contentManager.removeContent(optimizedLockContent, true);
            optimisedLockPanel = null;
            optimizedLockContent = null;
        }

        listenerCompilationChanged.clear();
    }

}
