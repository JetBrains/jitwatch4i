package org.adoptopenjdk.jitwatch.ui.code;

import com.intellij.execution.configurations.RunConfigurationBase;
import com.intellij.openapi.options.SettingsEditor;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

public class JitRunConfigurationEditor<T extends RunConfigurationBase<?>> extends SettingsEditor<T>
{
    private final JPanel editorPanel;
    private final JCheckBox enabledCheckbox;

    public JitRunConfigurationEditor()
    {
        editorPanel = new JPanel(new BorderLayout());
        enabledCheckbox = new JCheckBox("Log compilation");
        editorPanel.add(enabledCheckbox, BorderLayout.NORTH);
    }

    @Override
    public void applyEditorTo(@NotNull T s)
    {
        JitWatchSettings settings = JitWatchSettings.Companion.getOrCreate(s);
        settings.setEnabled(enabledCheckbox.isSelected());
    }

    @Override
    public void resetEditorFrom(@NotNull T s)
    {
        JitWatchSettings settings = JitWatchSettings.Companion.getOrCreate(s);
        enabledCheckbox.setSelected(settings.isEnabled());
    }

    @Override
    public @NotNull JComponent createEditor()
    {
        return editorPanel;
    }
}
