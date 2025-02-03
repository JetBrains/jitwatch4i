package org.adoptopenjdk.jitwatch.ui.code;

import com.intellij.execution.CommonJavaRunConfigurationParameters;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.RunConfigurationExtension;
import com.intellij.execution.configurations.*;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.util.io.FileUtil;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;

public class JitRunConfigurationExtension extends RunConfigurationExtension
{
    private static final Logger logger = Logger.getInstance(JitRunConfigurationExtension.class);

    private static final String JITWATCH_ENABLED_ATTRIBUTE = "jitwatch-enabled";

    @Override
    public <P extends RunConfigurationBase<?>> @NotNull SettingsEditor<P> createEditor(@NotNull P configuration)
    {
        return new JitRunConfigurationEditor<>();
    }

    @Override
    public String getEditorTitle()
    {
        return "JITWatch";
    }

    @Override
    public void patchCommandLine(@NotNull RunConfigurationBase<?> configuration,
                                 RunnerSettings runnerSettings,
                                 @NotNull GeneralCommandLine cmdLine,
                                 @NotNull String runnerId,
                                 @NotNull Executor executor) throws ExecutionException
    {
        super.patchCommandLine(configuration, runnerSettings, cmdLine, runnerId, executor);
    }

    @Override
    public boolean isEnabledFor(@NotNull RunConfigurationBase applicableConfiguration, @Nullable RunnerSettings runnerSettings)
    {
        return super.isEnabledFor(applicableConfiguration, runnerSettings);
    }

    @Override
    public boolean isApplicableFor(@NotNull RunConfigurationBase<?> configuration)
    {
        return configuration instanceof CommonJavaRunConfigurationParameters;
    }

    @Override
    public void readExternal(@NotNull RunConfigurationBase<?> runConfiguration, @NotNull Element element)
    {
        JitWatchSettings settings = JitWatchSettings.Companion.getOrCreate(runConfiguration);
        settings.setEnabled("true".equals(element.getAttributeValue(JITWATCH_ENABLED_ATTRIBUTE)));
    }

    @Override
    public void writeExternal(@NotNull RunConfigurationBase<?> runConfiguration, @NotNull Element element)
    {
        JitWatchSettings settings = JitWatchSettings.Companion.getOrCreate(runConfiguration);
        if (settings.isEnabled())
        {
            element.setAttribute(JITWATCH_ENABLED_ATTRIBUTE, "true");
        }
    }

    @Override
    public <T extends RunConfigurationBase<?>> void updateJavaParameters(@NotNull T configuration,
                                                                         @NotNull JavaParameters params,
                                                                         RunnerSettings runnerSettings)
    {
        JitWatchSettings settings = JitWatchSettings.Companion.getOrCreate(configuration);
        if (settings.isEnabled())
        {
            File logPath = null;
            try
            {
                logPath = FileUtil.createTempFile("jitwatch", ".log");
            }
            catch (IOException e)
            {
                logger.error("Cannot create compilation temp file!", e);
            }
            ParametersList vmOptions = params.getVMParametersList();
            vmOptions.add("-XX:+UnlockDiagnosticVMOptions");
            vmOptions.add("-XX:+LogCompilation");
            vmOptions.add("-XX:+PrintAssembly");
            vmOptions.add("-XX:LogFile=" + logPath.getAbsolutePath());
            settings.setLastLogPath(logPath);
        }
    }

    @Override
    public void attachToProcess(@NotNull RunConfigurationBase<?> configuration, @NotNull ProcessHandler handler, RunnerSettings runnerSettings)
    {
//        File logPath = JitWatchSettings.Companion.getOrCreate(configuration).getLastLogPath();
//        if (logPath != null)
//        {
//            handler.addProcessListener(new ProcessAdapter()
//            {
//                @Override
//                public void processTerminated(@NotNull ProcessEvent event)
//                {
//                    ApplicationManager.getApplication().invokeLater(() ->
//                    {
//                        JitWatchUtil.loadLogAndShowUI(configuration.getProject(), logPath);
//                    });
//                }
//            });
//        }
    }
}
