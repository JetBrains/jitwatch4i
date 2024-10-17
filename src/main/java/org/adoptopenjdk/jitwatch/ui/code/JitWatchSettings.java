package org.adoptopenjdk.jitwatch.ui.code;

import com.intellij.execution.configurations.RunConfigurationBase;
import com.intellij.openapi.util.Key;

import java.io.File;

public class JitWatchSettings
{
    private boolean enabled = false;
    private File lastLogPath = null;

    private static final Key<JitWatchSettings> KEY = Key.create("ru.yole.jitwatch.settings");

    public JitWatchSettings()
    {
    }

    public JitWatchSettings(boolean enabled, File lastLogPath)
    {
        this.enabled = enabled;
        this.lastLogPath = lastLogPath;
    }

    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    public File getLastLogPath()
    {
        return lastLogPath;
    }

    public void setLastLogPath(File lastLogPath)
    {
        this.lastLogPath = lastLogPath;
    }

    public static class Companion
    {
        public static JitWatchSettings getOrCreate(RunConfigurationBase<?> configuration)
        {
            JitWatchSettings settings = configuration.getUserData(KEY);
            if (settings == null)
            {
                settings = new JitWatchSettings();
                configuration.putUserData(KEY, settings);
            }
            return settings;
        }

        public static void clear(RunConfigurationBase<?> configuration)
        {
            configuration.putUserData(KEY, null);
        }
    }
}
