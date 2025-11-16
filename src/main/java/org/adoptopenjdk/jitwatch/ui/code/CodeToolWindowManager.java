package org.adoptopenjdk.jitwatch.ui.code;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.event.CaretEvent;
import com.intellij.openapi.editor.event.CaretListener;
import com.intellij.openapi.fileEditor.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.pom.Navigatable;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;
import com.intellij.util.OpenSourceUtil;
import org.adoptopenjdk.jitwatch.model.IMetaMember;
import org.adoptopenjdk.jitwatch.model.bytecode.LineTable;
import org.adoptopenjdk.jitwatch.model.bytecode.LineTableEntry;
import org.adoptopenjdk.jitwatch.ui.code.languages.JitWatchLanguageSupport;
import org.adoptopenjdk.jitwatch.ui.main.ICompilationChangeListener;

import static org.adoptopenjdk.jitwatch.ui.code.languages.JitWatchLanguageSupportUtil.LanguageSupport;

public class CodeToolWindowManager implements ICompilationChangeListener
{
    private final Project project;
    private final ToolWindow toolWindow;
    private final ViewerByteCode byteCodePanel;
    private final ViewerAssembly assemblyPanel;

    private final JitWatchModelService modelService;

    private Editor activeSourceEditor;
    private PsiFile activeSourceFile;
    private boolean movingCaretInSource = false;
    private int lastEditorBytecodeOffset;
    private int lastEditorCaretLinePosition;

    public CodeToolWindowManager(Project project, ToolWindow toolWindow)
    {
        this.project = project;
        this.toolWindow = toolWindow;

        this.byteCodePanel = new ViewerByteCode(project);
        toolWindow.getContentManager().addContent(
                ContentFactory.getInstance().createContent(byteCodePanel, "ByteCode", false)
        );
        this.assemblyPanel = new ViewerAssembly(project);
        toolWindow.getContentManager().addContent(
                ContentFactory.getInstance().createContent(assemblyPanel, "Asm", false)
        );

        this.modelService = JitWatchModelService.getInstance(project);

        byteCodePanel.setCodeToolWindowManager(this);
        assemblyPanel.setCodeToolWindowManager(this);

        ContentManager contentManager = toolWindow.getContentManager();

        project.getMessageBus().connect(contentManager).subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, new FileEditorManagerListener()
        {
            @Override
            public void selectionChanged(FileEditorManagerEvent event)
            {
                updateContent(event.getNewFile(), event.getNewEditor(), true);
            }
        });

        EditorFactory.getInstance().getEventMulticaster().addCaretListener(new CaretListener()
        {
            @Override
            public void caretPositionChanged(CaretEvent e)
            {
                if (e.getEditor() == activeSourceEditor && !movingCaretInSource)
                {
                    syncViewerToEditor(e.getNewPosition());
                }
            }
        }, contentManager);

