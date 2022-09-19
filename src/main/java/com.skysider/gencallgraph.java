package com.skysider;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.*;
import com.ibm.wala.ipa.callgraph.cha.CHACallGraph;
import com.ibm.wala.ipa.callgraph.impl.DefaultEntrypoint;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.config.AnalysisScopeReader;
import java.io.File;
import java.io.IOException;
import java.util.*;
public class gencallgraph {
    static String basePath = "/home/linux";
    public static Map<String, CGNode> allNode = new HashMap<>();
    public static IClassHierarchy cha;
    public AnalysisScope scope;
    public CHACallGraph chaCallGraph;
    public HashSet<Entrypoint> entrypoints = new HashSet<>();
    public static List<String> jarFileNames = new ArrayList<>();
    public static List<String> jarFilePaths = new ArrayList<>();
    public static final Map<MethodReference, Set<MethodReference>> caller2calleeRef = new HashMap<>();
    public static final Map<MethodReference, Set<MethodReference>> callee2callerRef = new HashMap<>();
    public static final Map<IClass,Set<IClass>> interfaceClass = new HashMap<>();
    public static Map<TypeReference, IClass> ref2Class = new HashMap<>();
    public static Map<MethodReference, IMethod> ref2Method = new HashMap<>();
    public static Map<FieldReference, IField> ref2Field = new HashMap<>();
    public static Map<String, TypeReference> str2ClassRef = new HashMap<>();
    public static Map<String, MethodReference> str2MethodRef = new HashMap<>();
    public static Map<String, FieldReference> str2FieldRef = new HashMap<>();
    public static Map<MethodReference,Set<MethodReference>> caller2callee = new HashMap<>();
    public static MethodReference mr1;
    public static MethodReference mr2;

    public enum FilterKey{
        RemoteFileSystem,
        RemoteServivce,
        Configured,
    }
    public static int printTimes = 0;

