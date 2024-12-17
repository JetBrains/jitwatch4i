package org.adoptopenjdk.jitwatch.ui.code.languages;

import com.intellij.debugger.engine.JVMNameUtil;
import com.intellij.lang.java.JavaLanguage;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.*;
import com.intellij.psi.search.ProjectScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.TypeConversionUtil;
import org.adoptopenjdk.jitwatch.model.MetaClass;
import org.adoptopenjdk.jitwatch.ui.code.JavaTypeUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class JitWatchJavaSupport implements JitWatchLanguageSupport<PsiClass, PsiMethod>
{
    @Override
    public List<PsiClass> getAllClasses(PsiFile file)
    {
        return ApplicationManager.getApplication().runReadAction(new Computable<List<PsiClass>>()
        {
            @Override
            public List<PsiClass> compute()
            {
                Collection<PsiClass> psiClasses = PsiTreeUtil.collectElementsOfType(file, PsiClass.class);
                return new ArrayList<>(psiClasses);
            }
        });
    }

    @Override
    public PsiClass findClass(Project project, MetaClass metaClass)
    {
        PsiClass psiClass = JavaPsiFacade.getInstance(project).findClass(
                metaClass.getFullyQualifiedName(), ProjectScope.getAllScope(project));
        if (psiClass != null && psiClass.getLanguage() == JavaLanguage.INSTANCE)
        {
            return psiClass;
        }
        return null;
    }

    @Override
    public List<PsiMethod> getAllMethods(PsiClass cls)
    {
        List<PsiMethod> methods = new ArrayList<>();
        methods.addAll(Arrays.asList(cls.getMethods()));
        methods.addAll(Arrays.asList(cls.getConstructors()));
        return methods;
    }

    @Override
    public PsiMethod findMethodAtOffset(PsiFile file, int offset)
    {
        return PsiTreeUtil.getParentOfType(file.findElementAt(offset), PsiMethod.class);
    }

    @Override
    public String getClassVMName(PsiClass cls)
    {
        return JVMNameUtil.getClassVMName(cls);
    }

    @Override
    public PsiClass getContainingClass(PsiMethod method)
    {
        return method.getContainingClass();
    }

    @Override
    public boolean matchesSignature(PsiMethod method, String memberName, List<String> paramTypeNames, String returnTypeName)
    {
        String psiMethodName;
        if (method.isConstructor())
        {
            String classVMName = JVMNameUtil.getClassVMName(method.getContainingClass());
            psiMethodName = (classVMName != null && classVMName.contains("."))
                    ? classVMName.substring(classVMName.lastIndexOf('.') + 1)
                    : method.getName();
        }
        else
        {
            psiMethodName = method.getName();
        }

        if (!memberName.equals(psiMethodName))
        {
            return false;
        }

        if (paramTypeNames.size() != method.getParameterList().getParametersCount())
        {
            return false;
        }

        List<String> methodParamTypes = new ArrayList<>();
        for (PsiParameter parameter : method.getParameterList().getParameters())
        {
            methodParamTypes.add(jvmText(parameter.getType()));
        }

        for (int i = 0; i < paramTypeNames.size(); i++)
        {
            String methodParamType = methodParamTypes.get(i);
            if (!paramTypeNames.get(i).equals(methodParamType))
            {
                methodParamType = JavaTypeUtils.normalizeTypeName(methodParamType);
                if (!paramTypeNames.get(i).equals(methodParamType))
                {
                    return false;
                }
            }
        }

        String psiMethodReturnTypeName = method.isConstructor() ? "void" : jvmText(method.getReturnType());

        if (!returnTypeName.equals(psiMethodReturnTypeName))
        {
            psiMethodReturnTypeName = JavaTypeUtils.normalizeTypeName(psiMethodReturnTypeName);
        }
        return returnTypeName.equals(psiMethodReturnTypeName);
    }

    private String jvmText(PsiType psiType)
    {
        PsiType erasedType = TypeConversionUtil.erasure(psiType);
        if (erasedType instanceof PsiClassType)
        {
            PsiClass psiClass = ((PsiClassType) erasedType).resolve();
            if (psiClass != null)
            {
                String vmName = JVMNameUtil.getClassVMName(psiClass);
                if (vmName != null)
                {
                    return vmName;
                }
            }
        }
        return erasedType.getCanonicalText();
    }
}
