/*
 * Copyright (c) 2013-2021 Chris Newland.
 * Licensed under https://github.com/AdoptOpenJDK/jitwatch/blob/master/LICENSE-BSD
 * Instructions: https://github.com/AdoptOpenJDK/jitwatch/wiki
 */
package org.adoptopenjdk.jitwatch.model;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.adoptopenjdk.jitwatch.model.bytecode.SourceMapper;
import org.adoptopenjdk.jitwatch.util.ParseUtil;
import org.adoptopenjdk.jitwatch.logger.Logger;
import org.adoptopenjdk.jitwatch.logger.LoggerFactory;

import static org.adoptopenjdk.jitwatch.core.JITWatchConstants.*;

public class JITDataModel implements IReadOnlyJITDataModel
{
	private static final Logger logger = LoggerFactory.getLogger(JITDataModel.class);

	private PackageManager packageManager;
	private JITStats stats;

	// Not using CopyOnWriteArrayList as writes will vastly out number reads
	private List<JITEvent> jitEvents = new ArrayList<>();

	// written during parse, make copy for graphing as needs sort
	private List<CodeCacheEvent> codeCacheTagList = new ArrayList<>();

	private Map<String, CompilerThread> compilerThreads = new HashMap<>();

	private Tag endOfLog;

	private int jdkMajorVersion;

	private long baseTimestamp = 0;

	public JITDataModel()
	{
		packageManager = new PackageManager();
		stats = new JITStats();
	}

	public void setJDKMajorVersion(int version)
	{
		this.jdkMajorVersion = version;
	}

	@Override public int getJDKMajorVersion()
	{
		return jdkMajorVersion;
	}

	public void reset()
	{
		if (DEBUG_LOGGING)
		{
			logger.debug("JITDataModel.reset()");
		}

		packageManager.clear();

		SourceMapper.clear();

		stats.reset();

		jitEvents.clear();

		for (CompilerThread thread : compilerThreads.values())
		{
			thread.clear();
		}

		compilerThreads.clear();

		codeCacheTagList.clear();
	}

	@Override public List<CompilerThread> getCompilerThreads()
	{
		List<CompilerThread> result = new ArrayList<>();

		for (Map.Entry<String, CompilerThread> entry : compilerThreads.entrySet())
		{
			if (!entry.getValue().getCompilations().isEmpty())
			{
				result.add(entry.getValue());
			}
		}

		Collections.sort(result, new Comparator<CompilerThread>()
		{
			@Override public int compare(CompilerThread o1, CompilerThread o2)
			{
				return o1.getThreadName().compareTo(o2.getThreadName());
			}
		});

		return Collections.unmodifiableList(result);
	}

	@Override public CompilerThread createCompilerThread(String threadId, String threadName)
	{
		CompilerThread compilerThread = new CompilerThread(threadId, threadName);

		compilerThreads.put(threadId, compilerThread);

		getJITStats().incCompilerThreads();

		return compilerThread;
	}

	@Override public CompilerThread getCompilerThread(String threadId)
	{
		return compilerThreads.get(threadId);
	}

	@Override public PackageManager getPackageManager()
	{
		return packageManager;
	}

	@Override public JITStats getJITStats()
	{
		return stats;
	}

	// ugly but better than using COWAL with so many writes
	public void addEvent(JITEvent event)
	{
		synchronized (jitEvents)
		{
			jitEvents.add(event);
		}
	}

	@Override public List<JITEvent> getEventListCopy()
	{
		synchronized (jitEvents)
		{
			return new ArrayList<>(jitEvents);
		}
	}

	public void addNativeBytes(long count)
	{
		stats.addNativeBytes(count);
	}