    public boolean isRecord(Object obj){
        if(obj instanceof TypeReference){
            return (str2ClassRef.containsKey(((TypeReference) obj).getName().toString()));
        }else if(obj instanceof MethodReference){
            return (str2MethodRef.containsKey(((MethodReference) obj).getSignature()));
        }else if(obj instanceof FieldReference){
            return (str2FieldRef.containsKey(((FieldReference) obj).getSignature()));
        }
        return false;
    }
    public boolean addReference(Object obj){
        if(isRecord(obj))
            return false;
        if(obj instanceof TypeReference){
            str2ClassRef.put(((TypeReference) obj).toString(),(TypeReference) obj);
            return true;
        }else if(obj instanceof MethodReference){
            str2MethodRef.put(((MethodReference) obj).getSignature(),(MethodReference) obj);
            return true;
        }else if(obj instanceof FieldReference){
            str2FieldRef.put(((FieldReference) obj).getSignature(),(FieldReference) obj);
            return true;
        }
        return false;
    }
    public static String genJarPaths(String basepath) throws IOException {
        File init = new File(basepath);
        if (!init.exists()) {
            return null;
        }
        String res = "";
        addJarToList(init);
        for (String path : jarFilePaths) {
            res += (path + File.pathSeparator);
        }
        jarFilePaths.clear();
        jarFileNames.clear();
        return res.substring(0, res.length());
    }
    public static void addJarToList(File file) throws IOException {
        if (!file.exists()) {
            return;
        }
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if(files == null)
                return;
            for (File sub : files){
                addJarToList(sub);
            }
        }
        if (
                (file.toString().endsWith("3.3.2.jar") ||
                        (       file.getName().toString().endsWith(".jar") &&
                                file.getName().toString().startsWith(Setting.analysisProjectName) &&
                                !file.toString().contains("sharelib") &&
                                !file.toString().contains("original") &&
                                !file.toString().contains("example") &&
                                !file.toString().contains("test") &&
                                !file.toString().contains("shade"))
                ) &&
                file.toString().endsWith(".jar") &&
                !jarFileNames.contains(file.getName()) &&
                filterName(file.getName())) {
            jarFileNames.add(file.getName());
            jarFilePaths.add(file.toString());
        }
    }
    public static boolean filterName(Object obj) {
        String str = "";
        if (obj instanceof IClass) {
            str = ((IClass) obj).getReference().toString();
        } else if (obj instanceof IMethod) {
            str = ((IMethod) obj).getSignature();
        } else if (obj instanceof String) {
            str = (String) obj;
        }
        if (str.contains("third") || str.contains("examples") ||
                str.contains("test") || str.contains("com.ibm.wala") || str.contains("shade") ||
                str.startsWith("<Primordial,Ljava/lang/") || str.contains("org.apache.hadoop.fs.shell"))
            return false;
        return true;
    }
    public void getMethodCallSite(IMethod iMethod) throws CancelException, IOException {
        DefaultEntrypoint defaultEntrypoint = new DefaultEntrypoint(iMethod, cha);
        entrypoints.add(defaultEntrypoint);
    }
    public void getClassCallSite(IClass iClass) throws CancelException, IOException {
        if (!filterName(iClass)) {
            return;
        }
        for(IClass implementClass : iClass.getAllImplementedInterfaces()){
            if(!interfaceClass.containsKey(implementClass)){
                interfaceClass.put(implementClass,new HashSet<>());
            }
            interfaceClass.get(implementClass).add(iClass);
        }
        Collection<? extends IMethod> iClassAllMethods = iClass.getAllMethods();
        Iterator<? extends IMethod> iMethodIterator = iClassAllMethods.iterator();
        for (Iterator<? extends IMethod> it = iMethodIterator; it.hasNext(); ) {
            IMethod iMethod = it.next();
            ref2Method.put(iMethod.getReference(), iMethod);
            str2MethodRef.put(iMethod.getReference().getSignature(), iMethod.getReference());
            getMethodCallSite(iMethod);
        }
        Collection<? extends IField> iClassAllFields = iClass.getAllFields();
        for (IField iField : iClassAllFields) {
            ref2Field.put(iField.getReference(), iField);
            str2FieldRef.put(iField.getReference().getSignature(), iField.getReference());
        }
    }
    public void getJarCallSite(String jarFile) throws CancelException, IOException, ClassHierarchyException {
        String exclusionFileName = basePath + "/ZeroProject/exclusions.txt";
        File exclusionFile = new File(exclusionFileName);
        scope = AnalysisScopeReader.makeJavaBinaryAnalysisScope(jarFile, exclusionFile);
        cha = ClassHierarchyFactory.make(scope);
        Iterator<IClass> iClassIterator = cha.iterator();
        for (Iterator<IClass> it = iClassIterator; it.hasNext(); ) {
            IClass iClass = it.next();
            ref2Class.put(iClass.getReference(), iClass);
            str2ClassRef.put(iClass.getReference().toString(), iClass.getReference());
            getClassCallSite(iClass);
        }
        chaCallGraph = new CHACallGraph(cha);
        chaCallGraph.init(entrypoints);
        for (CGNode cgNode : chaCallGraph)
            allNode.put(cgNode.getMethod().getSignature(), cgNode);
    }
    public void callGraph() throws IOException {
        Set<String> names = allNode.keySet();
        for (String name : names) {
            if (!filterName(name))
                continue;
            CGNode curNode = allNode.get(name);
            if (curNode == null)
                continue;
            Iterator<CallSiteReference> succs = curNode.iterateCallSites();
            if (succs == null)
                continue;
            Set<MethodReference> predRefList = new HashSet<>();
            MethodReference curNodeReference = curNode.getMethod().getReference();
            if(!isRecord(curNodeReference))
                addReference(curNodeReference);
            curNodeReference = str2MethodRef.get(curNodeReference.getSignature());
            for (Iterator<CallSiteReference> it = succs; it.hasNext(); ) {
                CallSiteReference pred = it.next();
                MethodReference predMethodRef = pred.getDeclaredTarget();
                if(!isRecord(predMethodRef)) {
                    addReference(predMethodRef);
                }
                predRefList.add(str2MethodRef.get(predMethodRef.getSignature()));
                predMethodRef = str2MethodRef.get(predMethodRef.getSignature());
                if (!callee2callerRef.keySet().contains(predMethodRef)) {
                    Set<MethodReference> list = new HashSet<>();
                    list.add(curNodeReference);
                    callee2callerRef.put(predMethodRef, list);
                } else {
                    callee2callerRef.get(predMethodRef).add(curNodeReference);
                }
                IClass predClass = ref2Class.get(predMethodRef.getDeclaringClass());
                if (predClass == null) {
                    continue;
                }
                Set<IClass> subclasses = findSubclasses(ref2Class.get(predMethodRef.getDeclaringClass()));
                subclasses.addAll(predClass.getAllImplementedInterfaces());
                if(interfaceClass.containsKey(predClass))
                    subclasses.addAll(interfaceClass.get(predClass));
                if(name.contains("FileTxnSnapLog.save")){
                    System.out.println(predClass.getDirectInterfaces());
                    System.out.println(predClass.getAllImplementedInterfaces());
                    if(interfaceClass.containsKey(predClass)){
                        System.out.println(interfaceClass.get(predClass));
                    }
                }
                 for (IClass subclass : subclasses) {
                    IMethod iMethod = subclass.getMethod(predMethodRef.getSelector());
                    if (iMethod == null)
                        continue;
                    MethodReference subClassPred = iMethod.getReference();
                    if(!isRecord(subClassPred))
                        addReference(subClassPred);
                    subClassPred = str2MethodRef.get(subClassPred.getSignature());
                    predRefList.add(subClassPred);
                    if (!callee2callerRef.keySet().contains(subClassPred)) {
                        Set<MethodReference> list = new HashSet<>();
                        list.add(curNodeReference);
                        callee2callerRef.put(subClassPred, list);
                    } else {
                        callee2callerRef.get(subClassPred).add(curNodeReference);
                    }
                }
            }
            if (!caller2calleeRef.containsKey(curNodeReference)) {
                caller2calleeRef.put(curNodeReference, predRefList);
            } else {
                caller2calleeRef.get(curNodeReference).addAll(predRefList);
            }
        }
        for (MethodReference methodReference : callee2callerRef.keySet()) {
            if (methodReference.getDeclaringClass().toString().contains("$")) {
                IClass iClass = ref2Class.get(methodReference.getDeclaringClass());
                if (iClass == null) {
                    continue;
                }
                IMethod iMethod = iClass.getMethod(methodReference.getSelector());
                if (iMethod == null) {
                    continue;
                }
                if (methodReference != iMethod.getReference()) {
                    Set<MethodReference> succ = new HashSet<>();
                    Set<MethodReference> callees = caller2calleeRef.get(iMethod.getReference());
                    if (callees == null)
                        continue;
                    for (MethodReference callee : callees) {
                        if (callee.getDeclaringClass().toString().contains("$")) {
                            if (ref2Class.get(callee.getDeclaringClass()) != null && ref2Class.get(methodReference.getDeclaringClass()) != null &&
                                ref2Class.get(callee.getDeclaringClass()).getSuperclass() == ref2Class.get(methodReference.getDeclaringClass()).getSuperclass() &&
                                ref2Class.get(callee.getDeclaringClass()) != ref2Class.get(methodReference.getDeclaringClass()))
                                        continue;
                        }
                        callee2callerRef.get(callee).add(methodReference);
                        succ.add(callee);
                    }
                    if (caller2calleeRef.containsKey(methodReference)) {
                        caller2calleeRef.get(methodReference).addAll(succ);
                    } else {
                        caller2calleeRef.put(methodReference, succ);
                    }
                }
            }
        }
    }
    public Set<IClass> findSubclasses(Collection<IClass> iClasses) {
        Set<IClass> res = new HashSet<>();
        if (iClasses == null) {
            return res;
        }
        if (iClasses.size() == 0) {
            return res;
        }
        for (IClass iClass : iClasses) {
            if(!filterName(iClass)){
                continue;
            }
            if (iClass == null)
                continue;
            Collection<IClass> subclasses;
            subclasses = cha.getImmediateSubclasses(iClass);
            res.addAll(subclasses);
            res.addAll(findSubclasses(subclasses));
        }
        return res;
    }
    public Set<IClass> findSubclasses(IClass iClass) {
        Collection<IClass> iClassCollection = new HashSet<>();
        iClassCollection.add(iClass);
        return findSubclasses(iClassCollection);
    }
    public Set<IClass> findSubclasses(String str) {
        TypeReference typeReference = str2ClassRef.get(str);
        if(typeReference == null)
            return null;
        return findSubclasses(typeReference);
    }
    public Set<IClass> findSubclasses(TypeReference typeReference) {
        IClass iClass = ref2Class.get(typeReference);
        if(iClass == null)
            return null;
        return findSubclasses(iClass);
    }
    public void showAllMethodFromEnd(MethodReference endMethodRef,int maxdeep,int curdeep){
        if(curdeep >= maxdeep){
            return;
        }
        Set<MethodReference> callers = callee2callerRef.get(endMethodRef);
        if(callers == null){
            return;
        }
        for(MethodReference caller : callers){
            showAllMethodFromEnd(caller,maxdeep,curdeep + 1);
        }
    }
    public Set<MethodReference> getAllMethodFromEnd(MethodReference endMethod,Trace trace,List<MethodReference> permission,Set<MethodReference> kill,Set<IClass> localfilesystem,Set<IClass> remotefilesystem,Map<String,IClass> belong,List<MethodReference> callstack,List<Trace> tracestack,int maxDeep,int curDeep) throws IOException {
        Set<MethodReference> res = new HashSet<>();
        if(curDeep >= maxDeep){
            return res;
        }
        if(endMethod == null) {
            return res;
        }
        if(!filterName(endMethod)){
            return res;
        }
        Set<MethodReference> callers = callee2callerRef.get(endMethod);
        if(callers == null){
            return res;
        }
        IClass endClass = ref2Class.get(endMethod.getDeclaringClass());
        if(kill.size() != 0){
            if(kill.contains(endMethod)){
                return res;
            }
        }
        if(endMethod.getSignature().contains("read")){
            return res;
        }
        if(callstack.contains(endMethod)){
            return res;
        }
        Map<String, IClass> curbelong = new HashMap<>(belong);
        List<MethodReference> curcallstack = new ArrayList<>(callstack);
        curcallstack.add(endMethod);
        if(remotefilesystem.contains(endClass)){
            if(curbelong.get("RemoteFileSystem") == null){
                curbelong.put("RemoteFileSystem",endClass);
            }else{
                if(!relation(curbelong.get("RemoteFileSystem"),endClass,false)){
                    return res;
                }
            }
        }
        endClass = null;
        for(MethodReference caller : callers){
            if(!allNode.keySet().contains(caller.getSignature())) {
                continue;
            }
            Trace t = newDU.inner(allNode.get(caller.getSignature()),endMethod, trace);
            List<Trace> curtracestack = new ArrayList<>(tracestack);
            curtracestack.add(t);
            if(!caller2callee.keySet().contains(caller)){
                Set<MethodReference> set = new HashSet<>();
                set.add(endMethod);
                caller2callee.put(caller,set);
            }else{
                if(caller2callee.get(caller).contains(endMethod)){
                    continue;
                }else{
                    caller2callee.get(caller).add(endMethod);
               }
            }
            IMethod callerMethod = ref2Method.get(caller);
            if(callerMethod == null){
                continue;
            }
            if(t == null || t.para == -1 || t.para > (callerMethod.isStatic() ? callerMethod.getNumberOfParameters() : callerMethod.getNumberOfParameters() + 1)){
                newDU. reachMethod(curtracestack,curcallstack,caller,permission);
            }
            if(t == null){
                continue;
            }
            if(t.para == -1){
                continue;
            }
            if(t.para > (callerMethod.isStatic() ? callerMethod.getNumberOfParameters() : callerMethod.getNumberOfParameters() + 1)){
                continue;
            }
            t.para -= 1;
            res.addAll(getAllMethodFromEnd(caller,t,permission,kill,localfilesystem,remotefilesystem,curbelong,curcallstack,curtracestack,maxDeep,curDeep+1));
        }
        callers = null;
        return res;
    }
    public static boolean relation(IClass iClass1 , IClass iClass2,boolean force){
        IClass _iClass1 = iClass1;
        IClass _iClass2 = iClass2;
        while(iClass1 != null){
            if(iClass1.equals(_iClass2))
                return true;
            iClass1 = iClass1.getSuperclass();
        }
        if(!force){
            while(iClass2 != null){
                if(iClass2.equals(_iClass1))
                    return true;
                iClass2 = iClass2.getSuperclass();
            }
        }
        _iClass1 = null;
        _iClass2 = null;
        return false;
    }
    public static void showMethodStack(List<MethodReference> methodReferences, MethodReference specialMethodRef,boolean isend) throws IOException {
        boolean showSpecial = (specialMethodRef == null);
        boolean isShowHidden = false;
        logger.State state;
        if (isend) {
            state = logger.State.ERROR;
        } else {
            state = logger.State.SUCCESS;
        }
        printTimes ++;
        int size = methodReferences.size();
        logger.log(state,"Find method : " + methodReferences.get(0).getSignature());
        for (MethodReference methodReference : methodReferences) {
            if (!showSpecial && methodReference.getSignature().equals(specialMethodRef.getSignature())) {
                logger.log(state, "===>" + methodReference.getSignature());
                showSpecial = true;
            }
            logger.log(state, "\t" + methodReference.getSignature());
        }
    }
    public gencallgraph(String basePath) throws ClassHierarchyException, CancelException, IOException {
        getJarCallSite(basePath);
        callGraph();
        for(MethodReference methodReference : caller2calleeRef.keySet()){
            if(methodReference.getSignature().contains("FileTxnSnapLog.save")){
                Set<MethodReference> callees = caller2calleeRef.get(methodReference);
                for(MethodReference callee : callees){
                    System.out.println(callee);
                }
            }
        }
    }
}
