package org.adoptopenjdk.jitwatch.ui.code;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.event.CaretEvent;
import com.intellij.openapi.editor.event.CaretListener;
import com.intellij.openapi.fileTypes.PlainTextFileType;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.adoptopenjdk.jitwatch.model.IMetaMember;

import javax.swing.*;
import java.awt.*;

public abstract class CodePanelBase extends JPanel implements Disposable
{
    private static final String MESSAGE_CARD = "Message";
    private static final String EDITOR_CARD = "Editor";

    private final Project project;

    private final JLabel messageLabel;
    private final CardLayout cardLayout;
    private final Editor viewerEditor;
    private final Document viewerDocument;
    private final JitWatchModelService modelService;
    private CodeToolWindowManager codeToolWindowManager;

    private boolean movingCaretInViewer = false;

    private JPanel contentPanel;

    public CodePanelBase(Project project)
    {
        super(new BorderLayout());

        this.project = project;

        messageLabel = new JLabel();

        viewerDocument = EditorFactory.getInstance().createDocument("");
        viewerEditor = EditorFactory.getInstance().createEditor(viewerDocument, project, PlainTextFileType.INSTANCE, true);
        viewerEditor.getSettings().setLineNumbersShown(false);
        viewerEditor.getSettings().setFoldingOutlineShown(false);
        viewerEditor.getSettings().setLineMarkerAreaShown(false);

        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);

        contentPanel.add(messageLabel, MESSAGE_CARD);
        contentPanel.add(viewerEditor.getComponent(), EDITOR_CARD);

        this.add(contentPanel, BorderLayout.CENTER);

        modelService = JitWatchModelService.getInstance(project);

        viewerEditor.getCaretModel().addCaretListener(new CaretListener()
        {
            @Override
            public void caretPositionChanged(CaretEvent e)
            {
                if (!movingCaretInViewer)
                {
                    getViewer().syncEditorToViewer(e.getNewPosition());
                }
            }
        });
    }

    protected abstract IViewer getViewer();

    public CodeToolWindowManager getCodeToolWindowManager()
    {
        return codeToolWindowManager;
    }

    public void setCodeToolWindowManager(CodeToolWindowManager codeToolWindowManager)
    {
        this.codeToolWindowManager = codeToolWindowManager;
    }

    public void setCurrentMember(IMetaMember member)
    {
        getViewer().setContentFromMember(member);
    }

    public void showSourceFile(PsiFile sourceFile)
    {
        cardLayout.show(contentPanel, EDITOR_CARD);
        getViewer().setContentFromPsiFile(sourceFile);
    }

    public JitWatchModelService getModelService()
    {
        return modelService;
    }

    public Project getProject()
    {
        return project;
    }

    public Editor getViewerEditor()
    {
        return viewerEditor;
    }

    public Document getViewerDocument()
    {
        return viewerDocument;
    }

    public void setMovingCaretInViewer(boolean value)
    {
        this.movingCaretInViewer = value;
    }

    public void showMessage(String message)
    {
        messageLabel.setText(message);
        cardLayout.show(contentPanel, MESSAGE_CARD);
    }

    private void moveCaretToLine(int line)
    {
        viewerEditor.getCaretModel().moveToLogicalPosition(new LogicalPosition(line, 0));
        viewerEditor.getScrollingModel().scrollToCaret(ScrollType.MAKE_VISIBLE);
    }

    public void moveSourceEditorCaretToLine(int line)
    {
        codeToolWindowManager.moveSourceEditorCaretToLine(line);
    }

    public void navigateToMemberBcOffsetOrLine(IMetaMember member, int bytecodeOffset, int lineNumber)
    {
        Integer viewerLine = getViewer().findLine(member, bytecodeOffset, lineNumber);
        if (viewerLine == null)
        {
            return;
        }

        movingCaretInViewer = true;
        try
        {
            moveCaretToLine(viewerLine);
        }
        finally
        {
            movingCaretInViewer = false;
        }
    }

    public void navigateToMemberLine(IMetaMember member, int lineNumber)
    {
        Integer viewerLine = getViewer().findLine(member, lineNumber);
        if (viewerLine == null)
        {
            return;
        }
        moveCaretToLine(viewerLine);
    }

    @Override
    public void dispose()
    {
        EditorFactory.getInstance().releaseEditor(viewerEditor);
    }
}
