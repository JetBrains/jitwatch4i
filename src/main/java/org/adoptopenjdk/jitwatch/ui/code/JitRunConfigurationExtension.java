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

public class JitRunConfigurationExtension extends RunConfigurationExtension
{
    private static final Logger logger = Logger.getInstance(JitRunConfigurationExtension.class);

    private static final String JITWATCH_ENABLED_ATTRIBUTE = "jitwatch-enabled";
    private static final String JITWATCH_LOG_FILE_PATTERN_ATTRIBUTE = "jitwatch-log-file-pattern";

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

        String pattern = element.getAttributeValue(JITWATCH_LOG_FILE_PATTERN_ATTRIBUTE);
        if (pattern == null || pattern.isBlank())
        {
            settings.setLogFilePattern(JitWatchSettings.DEFAULT_LOG_FILE_PATTERN);
        }
        else
        {
            settings.setLogFilePattern(pattern);
        }
    }

    @Override
    public void writeExternal(@NotNull RunConfigurationBase<?> runConfiguration, @NotNull Element element)
    {
        JitWatchSettings settings = JitWatchSettings.Companion.getOrCreate(runConfiguration);
        if (settings.isEnabled())
        {
            element.setAttribute(JITWATCH_ENABLED_ATTRIBUTE, "true");
        }
        String pattern = settings.getLogFilePattern();
        if (pattern != null && !pattern.isBlank())
        {
            element.setAttribute(JITWATCH_LOG_FILE_PATTERN_ATTRIBUTE, pattern);
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
            File logPath;
            String pattern = settings.getLogFilePattern();
            if (pattern == null || pattern.isBlank())
            {
                pattern = JitWatchSettings.DEFAULT_LOG_FILE_PATTERN;
            }

            String resolved = resolveLogPattern(pattern, configuration);
            logPath = new File(resolved);
            FileUtil.createParentDirs(logPath);

            if (logPath != null)
            {
                ParametersList vmOptions = params.getVMParametersList();
                vmOptions.add("-XX:+UnlockDiagnosticVMOptions");
                vmOptions.add("-XX:+LogCompilation");
                vmOptions.add("-XX:+PrintAssembly");
                vmOptions.add("-XX:LogFile=" + logPath.getAbsolutePath());
                settings.setLastLogPath(logPath);
            }
        }
    }

    private String resolveLogPattern(String pattern, RunConfigurationBase<?> configuration)
    {
        String projectBaseDir = "";
        if (configuration.getProject().getBasePath() != null)
        {
            projectBaseDir = configuration.getProject().getBasePath();
        }

        String timeStamp = new java.text.SimpleDateFormat("yyyy-MM-dd_HHmmss")
                .format(new java.util.Date());

        String result = pattern;
        result = result.replace("${project.basedir}", projectBaseDir);
        result = result.replace("${java.io.tmpdir}", System.getProperty("java.io.tmpdir"));
        result = result.replace("${TIME_STAMP}", timeStamp);

        return result;
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
