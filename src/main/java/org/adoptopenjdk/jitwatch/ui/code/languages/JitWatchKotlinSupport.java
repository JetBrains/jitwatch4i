package org.adoptopenjdk.jitwatch.ui.code.languages;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.ProjectScope;
import com.intellij.psi.util.PsiTreeUtil;
import org.adoptopenjdk.jitwatch.model.IMetaMember;
import org.adoptopenjdk.jitwatch.model.MetaClass;
import org.jetbrains.kotlin.caches.resolve.KotlinCacheService;
import org.jetbrains.kotlin.descriptors.CallableDescriptor;
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor;
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor;
import org.jetbrains.kotlin.idea.stubindex.KotlinFullClassNameIndex;
import org.jetbrains.kotlin.psi.KtCallableDeclaration;
import org.jetbrains.kotlin.psi.KtClassOrObject;
import org.jetbrains.kotlin.psi.KtNamedFunction;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode;
import org.jetbrains.kotlin.types.KotlinType;

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
                Collection<KtClassOrObject> ktClasses = PsiTreeUtil.collectElementsOfType(file, KtClassOrObject.class);
                return new ArrayList<>(ktClasses);
            }
        });
    }

    @Override
    public KtClassOrObject findClass(Project project, MetaClass metaClass)
    {
        Collection<KtClassOrObject> classes = KotlinFullClassNameIndex.getInstance()
                .get(metaClass.getFullyQualifiedName(), project, ProjectScope.getAllScope(project));

        return classes.isEmpty() ? null : classes.iterator().next();
    }

    @Override
    public List<KtCallableDeclaration> getAllMethods(KtClassOrObject cls)
    {
        List<KtCallableDeclaration> methods = new ArrayList<>();
        methods.addAll(cls.getDeclarations().stream()
                .filter(declaration -> declaration instanceof KtCallableDeclaration)
                .map(declaration -> (KtNamedFunction) declaration)
                .toList());
        return methods;
    }

    @Override
    public KtCallableDeclaration findMethodAtOffset(PsiFile file, int offset)
    {
        return PsiTreeUtil.getParentOfType(file.findElementAt(offset), KtCallableDeclaration.class);
    }

    @Override
    public String getClassVMName(KtClassOrObject cls) {
        String fqName = cls.getFqName().asString();
        return fqName;
    }

    @Override
    public KtClassOrObject getContainingClass(KtCallableDeclaration method)
    {
        KtClassOrObject parentOfType = PsiTreeUtil.getParentOfType(method, KtClassOrObject.class);
        return parentOfType;
    }

    @Override
    public boolean matchesSignature(KtCallableDeclaration method, String memberName, List<String> paramTypeNames, String returnTypeName)
    {
        BindingContext bindingContext = KotlinCacheService.Companion.getInstance(method.getProject())
                .getResolutionFacade(method.getContainingKtFile())
                .analyze(method, BodyResolveMode.PARTIAL);

        if (bindingContext == null)
        {
            return false;
        }

        CallableDescriptor callableDescriptor = (CallableDescriptor)
                bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, method);

        String name = callableDescriptor.getName().asString();

        if (!memberName.equals(name))
        {
            return false;
        }

        if (callableDescriptor == null)
        {
            return false;
        }

        List<ValueParameterDescriptor> valueParameters = callableDescriptor.getValueParameters();
        if (paramTypeNames.size() != valueParameters.size())
        {
            return false;
        }

        for (int i = 0; i < paramTypeNames.size(); i++)
        {
            KotlinType paramType = valueParameters.get(i).getType();
            String normalizedParamFqName = normalizeTypeName(paramType);
            if (normalizedParamFqName == null)
            {
                return false;
            }
            if (!paramTypeNames.get(i).equals(normalizedParamFqName))
            {
                return false;
            }
        }

        String normalizedReturnFqName = normalizeTypeName(callableDescriptor.getReturnType());

        return returnTypeName.equals(normalizedReturnFqName);
    }

    private String normalizeTypeName(KotlinType kotlinType)
    {
        if (kotlinType == null)
        {
            return Void.class.getName();
        }

        ClassifierDescriptor classifierDescriptor = kotlinType.getConstructor().getDeclarationDescriptor();
        if (classifierDescriptor == null)
        {
            return null;
        }

        String fqName = DescriptorUtils.getFqName(classifierDescriptor).toSafe().asString();
        boolean isNullable = kotlinType.isMarkedNullable();

        switch (fqName)
        {
            case "kotlin.String":
                return "java.lang.String";
            case "kotlin.Boolean":
                return isNullable ? "java.lang.Boolean" : "boolean";
            case "kotlin.Char":
                return isNullable ? "java.lang.Character" : "char";
            case "kotlin.Byte":
                return isNullable ? "java.lang.Byte" : "byte";
            case "kotlin.Short":
                return isNullable ? "java.lang.Short" : "short";
            case "kotlin.Int":
                return isNullable ? "java.lang.Integer" : "int";
            case "kotlin.Long":
                return isNullable ? "java.lang.Long" : "long";
            case "kotlin.Float":
                return isNullable ? "java.lang.Float" : "float";
            case "kotlin.Double":
                return isNullable ? "java.lang.Double" : "double";
            default:
                return fqName;
        }
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
