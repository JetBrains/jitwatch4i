package org.adoptopenjdk.jitwatch.ui.code.languages;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.ProjectScope;
import com.intellij.psi.util.PsiTreeUtil;
import org.adoptopenjdk.jitwatch.model.IMetaMember;
import org.adoptopenjdk.jitwatch.model.MetaClass;
import org.jetbrains.kotlin.idea.stubindex.KotlinFullClassNameIndex;
import org.jetbrains.kotlin.psi.KtCallableDeclaration;
import org.jetbrains.kotlin.psi.KtClassOrObject;
import org.jetbrains.kotlin.psi.KtConstructor;
import org.jetbrains.kotlin.psi.KtParameter;
import org.jetbrains.kotlin.psi.KtTypeReference;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class JitWatchKotlinSupport implements JitWatchLanguageSupport<KtClassOrObject, KtCallableDeclaration>
{
    public static final String ACCESS_PREFIX = "access$";

    @Override
    public List<KtClassOrObject> getAllClasses(PsiFile file)
    {
        return ApplicationManager.getApplication().runReadAction(new Computable<List<KtClassOrObject>>()
        {
            @Override
            public List<KtClassOrObject> compute()
            {
                Collection<KtClassOrObject> ktClasses = ReadAction.compute(() -> PsiTreeUtil.collectElementsOfType(file, KtClassOrObject.class));
                return new ArrayList<>(ktClasses);
            }
        });
    }

    @Override
    public KtClassOrObject findClass(Project project, MetaClass metaClass)
    {
       return ReadAction.compute(() -> doFindClass(project, metaClass));
    }
    private KtClassOrObject doFindClass(Project project, MetaClass metaClass)
    {
        Collection<KtClassOrObject> classes =
                KotlinFullClassNameIndex.Helper.get(metaClass.getFullyQualifiedName(), project, ProjectScope.getAllScope(project));

        return classes.isEmpty() ? null : classes.iterator().next();
    }

    @Override
    public List<KtCallableDeclaration> getAllMethods(KtClassOrObject cls)
    {
        return ReadAction.compute(() -> {
            List<KtCallableDeclaration> methods = new ArrayList<>();
            methods.addAll(cls.getDeclarations().stream()
                    .filter(declaration -> declaration instanceof KtCallableDeclaration)
                    .map(declaration -> (KtCallableDeclaration) declaration)
                    .toList());
            return methods;
        });
    }

    @Override
    public KtCallableDeclaration findMethodAtOffset(PsiFile file, int offset)
    {
        return ReadAction.compute(() -> PsiTreeUtil.getParentOfType(file.findElementAt(offset), KtCallableDeclaration.class));
    }

    @Override
    public String getClassVMName(KtClassOrObject cls) {
        return ReadAction.compute(() -> {
            if (cls.getFqName() == null)
            {
                return null;
            }
            return cls.getFqName().asString();
        });
    }

    @Override
    public KtClassOrObject getContainingClass(KtCallableDeclaration method)
    {
        KtClassOrObject parentOfType = ReadAction.compute(() -> PsiTreeUtil.getParentOfType(method, KtClassOrObject.class));
        return parentOfType;
    }

    @Override
    public boolean matchesSignature(KtCallableDeclaration method, String memberName, List<String> paramTypeNames, String returnTypeName)
    {
       return ReadAction.compute(() -> doMatchesSignature(method, memberName, paramTypeNames, returnTypeName));
    }

    private boolean doMatchesSignature(KtCallableDeclaration method, String memberName, List<String> paramTypeNames, String returnTypeName)
    {
        String name = getCallableName(method);

        if (!memberName.equals(name))
        {
            return false;
        }

        List<KtParameter> valueParameters = method.getValueParameters();
        if (paramTypeNames.size() != valueParameters.size())
        {
            return false;
        }

        for (int i = 0; i < paramTypeNames.size(); i++)
        {
            String normalizedParamFqName = normalizeTypeName(valueParameters.get(i).getTypeReference());
            if (normalizedParamFqName == null)
            {
                return false;
            }
            if (!paramTypeNames.get(i).equals(normalizedParamFqName))
            {
                return false;
            }
        }

        String normalizedReturnFqName = method instanceof KtConstructor ? Void.TYPE.getName() : normalizeTypeName(method.getTypeReference());

        return returnTypeName.equals(normalizedReturnFqName);
    }

    private String getCallableName(KtCallableDeclaration method)
    {
        if (method instanceof KtConstructor)
        {
            KtClassOrObject containingClass = getContainingClass(method);
            return containingClass == null ? null : containingClass.getName();
        }

        return method.getName();
    }

    private String normalizeTypeName(KtTypeReference typeReference)
    {
        if (typeReference == null)
        {
            return Void.TYPE.getName();
        }

        String typeName = typeReference.getText();
        boolean isNullable = typeName.endsWith("?");
        typeName = typeName.replace("?", "");
        typeName = stripGenericArguments(typeName);

        switch (typeName)
        {
            case "String":
            case "kotlin.String":
                return "java.lang.String";
            case "Boolean":
            case "kotlin.Boolean":
                return isNullable ? "java.lang.Boolean" : "boolean";
            case "Char":
            case "kotlin.Char":
                return isNullable ? "java.lang.Character" : "char";
            case "Byte":
            case "kotlin.Byte":
                return isNullable ? "java.lang.Byte" : "byte";
            case "Short":
            case "kotlin.Short":
                return isNullable ? "java.lang.Short" : "short";
            case "Int":
            case "kotlin.Int":
                return isNullable ? "java.lang.Integer" : "int";
            case "Long":
            case "kotlin.Long":
                return isNullable ? "java.lang.Long" : "long";
            case "Float":
            case "kotlin.Float":
                return isNullable ? "java.lang.Float" : "float";
            case "Double":
            case "kotlin.Double":
                return isNullable ? "java.lang.Double" : "double";
            case "Unit":
            case "kotlin.Unit":
                return Void.TYPE.getName();
            default:
                return typeName;
        }
    }

    private String stripGenericArguments(String typeName)
    {
        int genericStart = typeName.indexOf('<');
        return genericStart == -1 ? typeName : typeName.substring(0, genericStart);
    }

    @Override
    public PsiElement findMemberElement(Project project, PsiClass psiClass, IMetaMember member)
    {
        String memberName = member.getMemberName();
        if (memberName == null || memberName.isEmpty())
        {
            return null;
        }

        if (memberName.startsWith(ACCESS_PREFIX))
        {
            memberName = memberName.substring(ACCESS_PREFIX.length());
        }

        return JitWatchLanguageSupportUtil.findJavaMemberElement(project, psiClass, memberName, member);
    }

}
