package org.adoptopenjdk.jitwatch.ui.code;

public class JavaTypeUtils
{
    public static String normalizeTypeName(String javaTypeName)
    {
        if (javaTypeName.contains("$"))
        {
            javaTypeName = javaTypeName.replaceAll("\\$", ".");
        }
        return javaTypeName;
    }
}