	public void updateStats(IMetaMember member, Map<String, String> attrs)
	{
		String fullSignature = member.toString();

		for (String modifier : MODIFIERS)
		{
			if (fullSignature.contains(modifier + S_SPACE))
			{
				String incMethodName = "incCount" + modifier.substring(0, 1).toUpperCase() + modifier.substring(1);

				try
				{
					MethodType mt = MethodType.methodType(void.class);

					MethodHandle mh = MethodHandles.lookup().findVirtual(JITStats.class, incMethodName, mt);

					mh.invokeExact(stats);
				}
				catch (Throwable t)
				{
					logger.error("Exception: {}", t.getMessage(), t);
				}
			}
		}

		String level = attrs.get(ATTR_LEVEL);

		if (level != null)
		{
			if ("1".equals(level))
			{
				stats.incCountLevel1();
			}
			else if ("2".equals(level))
			{
				stats.incCountLevel2();
			}
			else if ("3".equals(level))
			{
				stats.incCountLevel3();
			}
			else if ("4".equals(level))
			{
				stats.incCountLevel4();
			}
		}
		else
		{
			stats.incCountLevel4();
		}

		String compiler = attrs.get(ATTR_COMPILER);

		if (compiler != null)
		{
			if (C1.equalsIgnoreCase(compiler))
			{
				stats.incCountC1();
			}
			else if (C2.equalsIgnoreCase(compiler))
			{
				stats.incCountC2();
			}
		}

		String compileKind = attrs.get(ATTR_COMPILE_KIND);

		boolean isC2N = false;

		if (compileKind != null)
		{
			if (OSR.equalsIgnoreCase(compileKind))
			{
				stats.incCountOSR();
			}
			else if (C2N.equalsIgnoreCase(compileKind))
			{
				stats.incCountC2N();
				isC2N = true;
			}
		}

		String compileID = attrs.get(ATTR_COMPILE_ID);

		Compilation compilation = member.getCompilationByCompileID(compileID);

		if (compilation != null)
		{
			if (!isC2N)
			{
				stats.recordDelay(compilation.getCompilationDuration());
			}
		}
		else
		{
			logger.warn("Didn't find compilation with ID {} on member {}", compileID, member.getFullyQualifiedMemberName());
		}
	}

	@Override public IMetaMember findMetaMember(MemberSignatureParts msp)
	{
		IMetaMember result = null;

		MetaClass metaClass = packageManager.getMetaClass(msp.getFullyQualifiedClassName());

		if (metaClass == null) // possible if no TraceClassLoading logs
		{
			if (DEBUG_LOGGING)
			{
				logger.debug("No metaClass found, trying late load {}", msp.getFullyQualifiedClassName());
			}

			metaClass = ParseUtil.lateLoadMetaClass(this, msp.getFullyQualifiedClassName());
		}

		if (metaClass != null)
		{
			List<IMetaMember> metaList = metaClass.getMetaMembers();

			if (DEBUG_LOGGING)
			{
				logger.debug("Comparing msp against {} members of metaClass {}", metaList.size(), metaClass.toString());
			}

			for (IMetaMember member : metaList)
			{
				if (member.matchesSignature(msp, true))
				{
					result = member;
					break;
				}
			}

			if (result == null)
			{
				if (msp.getMemberName().equals("<init>"))
				{
					MetaConstructor metaConstructor = new MetaConstructor(msp, metaClass);
					metaClass.addMember(metaConstructor);
					result = metaConstructor;
					stats.incCountMethod();
				}
				else
				{
					MetaMethod metaMethod = new MetaMethod(msp, metaClass);
					metaClass.addMember(metaMethod);
					result = metaMethod;
					stats.incCountMethod();
				}
			}
		}
		else
		{
			if (DEBUG_LOGGING)
			{
				logger.debug("No metaClass found for fqClassName {}", msp.getFullyQualifiedClassName());
			}
		}

		return result;
	}

	@Override public MetaClass buildAndGetMetaClass(String fqClassName)
	{
		MetaClass resultMetaClass;

		String packageName;
		String className;

		int lastDotIndex = fqClassName.lastIndexOf(C_DOT);

		if (lastDotIndex != -1)
		{
			packageName = fqClassName.substring(0, lastDotIndex);
			className = fqClassName.substring(lastDotIndex + 1);
		}
		else
		{
			packageName = S_EMPTY;
			className = fqClassName;
		}

		if (DEBUG_LOGGING)
		{
			logger.debug("buildAndGetMetaClass {} {}", packageName, fqClassName);
		}

		MetaPackage metaPackage = packageManager.getMetaPackage(packageName);

		if (metaPackage == null)
		{
			metaPackage = packageManager.buildPackage(packageName);
		}

		resultMetaClass = new MetaClass(metaPackage, className);

		packageManager.addMetaClass(resultMetaClass);

		metaPackage.addClass(resultMetaClass);

		stats.incCountClass();

		return resultMetaClass;
	}

	public void addCodeCacheEvent(CodeCacheEvent event)
	{
		synchronized (codeCacheTagList)
		{
			codeCacheTagList.add(event);
		}
	}

	public void setEndOfLog(Tag tag)
	{
		this.endOfLog = tag;
	}

	@Override public Tag getEndOfLogTag()
	{
		return endOfLog;
	}

	@Override public List<CodeCacheEvent> getCodeCacheEvents()
	{
		synchronized (codeCacheTagList)
		{
			return new ArrayList<>(codeCacheTagList);
		}
	}

	@Override public long getBaseTimestamp()
	{
		return baseTimestamp;
	}

	public void setBaseTimestamp(long baseTimestamp)
	{
		this.baseTimestamp = baseTimestamp;
	}
}
