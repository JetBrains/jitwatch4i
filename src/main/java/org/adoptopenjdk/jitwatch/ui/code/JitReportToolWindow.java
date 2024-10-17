package org.adoptopenjdk.jitwatch.ui.code;

import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.psi.NavigatablePsiElement;
import com.intellij.psi.PsiElement;
import com.intellij.ui.ColoredTableCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.TableView;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.ListTableModel;
import org.adoptopenjdk.jitwatch.model.IMetaMember;
import org.adoptopenjdk.jitwatch.model.bytecode.MemberBytecode;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Comparator;
import java.util.List;

public class JitReportToolWindow extends JPanel
{
    private final Project project;
    private final JitWatchModelService modelService;
    private final TableView<InlineFailureGroup> reportTable;
    private final ListTableModel<InlineFailureGroup> reportTableModel;

    public static final String ID = "JITWatch Report";

    public JitReportToolWindow(Project project)
    {
        super(new BorderLayout());
        this.project = project;
        this.modelService = JitWatchModelService.getInstance(project);
        this.reportTable = new TableView<>();

        this.reportTableModel = new ListTableModel<>(
                new ColumnInfo[]{
                        CalleeColumnInfo,
                        CalleeSizeColumnInfo,
                        CalleeCountColumnInfo,
                        CallSiteColumnInfo,
                        ReasonColumnInfo
                }
        );
        this.reportTableModel.setSortable(true);

        reportTable.setModelAndUpdateColumns(reportTableModel);
        add(new JBScrollPane(reportTable), BorderLayout.CENTER);

        updateData();

        reportTable.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                int columnAtPoint = reportTable.columnAtPoint(e.getPoint());
                int rowAtPoint = reportTable.rowAtPoint(e.getPoint());
                if (columnAtPoint < 0 || rowAtPoint < 0) return;

                int col = reportTable.convertColumnIndexToModel(columnAtPoint);
                int row = reportTable.convertRowIndexToModel(rowAtPoint);
                InlineFailureGroup failureGroup = reportTableModel.getItem(row);
                if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 1)
                {
                    switch (col)
                    {
                        case 0:
                            navigateToMember(failureGroup.getCallee(), null);
                            break;
                        case 3:
                            showCallSitesPopup(e, failureGroup.getCallSites());
                            break;
                        default:
                            break;
                    }
                }
            }
        });

        modelService.addUpdateListener(this::updateData);
    }

    private void updateData()
    {
        reportTableModel.setItems(modelService.getInlineFailureGroups());
    }

    private void navigateToMember(IMetaMember member, Integer bci)
    {
        PsiElement psiMethod = JitWatchModelService.getInstance(project).getPsiMember(member);
        if (psiMethod == null)
        {
            return;
        }
        MemberBytecode memberBC = member.getMemberBytecode();
        if (memberBC != null && bci != null)
        {
            int sourceLine = memberBC.getLineTable().findSourceLineForBytecodeOffset(bci);
            if (sourceLine != -1)
            {
                new OpenFileDescriptor(project, psiMethod.getContainingFile().getVirtualFile(), sourceLine - 1, 0).navigate(true);
                return;
            }
        }
        if (psiMethod instanceof NavigatablePsiElement)
        {
            ((NavigatablePsiElement) psiMethod).navigate(true);
        }
    }

    private void showCallSitesPopup(MouseEvent e, List<InlineCallSite> callSites)
    {
        BaseListPopupStep<InlineCallSite> popupStep = new BaseListPopupStep<InlineCallSite>("Call Sites", callSites)
        {
            @Override
            public String getTextFor(InlineCallSite value)
            {
                IMetaMember member = value.getMember();
                String className = (member.getMetaClass() != null) ? member.getMetaClass().getName() : "<unknown>";
                return className + "." + member.getMemberName();
            }

            @Override
            public PopupStep<?> onChosen(InlineCallSite selectedValue, boolean finalChoice)
            {
                if (selectedValue != null)
                {
                    navigateToMember(selectedValue.getMember(), selectedValue.getBci());
                }
                return PopupStep.FINAL_CHOICE;
            }
        };
        JBPopupFactory.getInstance().createListPopup(popupStep).show(RelativePoint.fromScreen(e.getLocationOnScreen()));
    }

    public static final ColumnInfo<InlineFailureGroup, String> CalleeColumnInfo = new ColumnInfo<InlineFailureGroup, String>("Callee")
    {
        @Override
        public String valueOf(InlineFailureGroup item)
        {
            return item.getCallee().getFullyQualifiedMemberName();
        }

        @Override
        public Comparator<InlineFailureGroup> getComparator()
        {
            return Comparator.comparing(group -> group.getCallee().getFullyQualifiedMemberName());
        }

        @Override
        public TableCellRenderer getRenderer(InlineFailureGroup item)
        {
            return LinkRenderer;
        }
    };

    public static final ColumnInfo<InlineFailureGroup, Integer> CalleeSizeColumnInfo = new ColumnInfo<InlineFailureGroup, Integer>("Callee Size")
    {
        @Override
        public Integer valueOf(InlineFailureGroup item)
        {
            return item.getCalleeSize();
        }

        @Override
        public Comparator<InlineFailureGroup> getComparator()
        {
            return Comparator.comparingInt(InlineFailureGroup::getCalleeSize);
        }
    };

    public static final ColumnInfo<InlineFailureGroup, Integer> CalleeCountColumnInfo = new ColumnInfo<InlineFailureGroup, Integer>("Invocations")
    {
        @Override
        public Integer valueOf(InlineFailureGroup item)
        {
            return item.getCalleeInvocationCount();
        }

        @Override
        public Comparator<InlineFailureGroup> getComparator()
        {
            return Comparator.comparingInt(InlineFailureGroup::getCalleeInvocationCount);
        }
    };

    public static final ColumnInfo<InlineFailureGroup, Integer> CallSiteColumnInfo = new ColumnInfo<InlineFailureGroup, Integer>("Call Sites")
    {
        @Override
        public Integer valueOf(InlineFailureGroup item)
        {
            return item.getCallSites().size();
        }

        @Override
        public Comparator<InlineFailureGroup> getComparator()
        {
            return Comparator.comparingInt(group -> group.getCallSites().size());
        }

        @Override
        public TableCellRenderer getRenderer(InlineFailureGroup item)
        {
            return LinkRenderer;
        }
    };

    public static final ColumnInfo<InlineFailureGroup, String> ReasonColumnInfo = new ColumnInfo<InlineFailureGroup, String>("Reason")
    {
        @Override
        public String valueOf(InlineFailureGroup item)
        {
            return String.join(", ", item.getReasons());
        }
    };

    public static final ColoredTableCellRenderer LinkRenderer = new ColoredTableCellRenderer()
    {
        @Override
        protected void customizeCellRenderer(JTable table, Object value, boolean selected, boolean hasFocus, int row, int column)
        {
            if (value != null)
            {
                append(value.toString(), SimpleTextAttributes.LINK_ATTRIBUTES);
            }
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
    };
}
