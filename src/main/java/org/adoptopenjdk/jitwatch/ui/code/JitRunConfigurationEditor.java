package org.adoptopenjdk.jitwatch.ui.code;

import com.intellij.execution.configurations.RunConfigurationBase;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

import javax.swing.*;
import java.awt.*;

public class JitRunConfigurationEditor<T extends RunConfigurationBase<?>> extends SettingsEditor<T>
{
    private final JPanel editorPanel;
    private final JCheckBox enabledCheckbox;
    private final JTextField logPatternField;

    public JitRunConfigurationEditor()
    {
        enabledCheckbox = new JCheckBox("Log compilation");

        logPatternField = new JTextField(JitWatchSettings.DEFAULT_LOG_FILE_PATTERN);
        logPatternField.setColumns(50);

        JLabel helpLabel = new JLabel(
                "Placeholders: ${project.basedir}, ${java.io.tmpdir}, ${TIME_STAMP}"
        );

        JPanel formPanel = FormBuilder.createFormBuilder()
                .addComponent(enabledCheckbox)
                .addLabeledComponent("Log file pattern:", logPatternField, 1, false)
                .addComponent(helpLabel)
                .getPanel();

        editorPanel = new JPanel(new BorderLayout());
        editorPanel.add(formPanel, BorderLayout.NORTH);

        enabledCheckbox.addItemListener(e ->
                logPatternField.setEnabled(enabledCheckbox.isSelected())
        );

        logPatternField.setEnabled(false);
    }

    @Override
    public void applyEditorTo(@NotNull T configuration)
    {
        JitWatchSettings settings = JitWatchSettings.Companion.getOrCreate(configuration);
        settings.setEnabled(enabledCheckbox.isSelected());

        String pattern = logPatternField.getText().trim();
        if (pattern.isEmpty())
        {
            pattern = JitWatchSettings.DEFAULT_LOG_FILE_PATTERN;
        }
        settings.setLogFilePattern(pattern);
    }

    @Override
    public void resetEditorFrom(@NotNull T configuration)
    {
        JitWatchSettings settings = JitWatchSettings.Companion.getOrCreate(configuration);
        boolean enabled = settings.isEnabled();

        enabledCheckbox.setSelected(enabled);

        String pattern = settings.getLogFilePattern();
        if (pattern == null || pattern.isBlank())
        {
            pattern = JitWatchSettings.DEFAULT_LOG_FILE_PATTERN;
        }
        logPatternField.setText(pattern);

        logPatternField.setEnabled(enabled);
    }

    @Override
    public @NotNull JComponent createEditor()
    {
        return editorPanel;
    }
}
