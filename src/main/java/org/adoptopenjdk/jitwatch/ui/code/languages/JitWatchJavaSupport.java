package org.adoptopenjdk.jitwatch.ui.code.languages;

import com.intellij.debugger.engine.JVMNameUtil;
import com.intellij.lang.ASTNode;
import com.intellij.lang.java.JavaLanguage;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.psi.search.ProjectScope;
import com.intellij.psi.util.ClassUtil;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.TypeConversionUtil;
import org.adoptopenjdk.jitwatch.model.MemberSignatureParts;
import org.adoptopenjdk.jitwatch.model.MetaClass;

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
    public boolean isMethod(PsiElement element)
    {
        return element instanceof PsiMethod;
    }

    @Override
    public PsiMethod findMethodAtOffset(PsiFile file, int offset)
    {
        return PsiTreeUtil.getParentOfType(file.findElementAt(offset), PsiMethod.class);
    }

    @Override
    public TextRange getNameRange(PsiElement element)
    {
        if (element instanceof PsiNameIdentifierOwner)
        {
            PsiElement nameIdentifier = ((PsiNameIdentifierOwner) element).getNameIdentifier();
            if (nameIdentifier != null)
            {
                return nameIdentifier.getTextRange();
            }
            else
            {
                return element.getTextRange();
            }
        }
        else
        {
            return element.getTextRange();
        }
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
            if (!paramTypeNames.get(i).equals(methodParamTypes.get(i)))
            {
                return false;
            }
        }

        String psiMethodReturnTypeName = method.isConstructor() ? "void" : jvmText(method.getReturnType());
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

    @Override
    public PsiElement findCallToMember(PsiFile file, int offset, MemberSignatureParts calleeMember, int sameLineCallIndex)
    {
        PsiStatement statement = findStatement(file, offset);
        if (statement == null)
        {
            return null;
        }
        final PsiElement[] result = {null};
        final int[] curIndex = {0};
        statement.acceptChildren(new JavaRecursiveElementVisitor()
        {
            @Override
            public void visitCallExpression(PsiCallExpression callExpression)
            {
                super.visitCallExpression(callExpression);
                PsiMethod method = callExpression.resolveMethod();
                if (method != null && matchesSignature(method, calleeMember))
                {
                    if (curIndex[0] == sameLineCallIndex)
                    {
                        if (callExpression instanceof PsiMethodCallExpression)
                        {
                            result[0] = ((PsiMethodCallExpression) callExpression).getMethodExpression();
                        }
                        else if (callExpression instanceof PsiNewExpression)
                        {
                            result[0] = ((PsiNewExpression) callExpression).getClassReference();
                        }
                        else
                        {
                            result[0] = callExpression;
                        }
                    }
                    curIndex[0]++;
                }
            }
        });
        return result[0];
    }

    @Override
    public PsiElement findAllocation(PsiFile file, int offset, String jvmName)
    {
        PsiClass expectedClass = ClassUtil.findPsiClassByJVMName(file.getManager(), jvmName);
        if (expectedClass == null)
        {
            return null;
        }
        PsiStatement statement = findStatement(file, offset);
        if (statement == null)
        {
            return null;
        }
        final PsiElement[] result = {null};
        statement.acceptChildren(new JavaRecursiveElementVisitor()
        {
            @Override
            public void visitNewExpression(PsiNewExpression expression)
            {
                super.visitNewExpression(expression);
                PsiMethod constructor = expression.resolveConstructor();
                PsiClass createdClass = constructor != null ? constructor.getContainingClass() : null;
                if (createdClass != null && createdClass.isEquivalentTo(expectedClass))
                {
                    ASTNode newKeywordNode = expression.getNode().findChildByType(JavaTokenType.NEW_KEYWORD);
                    result[0] = newKeywordNode != null ? newKeywordNode.getPsi() : expression;
                }
            }
        });
        return result[0];
    }

    private PsiStatement findStatement(PsiFile file, int offset)
    {
        return PsiTreeUtil.getParentOfType(file.findElementAt(offset), PsiStatement.class);
    }

    private boolean matchesSignature(PsiMethod method, MemberSignatureParts calleeMember)
    {
        return matchesSignature(method, calleeMember.getMemberName(), calleeMember.getParamTypes(), calleeMember.getReturnType());
    }
}
