package org.adoptopenjdk.jitwatch.ui.code;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.JavaSdk;
import com.intellij.openapi.roots.CompilerModuleExtension;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.containers.ContainerUtil;
import org.adoptopenjdk.jitwatch.core.IJITListener;
import org.adoptopenjdk.jitwatch.core.JITWatchConfig;
import org.adoptopenjdk.jitwatch.core.JITWatchConstants;
import org.adoptopenjdk.jitwatch.model.*;
import org.adoptopenjdk.jitwatch.model.bytecode.*;
import org.adoptopenjdk.jitwatch.parser.ILogParseErrorListener;
import org.adoptopenjdk.jitwatch.parser.ILogParser;
import org.adoptopenjdk.jitwatch.parser.hotspot.HotSpotLogParser;
import org.adoptopenjdk.jitwatch.treevisitor.TreeVisitor;
import org.adoptopenjdk.jitwatch.ui.code.languages.JitWatchLanguageSupport;
import org.adoptopenjdk.jitwatch.ui.code.languages.JitWatchLanguageSupportUtil;
import org.adoptopenjdk.jitwatch.util.ParseUtil;
import org.adoptopenjdk.jitwatch.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import static org.adoptopenjdk.jitwatch.ui.code.languages.JitWatchLanguageSupportUtil.LanguageSupport;