        updateContentFromSelectedEditor();
        modelService.addUpdateListener(this::updateContentFromSelectedEditor);
    }

    @Override
    public void compilationChanged(IMetaMember member)
    {
        assemblyPanel.setCurrentMember(member, true);
        IMetaMember currentMember = assemblyPanel.getCurrentMember();
        if (currentMember != null)
        {
            assemblyPanel.navigateToMemberBcOffsetOrLine(currentMember, lastEditorBytecodeOffset, lastEditorCaretLinePosition + 1);
        }
    }

    public void moveSourceEditorCaretToLine(int line)
    {
        activeSourceEditor.getCaretModel().moveToLogicalPosition(new LogicalPosition(line, 0));
        activeSourceEditor.getScrollingModel().scrollToCaret(ScrollType.MAKE_VISIBLE);
    }

    private void setCurrentMember(IMetaMember currentMember)
    {
        byteCodePanel.setCurrentMember(currentMember, false);
        assemblyPanel.setCurrentMember(currentMember, false);
    }

    private void updateContentFromSelectedEditor()
    {
        FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
        VirtualFile[] selectedFiles = fileEditorManager.getSelectedFiles();
        FileEditor[] selectedEditors = fileEditorManager.getSelectedEditors();
        VirtualFile file = selectedFiles.length > 0 ? selectedFiles[0] : null;
        FileEditor editor = selectedEditors.length > 0 ? selectedEditors[0] : null;
        updateContent(file, editor, true);
    }

    public void openClassAtMember(IMetaMember member)
    {
        String qualifiedName = member.getMetaClass().getFullyQualifiedName();
        JavaPsiFacade javaPsiFacade = JavaPsiFacade.getInstance(project);
        PsiClass psiClass = javaPsiFacade.findClass(qualifiedName, GlobalSearchScope.allScope(project));

        if (psiClass == null && qualifiedName.contains("$"))
        {
            String ideaQualifiedName = qualifiedName.replaceAll("\\$", ".");
            psiClass = javaPsiFacade.findClass(ideaQualifiedName, GlobalSearchScope.allScope(project));
        }

        if (psiClass == null)
        {
            showNotification(project, "Class Not Found", "Cannot find class: " + qualifiedName, NotificationType.ERROR);
            return;
        }

        JitWatchLanguageSupport<PsiElement, PsiElement> languageSupport = LanguageSupport.forLanguage(psiClass.getLanguage());

        if (languageSupport == null)
        {
            showNotification(project, "Unsupported language.", "Cannot open class: " + qualifiedName, NotificationType.ERROR);
        }

        PsiElement targetElement = languageSupport.findMemberElement(project, psiClass, member);

        if (targetElement == null)
        {
            showNotification(project, "Member Not Found", "Cannot find the specified member in the class.", NotificationType.ERROR);
            return;
        }

        if (targetElement instanceof Navigatable)
        {
            OpenSourceUtil.navigate((Navigatable) targetElement);
        }
        else
        {
            OpenSourceUtil.navigate(psiClass);
        }
    }

    private void showNotification(Project project, String title, String content, NotificationType notificationType)
    {
        Notifications.Bus.notify(new Notification(
                "OpenClassAtMember",
                title,
                content,
                notificationType
        ), project);
    }

    protected void showMessage(String message)
    {
        byteCodePanel.showMessage(message);
        assemblyPanel.showMessage(message);
    }

    private void updateContent(VirtualFile file, FileEditor fileEditor, boolean syncCaret)
    {
        if (fileEditor instanceof TextEditor)
        {
            activeSourceEditor = ((TextEditor) fileEditor).getEditor();
        }
        else
        {
            activeSourceEditor = null;
        }

        if (activeSourceEditor == null)
        {
            showMessage("Please open a text editor");
            return;
        }

        if (modelService.getModel() == null)
        {
            showMessage("Please open a HotSpot compilation log");
            return;
        }

        if (file == null)
        {
            showMessage("Please select a source file");
            return;
        }

        activeSourceFile = ReadAction.compute(() -> activeSourceFile = PsiManager.getInstance(project).findFile(file));

        if (activeSourceFile == null)
        {
            showMessage("Please select a source file");
            return;
        }

        JitWatchLanguageSupport<PsiElement, PsiElement> language = LanguageSupport.forLanguage(activeSourceFile.getLanguage());
        if (language == null)
        {
            showMessage("Please select a file in a supported language");
            return;
        }

        if (language.getAllClasses(activeSourceFile).isEmpty())
        {
            showMessage("Please select a Java file that contains classes");
            return;
        }

        PsiFile sourceFile = activeSourceFile;
        modelService.loadBytecodeAsync(sourceFile, () ->
        {
            if (activeSourceFile != sourceFile)
            {
                return;
            }

            byteCodePanel.showSourceFile(sourceFile);
            assemblyPanel.showSourceFile(sourceFile);
            if (syncCaret && activeSourceEditor != null)
            {
                syncViewerToEditor(activeSourceEditor.getCaretModel().getLogicalPosition());
            }
        });
    }

    private void syncViewerToEditor(LogicalPosition caretPosition)
    {
        if (activeSourceFile == null || activeSourceEditor == null)
        {
            setCurrentMember(null);
            return;
        }
        int caretOffset = activeSourceEditor.logicalPositionToOffset(caretPosition);
        JitWatchLanguageSupport<PsiElement, PsiElement> languageSupport = LanguageSupport.forLanguage(activeSourceFile.getLanguage());
        if (languageSupport == null)
        {
            setCurrentMember(null);
            return;
        }
        PsiElement methodAtCaret = languageSupport.findMethodAtOffset(activeSourceFile, caretOffset);
        if (methodAtCaret == null)
        {
            setCurrentMember(null);
            return;
        }

        IMetaMember currentMember = modelService.getMetaMember(methodAtCaret);
        if (currentMember == null || currentMember.getMemberBytecode() == null)
        {
            setCurrentMember(null);
            return;
        }
        LineTable lineTable = currentMember.getMemberBytecode().getLineTable();
        LineTableEntry lineTableEntry = lineTable.getEntryForSourceLine(caretPosition.line + 1);
        if (lineTableEntry == null)
        {
            setCurrentMember(null);
            return;
        }

        setCurrentMember(currentMember);

        lastEditorBytecodeOffset = lineTableEntry.getBytecodeOffset();
        lastEditorCaretLinePosition = caretPosition.line;

        byteCodePanel.navigateToMemberBcOffsetOrLine(currentMember, lineTableEntry.getBytecodeOffset(), caretPosition.line + 1);
        assemblyPanel.navigateToMemberBcOffsetOrLine(currentMember, lineTableEntry.getBytecodeOffset(), caretPosition.line + 1);
    }

    public void navigateToMember(PsiElement member)
    {
        IMetaMember currentMember = modelService.getMetaMember(member);
        setCurrentMember(currentMember);
        if (currentMember != null)
        {
            PsiFile psiFile = member.getContainingFile();
            int lineNumber = 1;
            if (psiFile != null)
            {
                Document document = PsiDocumentManager.getInstance(psiFile.getProject()).getDocument(psiFile);
                if (document != null)
                {
                    int offset = member.getTextOffset();
                    lineNumber = document.getLineNumber(offset);
                }
            }

            byteCodePanel.navigateToMemberLine(currentMember, lineNumber);
            assemblyPanel.navigateToMemberLine(currentMember, lineNumber);
        }
    }
}