import javax.swing.SwingUtilities;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class JitWatchModelService
{
    private static final Logger LOG = Logger.getInstance(JitWatchModelService.class);

    private final Project project;
    private final JITWatchConfig config = new JITWatchConfig();
    private IReadOnlyJITDataModel model = null;
    private InlineAnalyzer inlineAnalyzer = null;
    private final Map<MetaClass, Map<IMetaMember, BytecodeAnnotations>> bytecodeAnnotations = new HashMap<>();
    private final List<JitWatchLanguageSupport<PsiElement, PsiElement>> allLanguages = JitWatchLanguageSupportUtil.getAllSupportedLanguages();
    private final List<Runnable> updateListeners = new ArrayList<>();

    public JitWatchModelService(Project project)
    {
        this.project = project;
    }

    public IReadOnlyJITDataModel getModel()
    {
        return model;
    }

    public List<InlineFailureInfo> getInlineFailures()
    {
        return inlineAnalyzer != null ? inlineAnalyzer.getFailures() : Collections.emptyList();
    }

    public List<InlineFailureGroup> getInlineFailureGroups()
    {
        return inlineAnalyzer != null ? inlineAnalyzer.getFailureGroups() : Collections.emptyList();
    }

    public void addUpdateListener(Runnable listener)
    {
        updateListeners.add(listener);
    }

    public void setParserResult(ILogParser parser)
    {
        model = parser.getModel();

        inlineAnalyzer = new InlineAnalyzer(parser.getModel(), metaMember ->
        {
            PsiElement psiMember = getPsiMember(metaMember);
            return psiMember != null && ModuleUtil.findModuleForPsiElement(psiMember) != null;
        });

        ApplicationManager.getApplication().runReadAction(() -> TreeVisitor.walkTree(model, inlineAnalyzer));

        SwingUtilities.invokeLater(() ->
        {
            modelUpdated();
            ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(JitReportToolWindow.ID);
            if (toolWindow != null)
            {
                toolWindow.activate(null);
            }
        });
    }

    private void modelUpdated()
    {
        DaemonCodeAnalyzer.getInstance(project).restart();
        for (Runnable listener : updateListeners)
        {
            listener.run();
        }
    }

    public MetaClass getMetaClass(PsiElement cls)
    {
        if (cls == null)
        {
            return null;
        }
        if (model == null)
        {
            return null;
        }

        JitWatchLanguageSupport<PsiElement, PsiElement> languageSupport = LanguageSupport.forLanguage(cls.getLanguage());
        if (languageSupport == null)
        {
            return null;
        }

        String classQName = languageSupport.getClassVMName(cls);
        return model.getPackageManager().getMetaClass(classQName);
    }

    public IMetaMember getMetaMember(PsiElement method)
    {
        if (method == null)
        {
            return null;
        }
        JitWatchLanguageSupport<PsiElement, PsiElement> languageSupport = LanguageSupport.forLanguage(method.getLanguage());
        if (languageSupport == null)
        {
            return null;
        }

        PsiElement containingClass = languageSupport.getContainingClass(method);
        MetaClass metaClass = getMetaClass(containingClass);
        if (metaClass == null)
        {
            return null;
        }

        return ContainerUtil.find(metaClass.getMetaMembers(), metaMember -> methodMatchesSignature(method, new PsiMetaMemberWrapper(metaMember)));
    }

    public PsiElement getPsiMember(IMetaMember metaMember)
    {
        if (metaMember == null)
        {
            return null;
        }

        PsiElement psiClass = getPsiClass(metaMember.getMetaClass());
        if (psiClass == null)
        {
            return null;
        }

        JitWatchLanguageSupport<PsiElement, PsiElement> languageSupport = LanguageSupport.forLanguage(psiClass.getLanguage());
        if (languageSupport == null)
        {
            return null;
        }

        List<PsiElement> allMethods = languageSupport.getAllMethods(psiClass);
        return ContainerUtil.find(allMethods, psiMethod -> methodMatchesSignature(psiMethod, new PsiMetaMemberWrapper(metaMember)));
    }

    private boolean methodMatchesSignature(PsiElement method, PsiMetaMemberWrapper metaMemberWrapper)
    {
        JitWatchLanguageSupport<PsiElement, PsiElement> languageSupport = LanguageSupport.forLanguage(method.getLanguage());
        return languageSupport.matchesSignature(method, metaMemberWrapper.getMemberName(),
                metaMemberWrapper.getParamTypeNames(), metaMemberWrapper.getReturnTypeName());
    }

    public PsiElement getPsiClass(MetaClass metaClass)
    {
        if (metaClass == null)
        {
            return null;
        }

        for (JitWatchLanguageSupport<PsiElement, PsiElement> languageSupport : allLanguages)
        {
            PsiElement psiClass = languageSupport.findClass(project, metaClass);
            if (psiClass != null)
            {
                return psiClass;
            }
        }
        return null;
    }

    public void loadBytecodeAsync(PsiFile file, Runnable callback)
    {
        ApplicationManager.getApplication().executeOnPooledThread(() ->
        {
            ApplicationManager.getApplication().runReadAction(() -> loadBytecode(file));

            SwingUtilities.invokeLater(callback);
        });
    }

    public void loadBytecode(PsiFile file)
    {
        Module[] moduleAr = new Module[1];
        ApplicationManager.getApplication().runReadAction(() ->
        {
            moduleAr[0] = ModuleUtil.findModuleForPsiElement(file);
        });

        Module module = moduleAr[0];

        if (module == null)
        {
            return;
        }

        CompilerModuleExtension compilerExtension = CompilerModuleExtension.getInstance(module);
        if (compilerExtension == null)
        {
            return;
        }

        List<String> outputRoots = Arrays.stream(compilerExtension.getOutputRoots(true))
                .map(VirtualFile::getCanonicalPath)
                .toList();

        Path javapPath = findJavapPath(module);
        if (javapPath == null)
        {
            return;
        }

        JitWatchLanguageSupport<PsiElement, PsiElement> languageSupport = LanguageSupport.forLanguage(file.getLanguage());
        if (languageSupport == null)
        {
            return;
        }

        List<PsiElement> allClasses = languageSupport.getAllClasses(file);
        for (PsiElement cls : allClasses)
        {
            Map<IMetaMember, BytecodeAnnotations> memberAnnotations = new HashMap<>();
            MetaClass metaClass = ApplicationManager.getApplication().runReadAction((Computable<MetaClass>) () -> getMetaClass(cls));
            if (metaClass == null)
            {
                continue;
            }

            metaClass.getClassBytecode(model, outputRoots, javapPath);
            buildAllBytecodeAnnotations(metaClass, memberAnnotations);
            bytecodeAnnotations.put(metaClass, memberAnnotations);
        }
    }

    private Path findJavapPath(Module module)
    {
        if (module == null)
        {
            return null;
        }

        com.intellij.openapi.projectRoots.Sdk sdk = ModuleRootManager.getInstance(module).getSdk();
        if (sdk == null)
        {
            return null;
        }

        JavaSdk javaSdk = JavaSdk.getInstance();
        if (sdk.getSdkType() != javaSdk) return null;

        String binPath = javaSdk.getBinPath(sdk);
        String exeName = SystemInfo.isWindows ? "javap.exe" : "javap";
        Path result = Paths.get(binPath, exeName);

        return Files.isRegularFile(result) ? result : null;
    }

    private void buildAllBytecodeAnnotations(MetaClass metaClass, Map<IMetaMember, BytecodeAnnotations> target)
    {
        for (IMetaMember metaMember : metaClass.getMetaMembers())
        {
            try
            {
                BytecodeAnnotations annotations = new IJBytecodeAnnotationBuilder().buildBytecodeAnnotations(metaMember, 0, model);
                target.put(metaMember, annotations);
            }
            catch (Exception e)
            {
                LOG.error("Failed to build annotations", e);
            }
        }
    }

    public void processBytecodeAnnotations(PsiFile psiFile, Callback5<PsiElement, IMetaMember, MemberBytecode, BytecodeInstruction, List<LineAnnotation>> callback)
    {
        JitWatchLanguageSupport<PsiElement, PsiElement> languageSupport = LanguageSupport.forLanguage(psiFile.getLanguage());
        if (languageSupport == null) return;

        List<PsiElement> allClasses = languageSupport.getAllClasses(psiFile);
        for (PsiElement cls : allClasses)
        {
            MetaClass metaClass = getMetaClass(cls);
            if (metaClass == null)
            {
                continue;
            }


            ClassBC classBytecode = metaClass.getClassBytecode();
            if (classBytecode == null)
            {
                continue;
            }

            Map<IMetaMember, BytecodeAnnotations> classAnnotations = bytecodeAnnotations.get(metaClass);
            if (classAnnotations == null)
            {
                continue;
            }

            List<PsiElement> allMethods = languageSupport.getAllMethods(cls);
            for (PsiElement method : allMethods)
            {
                IMetaMember member = classAnnotations.keySet().stream()
                        .filter(metaMember -> methodMatchesSignature(method, new PsiMetaMemberWrapper(metaMember)))
                        .findFirst()
                        .orElse(null);
                if (member == null)
                {
                    continue;
                }

                BytecodeAnnotations annotations = classAnnotations.get(member);
                if (annotations == null)
                {
                    continue;
                }

                MemberBytecode memberBytecode =  classBytecode.getMemberBytecode(member);
                if (memberBytecode == null)
                {
                    continue;
                }

                for (IMetaMember memberWithAnnot : annotations.getMembers())
                {
                    BytecodeAnnotationList annotationList = annotations.getAnnotationList(memberWithAnnot);
                    for (BytecodeInstruction instruction : memberBytecode.getInstructions())
                    {
                        List<LineAnnotation> annotationsForBCI = annotationList.getAnnotationsForBCI(instruction.getOffset());
                        if (annotationsForBCI == null || annotationsForBCI.isEmpty())
                        {
                            continue;
                        }

                        callback.call(method, member, memberBytecode, instruction, annotationsForBCI);
                    }
                }
            }
        }
    }

    private class IJBytecodeAnnotationBuilder extends BytecodeAnnotationBuilder
    {
        public IJBytecodeAnnotationBuilder()
        {
            super(false);
        }

        @Override
        public String buildInlineAnnotation(IParseDictionary parseDictionary, Map<String, String> methodAttrs,
                                            Map<String, String> callAttrs, String reason, boolean inlined)
        {
            String holder = methodAttrs.get(JITWatchConstants.ATTR_HOLDER);
            String methodName = methodAttrs.get(JITWatchConstants.ATTR_NAME);
            String calleeClass = ParseUtil.lookupType(holder, parseDictionary);
            String calleeMethod = StringUtil.replaceXMLEntities(methodName);
            StringBuilder builder = new StringBuilder(calleeClass)
                    .append(".")
                    .append(calleeMethod)
                    .append(inlined ? " inlined " : " not inlined ")
                    .append("(")
                    .append(reason)
                    .append(")");

            if (callAttrs.containsKey(JITWatchConstants.ATTR_COUNT))
            {
                builder.append(". Count: ").append(callAttrs.get(JITWatchConstants.ATTR_COUNT));
            }
            if (methodAttrs.containsKey(JITWatchConstants.ATTR_IICOUNT))
            {
                builder.append(". iicount: ").append(methodAttrs.get(JITWatchConstants.ATTR_IICOUNT));
            }
            if (methodAttrs.containsKey(JITWatchConstants.ATTR_BYTES))
            {
                builder.append(". Bytes: ").append(methodAttrs.get(JITWatchConstants.ATTR_BYTES));
            }
            return builder.toString();
        }
    }

    public MetaClass getMetaClassForClassName(String className)
    {
        if (model == null) return null;
        return model.getPackageManager().getMetaClass(className);
    }

    public static JitWatchModelService getInstance(Project project)
    {
        return project.getService(JitWatchModelService.class);
    }

    @FunctionalInterface
    public interface Callback<T>
    {
        void call(T t);
    }

    @FunctionalInterface
    public interface Callback5<A, B, C, D, E>
    {
        void call(A a, B b, C c, D d, E e);
    }
}
