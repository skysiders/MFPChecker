package com.skysider;
import com.ibm.wala.cfg.cdg.ControlDependenceGraph;
import com.ibm.wala.classLoader.IBytecodeMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.ssa.*;
import com.ibm.wala.types.MethodReference;
import java.io.IOException;
import java.util.*;
public class newDU {
    public static int totalbugs;
    public static int hdfsbugs;
    public static int javabugs;
    public static SSAInvokeInstruction getInstruction(CGNode node , MethodReference methodReference){
        Iterator<SSAInstruction> ssaInstructionIterator = node.getIR().iterateAllInstructions();
        for (Iterator<SSAInstruction> it = ssaInstructionIterator; it.hasNext(); ) {
            SSAInstruction ssaInstruction = it.next();
            if(ssaInstruction instanceof SSAInvokeInstruction){
                if(((SSAInvokeInstruction) ssaInstruction).getDeclaredTarget().getSelector().equals(methodReference.getSelector())){
                    return (SSAInvokeInstruction) ssaInstruction;
                }
            }
        }
        return null;
    }
    public static Trace inner(CGNode node , MethodReference methodReference , Trace trace) throws IOException {
        SSAInvokeInstruction endIR = getInstruction(node,methodReference);
        int def;
        if(endIR.isSpecial()){
            def = endIR.getUse(trace.para);
        }else if(endIR.isStatic()){
            def = endIR.getUse(trace.para);
        }else{
            def = endIR.getUse(trace.para);
        }
        Trace newt = new Trace(def,trace.fieldReference,endIR,trace.traceintoStack);
        Trace t = traceReturn(node,newt);
        if(t.para > node.getMethod().getNumberOfParameters() + 1){
            Trace tmp = traceReturn(node, t);
            if(t.para != -1)
                t = tmp;
        }
        t.ssaInstruction = endIR;
        return t;
    }
    public static Trace traceReturn(CGNode node , Trace trace) throws IOException {
        int def = trace.para;
        if(def <= node.getMethod().getNumberOfParameters()){
            Trace t = new Trace(def,trace.fieldReference,trace.ssaInstruction,trace.traceintoStack);
            return t;
        }
        DefUse du = node.getDU();
        SSAInstruction instruction = du.getDef(def);
        if(instruction == null){
            return new Trace(-1,null,trace.ssaInstruction,trace.traceintoStack);
        }
        Trace t = traceInstruction(node,new Trace(0,trace.fieldReference,instruction,trace.traceintoStack));
        if(t.para >= 0 && 1 <= t.para && t.para <= node.getMethod().getNumberOfParameters() + 1){
            if (instruction instanceof SSAPhiInstruction) {
                return t;
            }else if(instruction instanceof SSANewInstruction){
                return t;
            }else if(instruction instanceof SSAInvokeInstruction){
                if(instruction.getUse(t.para - 1) <= node.getMethod().getNumberOfParameters() + 1){
                    t.para = instruction.getUse(t.para - 1);
                    return t;
                }else{
                    t.para = -1;
                    return t;
                }
            }else{
                if(t.para == -1){
                    return t;
                }
                t.para = instruction.getUse(t.para - 1);
            }
        }
        return t;
    }
    public static Trace traceInstruction(CGNode node, Trace trace) throws IOException {
        SSAInstruction instruction = trace.ssaInstruction;
        Trace t = new Trace(-1,null,instruction,trace.traceintoStack);
        for(int i=0;instruction != null && i<trace.traceintoStack.size();i++){
            if(instruction.toString().equals(trace.traceintoStack.get(i).toString()))
                return t;
        }
        trace.push(instruction);
        if(instruction instanceof SSAThrowInstruction){
            return t;
        }
        if(instruction instanceof SSAGetInstruction){
            SSAGetInstruction ssaGetInstruction = (SSAGetInstruction) instruction;
            if(ssaGetInstruction.getUse(0) == 1){
                List<SSAPutInstruction> ssaPutInstructionList = getPutInstruction(node);
                if(ssaPutInstructionList.size() != 0){
                    for(SSAPutInstruction ssaPutInstruction : ssaPutInstructionList){
                        if(ssaPutInstruction.getDeclaredField().equals(ssaGetInstruction.getDeclaredField())){
                            int use = ssaPutInstruction.getUse(1);
                            return traceInstruction(node,new Trace(0,null,node.getDU().getDef(use),trace.traceintoStack));
                        }
                    }
                }
                return new Trace(ssaGetInstruction.getUse(0),ssaGetInstruction.getDeclaredField(),ssaGetInstruction,trace.traceintoStack);
            }else if(ssaGetInstruction.getUse(0) <= node.getMethod().getNumberOfParameters() + 1 && ssaGetInstruction.getUse(0) > 0){
                return new Trace(ssaGetInstruction.getUse(0),ssaGetInstruction.getDeclaredField(),ssaGetInstruction,trace.traceintoStack);
            }else if(ssaGetInstruction.isStatic()){
                List<SSAPutInstruction> ssaPutInstructionList = getPutInstruction(node);
                if(ssaPutInstructionList.size() != 0){
                    for(SSAPutInstruction ssaPutInstruction : ssaPutInstructionList){
                        if(ssaPutInstruction.getDeclaredField().equals(ssaGetInstruction.getDeclaredField())){
                            int use = ssaPutInstruction.getUse(1);
                            return traceInstruction(node,new Trace(0,null,node.getDU().getDef(use),trace.traceintoStack));
                        }
                    }
                }
                return new Trace(ssaGetInstruction.getUse(0),ssaGetInstruction.getDeclaredField(),ssaGetInstruction,trace.traceintoStack);
            }else{
                return traceInstruction(node,new Trace(0,null,node.getDU().getDef(ssaGetInstruction.getUse(0)),trace.traceintoStack));
            }
        }else if(instruction instanceof SSAInvokeInstruction){
            SSAInvokeInstruction ssaInvokeInstruction = (SSAInvokeInstruction) instruction;
            Trace invoke = new Trace(0,trace.fieldReference,ssaInvokeInstruction,trace.traceintoStack);
            Trace specialrtn = special(node,invoke);
            if(specialrtn.para != -1){
                if(specialrtn.para <= node.getMethod().getNumberOfParameters() + 1 && specialrtn.para >= 0){
                    return specialrtn;
                }else{
                    return new Trace(-1,null,ssaInvokeInstruction,trace.traceintoStack);
                }
            }
            invoke.pop();
            Trace invoket = traceInto(invoke);
            while(invoket.para >= 0 && invoket.para > node.getMethod().getNumberOfParameters() + 1){
                invoket = traceInstruction(node,new Trace(0,trace.fieldReference,node.getDU().getDef(node.getDU().getNumberOfUses(invoket.para - 1)),trace.traceintoStack));
            }
            return invoket;
        }else if(instruction instanceof SSAPhiInstruction){
            for(int i=0;i<instruction.getNumberOfUses();i++){
                if(instruction.getDef(0) <= instruction.getUse(i))
                    continue;
                t = traceReturn(node,new Trace(instruction.getUse(i),trace.fieldReference,instruction,trace.traceintoStack));
                if(t.para != -1){
                    return t;
                }
            }
        }else if(instruction instanceof SSANewInstruction){
            Iterator<SSAInstruction> ssaInstructionIterator = node.getDU().getUses(instruction.getDef());
            for (Iterator<SSAInstruction> it = ssaInstructionIterator; it.hasNext(); ) {
                SSAInstruction instruction1 = it.next();
                if(instruction1.getUse(0) == instruction.getDef() &&
                        instruction1 instanceof SSAInvokeInstruction &&
                        ((SSAInvokeInstruction) instruction1).getDeclaredTarget().isInit()) {
                    t = traceInstruction(node, new Trace(0,trace.fieldReference,instruction1,trace.traceintoStack));
                    if(t.para != -1){
                        t.para = instruction1.getUse(t.para-1);
                        return t;
                    }
                }
            }
        }else if(instruction instanceof SSACheckCastInstruction){
            SSACheckCastInstruction ssaCheckCastInstruction = (SSACheckCastInstruction) instruction;
            t = new Trace(ssaCheckCastInstruction.getUse(0),null,ssaCheckCastInstruction,trace.traceintoStack);
        }
        return t;
    }
    public static Trace traceInto(Trace trace) throws IOException {
        SSAInvokeInstruction ssaInvokeInstruction = (SSAInvokeInstruction) trace.ssaInstruction;
        if(trace.inStack(ssaInvokeInstruction) && !trace.traceintoStack.isEmpty()){
            return new Trace(-1,null,ssaInvokeInstruction,trace.traceintoStack);
        }
        trace.push(ssaInvokeInstruction);
        MethodReference methodReference = ssaInvokeInstruction.getDeclaredTarget();
        CGNode node = gencallgraph.allNode.get(methodReference.getSignature());
        if(node == null){
            trace.pop();
            return new Trace(-1,null,trace.ssaInstruction,trace.traceintoStack);
        }
        Set<SSAReturnInstruction> returnSSA = new HashSet<>();
        Set<SSAPutInstruction> putSSA = new HashSet<>();
        if(node.getIR() == null){
            trace.pop();
            return new Trace(-1,null,trace.ssaInstruction,trace.traceintoStack);
        }
        Iterator<SSAInstruction> ssaInstructionIterator = node.getIR().iterateAllInstructions();
        for (Iterator<SSAInstruction> it = ssaInstructionIterator; it.hasNext(); ) {
            SSAInstruction ssaInstruction = it.next();
            if(ssaInstruction instanceof SSAReturnInstruction)
                returnSSA.add((SSAReturnInstruction) ssaInstruction);
            if(ssaInstruction instanceof SSAPutInstruction)
                putSSA.add((SSAPutInstruction) ssaInstruction);
        }
        List<SSAReturnInstruction> returnInstructionList = new ArrayList<>(returnSSA);
        for(int i=0;i<returnInstructionList.size();i++){
            SSAReturnInstruction ssaReturnInstruction = returnInstructionList.get(returnInstructionList.size() - 1 - i);
            Trace t = new Trace(-1,null,ssaReturnInstruction,trace.traceintoStack);
            if(ssaReturnInstruction.getResult() == -1){
                for(SSAPutInstruction ssaPutInstruction : putSSA){
                    if(ssaPutInstruction.getDeclaredField().equals(trace.fieldReference)){
                        Trace trace1 = new Trace(ssaPutInstruction.getUse(0),null,node.getDU().getDef(ssaPutInstruction.getUse(1)),trace.traceintoStack);
                        t = traceInstruction(node,trace1);
                        if(t.para != -1 && t.para > node.getMethod().getNumberOfParameters() + 1){
                                t.para = -1;
                                trace.pop();
                                return t;
                            }
                    }
                }
            }else {
                t = traceReturn(node, new Trace(ssaReturnInstruction.getResult(),trace.fieldReference,ssaReturnInstruction,trace.traceintoStack));
            }
            if( t.para != -1 ){
                if(t.para > (node.getMethod().isStatic() ? node.getMethod().getNumberOfParameters() : node.getMethod().getNumberOfParameters() + 1))
                    t.para = -1;
                trace.pop();
                return t;
            }
        }
        trace.pop();
        return new Trace(-1,null,trace.ssaInstruction,trace.traceintoStack);
    }
    public static Trace special(CGNode node,Trace trace) throws IOException {
        SSAInvokeInstruction ssaInvokeInstruction = (SSAInvokeInstruction) trace.ssaInstruction;
        if(ssaInvokeInstruction.getDeclaredTarget().getSignature().equals("java.io.FileSystem.normalize(Ljava/lang/String;)Ljava/lang/String;")){
            return traceReturn(node,new Trace(ssaInvokeInstruction.getUse(1),null,ssaInvokeInstruction,trace.traceintoStack));
        }
        if(ssaInvokeInstruction.getDeclaredTarget().getSignature().equals("java.io.FileSystem.resolve(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;")){
            return traceReturn(node,new Trace(-1,null,ssaInvokeInstruction,trace.traceintoStack));
        }
        return new Trace(-1,null,trace.traceintoStack);
    }
    public static List<SSAPutInstruction> getPutInstruction(CGNode node){
        if(node == null)
            return null;
        IR ir = node.getIR();
        if(ir == null)
            return null;
        Iterator<SSAInstruction> ssaInstructionIterator = ir.iterateAllInstructions();
        List<SSAPutInstruction> ssaPutInstructionList = new ArrayList<>();
        for (Iterator<SSAInstruction> it = ssaInstructionIterator; it.hasNext(); ) {
            SSAInstruction ssaInstruction = it.next();
            if(ssaInstruction instanceof SSAPutInstruction)
                ssaPutInstructionList.add((SSAPutInstruction) ssaInstruction);
        }
        return ssaPutInstructionList;
    }
    public static void reachMethod(List<Trace> traceStack,List<MethodReference> calleeMethodReference,MethodReference callerMethodReference,List<MethodReference> permMethodRef) throws IOException {
        boolean isreach = false;
        if(traceStack == null || traceStack.size() == 0){
            return;
        }
        if(permMethodRef == null){
            return;
        }
        List<Trace> curtracestack = new ArrayList<>();
        curtracestack.addAll(traceStack);
        int i = 1;
        for(i = 1;i<calleeMethodReference.size();i++){
            SSAInvokeInstruction ssaInvokeInstruction = null;
            CGNode curNode = gencallgraph.allNode.get(calleeMethodReference.get(i).getSignature());
            Iterator<SSAInstruction> ssaInstructionIterator = curNode.getIR().iterateAllInstructions();
            for (; ssaInstructionIterator.hasNext(); ) {
                SSAInstruction ssaInstruction = ssaInstructionIterator.next();
                if(ssaInstruction instanceof SSAInvokeInstruction){
                    ssaInvokeInstruction = (SSAInvokeInstruction) ssaInstruction;
                    if(ssaInvokeInstruction.getDeclaredTarget().getSelector().equals(calleeMethodReference.get(i-1).getSelector()) && !isreach){
                        {
                            if(ssaInvokeInstruction.getDeclaredTarget().getDeclaringClass().toString().replace("/",".").contains("org.apache.hadoop.fs.FileContext")){
                                int use = ssaInvokeInstruction.getUse(0);
                                List<Integer> aliasFieldUseNum = aliasField(curNode,use);
                                Set<SSAInstruction> instructionUseAliasField = new HashSet<>();
                                for (Integer integer : aliasFieldUseNum) {
                                    Iterator<SSAInstruction> curAliasFieldNumInstructions = curNode.getDU().getUses(integer);
                                    for (; curAliasFieldNumInstructions.hasNext(); ) {
                                        SSAInstruction curAliasFieldNumInstruction = curAliasFieldNumInstructions.next();
                                        instructionUseAliasField.add(curAliasFieldNumInstruction);
                                    }
                                    if (integer == use)
                                        break;
                                }
                                for(SSAInstruction eachSSAInstruction : instructionUseAliasField){
                                    if(eachSSAInstruction instanceof SSAInvokeInstruction){
                                        SSAInvokeInstruction ssaInvokeInstruction1 = (SSAInvokeInstruction) eachSSAInstruction;
                                        if(ssaInvokeInstruction1.getDeclaredTarget().getSignature().contains("org.apache.hadoop.fs.FileContext.setUMask") && !isreach){
                                            successLog(curNode,ssaInvokeInstruction,ssaInvokeInstruction1);
                                            isreach = true;
                                        }
                                    }
                                }
                            }
                        }
                        int calleeuse = ssaInvokeInstruction.getUse(traceStack.get(i-1).para);
                        Iterator<SSAInstruction> var = curNode.getDU().getUses(calleeuse);
                        List<SSAInstruction> ssaInstructionSet = new ArrayList<>();
                        List<Integer> calleeuses = new ArrayList<>();
                        calleeuses.add(calleeuse);
                        for (Iterator<SSAInstruction> it = var; it.hasNext(); ) {
                            SSAInstruction ssaInstruction1 = it.next();
                            ssaInstructionSet.add(ssaInstruction1);
                        }
                        {
                            int calleeuseNum = curNode.getDU().getNumberOfUses(calleeuse);
                            if(calleeuseNum == 1){
                                Iterator<SSAInstruction> ssaInstructionIterator1 = var;
                                SSAInstruction ssaInstructionFirst = curNode.getDU().getDef(calleeuse);
                                SSAInstruction ssaInstructionSecon = ssaInstructionIterator1.next();
                                if(ssaInstructionFirst instanceof SSAGetInstruction){
                                    SSAGetInstruction ssaGetInstruction = (SSAGetInstruction) ssaInstructionFirst;
                                    Iterator<SSAInstruction> iterateAllInstructions = curNode.getIR().iterateAllInstructions();
                                    for (; iterateAllInstructions.hasNext(); ) {
                                        SSAInstruction instruction = iterateAllInstructions.next();
                                        if(instruction.equals(ssaInstructionSecon)){
                                            break;
                                        }
                                    }
                                    for(;iterateAllInstructions.hasNext();){
                                        SSAInstruction instruction = iterateAllInstructions.next();
                                        if(instruction instanceof SSAGetInstruction){
                                            SSAGetInstruction ssaNextGetInstruction = (SSAGetInstruction) instruction;
                                            if(ssaNextGetInstruction.getUse(0) == ssaGetInstruction.getUse(0) && ssaNextGetInstruction.getDeclaredField().equals(ssaGetInstruction.getDeclaredField()) && !ssaNextGetInstruction.equals(ssaGetInstruction)){
                                                curNode.getDU().getUses(ssaNextGetInstruction.getDef()).next();
                                                ssaInstructionSet.add(curNode.getDU().getUses(ssaNextGetInstruction.getDef()).next());
                                                calleeuses.add(ssaNextGetInstruction.getDef());
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        SSAInstruction ssaInstruction1;
                        int varindex = 0;
                        for (; varindex < ssaInstructionSet.size(); ) {
                            ssaInstruction1 = ssaInstructionSet.get(varindex);
                            varindex++;
                            if(ssaInstruction1 instanceof SSAInvokeInstruction){
                                SSAInvokeInstruction ssaInvokeInstruction1 = (SSAInvokeInstruction) ssaInstruction1;
                                if(ssaInvokeInstruction1.equals(ssaInvokeInstruction)){
                                    break;
                                }
                            }
                        }
                        SSAInstruction ssaInstruction2;
                        for (; varindex < ssaInstructionSet.size(); ) {
                            ssaInstruction2 = ssaInstructionSet.get(varindex);
                            varindex++;
                            if(ssaInstruction2 instanceof SSAInvokeInstruction){
                                SSAInvokeInstruction ssaInvokeInstruction1 = (SSAInvokeInstruction) ssaInstruction2;
                                for (MethodReference methodReference : permMethodRef) {
                                    if (methodReference.getSelector().toString().equals(ssaInvokeInstruction1.getDeclaredTarget().getSelector().toString()) && !isreach) {
                                        successLog(curNode, ssaInvokeInstruction, ssaInvokeInstruction1);
                                        isreach = true;
                                    }
                                }
                                for(int j=0;j<ssaInvokeInstruction1.getNumberOfUses() && !isreach;j++){
                                    if(calleeuses.contains(ssaInvokeInstruction1.getUse(j))){
                                        if(traceinto(ssaInvokeInstruction1,j+1,permMethodRef,new HashSet<>()) && !isreach){
                                            successLog(curNode,ssaInvokeInstruction,ssaInvokeInstruction1);
                                            isreach = true;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        CGNode curNode = gencallgraph.allNode.get(callerMethodReference.getSignature());
        Iterator<SSAInstruction> instructionIterator = curNode.getIR().iterateAllInstructions();
        for (Iterator<SSAInstruction> it = instructionIterator; it.hasNext() && !isreach; ) {
            SSAInstruction ssaInstruction = it.next();
            if(ssaInstruction instanceof SSAInvokeInstruction){
                SSAInvokeInstruction ssaInvokeInstruction = (SSAInvokeInstruction) ssaInstruction;
                if(ssaInvokeInstruction.getDeclaredTarget().getSelector().equals(calleeMethodReference.get(calleeMethodReference.size() - 1).getSelector())){
                    {
                        if(ssaInvokeInstruction.getDeclaredTarget().getDeclaringClass().toString().replace("/",".").contains("org.apache.hadoop.fs.FileContext")){
                            int use = ssaInvokeInstruction.getUse(0);
                            List<Integer> aliasFieldUseNum = aliasField(curNode,use);
                            Set<SSAInstruction> instructionUseAliasField = new HashSet<>();
                            for (Integer integer : aliasFieldUseNum) {
                                Iterator<SSAInstruction> curAliasFieldNumInstructions = curNode.getDU().getUses(integer);
                                for (; curAliasFieldNumInstructions.hasNext(); ) {
                                    SSAInstruction curAliasFieldNumInstruction = curAliasFieldNumInstructions.next();
                                    instructionUseAliasField.add(curAliasFieldNumInstruction);
                                }
                                if (integer == use)
                                    break;
                            }
                            for(SSAInstruction eachSSAInstruction : instructionUseAliasField){
                                if(eachSSAInstruction instanceof SSAInvokeInstruction){
                                    SSAInvokeInstruction ssaInvokeInstruction1 = (SSAInvokeInstruction) eachSSAInstruction;
                                    if(ssaInvokeInstruction1.getDeclaredTarget().getSignature().contains("org.apache.hadoop.fs.FileContext.setUMask") && !isreach){
                                        successLog(curNode,ssaInvokeInstruction,ssaInvokeInstruction1);
                                        isreach = true;
                                    }
                                }
                            }
                        }
                    }
                    int calleeuse = ssaInvokeInstruction.getUse(traceStack.get(traceStack.size() - 2).para);
                    Iterator<SSAInstruction> iterator = curNode.getDU().getUses(calleeuse);
                    List<SSAInstruction> ssaInstructionSet = new ArrayList<>();
                    List<Integer> calleeuses = new ArrayList<>();
                    calleeuses.add(calleeuse);
                    for (Iterator<SSAInstruction> its = iterator; its.hasNext(); ) {
                        SSAInstruction ssaInstruction1 = its.next();
                        ssaInstructionSet.add(ssaInstruction1);
                    }
                    {
                        int calleeuseNum = curNode.getDU().getNumberOfUses(calleeuse);
                        if(calleeuseNum == 1){
                            Iterator<SSAInstruction> ssaInstructionIterator1 = curNode.getDU().getUses(calleeuse);
                            SSAInstruction ssaInstructionFirst = curNode.getDU().getDef(calleeuse);
                            SSAInstruction ssaInstructionSecon = ssaInstructionIterator1.next();
                            if(ssaInstructionFirst instanceof SSAGetInstruction){
                                SSAGetInstruction ssaGetInstruction = (SSAGetInstruction) ssaInstructionFirst;
                                Iterator<SSAInstruction> iterateAllInstructions = curNode.getIR().iterateAllInstructions();
                                for (; iterateAllInstructions.hasNext(); ) {
                                    SSAInstruction instruction = iterateAllInstructions.next();
                                    if(instruction.equals(ssaInstructionSecon)){
                                        break;
                                    }
                                }
                                for(;iterateAllInstructions.hasNext();){
                                    SSAInstruction instruction = iterateAllInstructions.next();
                                    if(instruction instanceof SSAGetInstruction){
                                        SSAGetInstruction ssaNextGetInstruction = (SSAGetInstruction) instruction;
                                        if(ssaNextGetInstruction.getUse(0) == ssaGetInstruction.getUse(0) && ssaNextGetInstruction.getDeclaredField().equals(ssaGetInstruction.getDeclaredField()) && !ssaNextGetInstruction.equals(ssaGetInstruction)){
                                            curNode.getDU().getUses(ssaNextGetInstruction.getDef()).next();
                                            ssaInstructionSet.add(curNode.getDU().getUses(ssaNextGetInstruction.getDef()).next());
                                            calleeuses.add(ssaNextGetInstruction.getDef());
                                        }
                                    }
                                }
                            }
                        }
                    }
                    boolean isfind = false;
                    SSAInvokeInstruction ssaInvokeInstruction1 = null;
                    int iteratorindex = 0;
                    for(;iteratorindex < ssaInstructionSet.size();){
                        SSAInstruction ssaInstruction1 = ssaInstructionSet.get(iteratorindex);
                        iteratorindex++;
                        if(ssaInstruction1 instanceof SSAInvokeInstruction){
                            ssaInvokeInstruction1 = (SSAInvokeInstruction) ssaInstruction1;
                            if(ssaInvokeInstruction1.equals(ssaInvokeInstruction)){
                                break;
                            }
                        }
                    }
                    for(;iteratorindex < ssaInstructionSet.size() && !isreach;){
                        SSAInstruction ssaInstruction1 = ssaInstructionSet.get(iteratorindex);
                        iteratorindex++;
                        if(ssaInstruction1 instanceof SSAInvokeInstruction){
                            SSAInvokeInstruction ssaInvokeInstruction2 = (SSAInvokeInstruction) ssaInstruction1;
                            for (MethodReference methodReference : permMethodRef) {
                                if (methodReference.getSelector().toString().equals(ssaInvokeInstruction2.getDeclaredTarget().getSelector().toString()) && !isreach) {
                                    successLog(curNode, ssaInvokeInstruction, ssaInvokeInstruction2);
                                    isreach = true;
                                }
                            }
                            for(int j=0;j<ssaInvokeInstruction2.getNumberOfUses();j++){
                                if(calleeuses.contains(ssaInvokeInstruction2.getUse(j))){
                                    if(traceinto(ssaInvokeInstruction2,j+1,permMethodRef,new HashSet<>()) && !isreach){
                                        successLog(curNode,ssaInvokeInstruction,ssaInvokeInstruction2);
                                        isreach = true;
                                    }
                                }
                            }
                        }
                    }
                    if(!isreach){
                        errorLog(callerMethodReference,traceStack,calleeMethodReference,curNode,ssaInvokeInstruction);
                    }
                }
            }
        }
    }
    public static boolean traceinto(SSAInvokeInstruction ssaInvokeInstruction,int index,List<MethodReference> permMethodRef,Set<SSAInvokeInstruction> stack) throws IOException {
        CGNode curNode = gencallgraph.allNode.get(ssaInvokeInstruction.getDeclaredTarget().getSignature());
        if(stack.contains(ssaInvokeInstruction)){
            return false;
        }
        Set<SSAInvokeInstruction> curstack = new HashSet<>();
        curstack.addAll(stack);
        curstack.add(ssaInvokeInstruction);
        if(curNode == null){
            return false;
        }
        IR ir = curNode.getIR();
        if(ir == null){
            return false;
        }
        DefUse du = curNode.getDU();
        if(du == null){
            return false;
        }
        Iterator<SSAInstruction> ssaInstructionIterator = du.getUses(index);
        for(;ssaInstructionIterator.hasNext();){
            SSAInstruction ssaInstruction = ssaInstructionIterator.next();
            if(ssaInstruction instanceof SSAInvokeInstruction){
                SSAInvokeInstruction ssaInvokeInstruction1 = (SSAInvokeInstruction) ssaInstruction;
                for(MethodReference methodReference : permMethodRef){
                    if(methodReference.getSelector().equals(ssaInvokeInstruction1.getDeclaredTarget().getSelector())){
                        return true;
                    }
                }
                for(int j=0;j<ssaInvokeInstruction1.getNumberOfUses();j++){
                    if(ssaInvokeInstruction1.getUse(j) == index){
                        if(traceinto(ssaInvokeInstruction1,j+1,permMethodRef,curstack))
                            return true;
                    }
                }
            }
        }
        return false;
    }
    public static List<Integer> aliasField(CGNode node , int useNum){
        if(node == null)
            return null;
        IR ir = node.getIR();
        if(ir == null)
            return null;
        DefUse defUse = node.getDU();
        Iterator<SSAInstruction> allUseInstruction = defUse.getUses(useNum);
        SSAInstruction defInstruction = defUse.getDef(useNum);
        List<SSAInstruction> allUseInstructionList = new ArrayList<>();
        for (Iterator<SSAInstruction> it = allUseInstruction; it.hasNext(); ) {
            SSAInstruction ssaInstruction = it.next();
            allUseInstructionList.add(ssaInstruction);
        }
        List<Integer> res = new ArrayList<>();
        if(defInstruction instanceof SSAGetInstruction && allUseInstructionList.size() == 1){
            Iterator<SSAInstruction> allNodeInstruction = ir.iterateAllInstructions();
            for(;allNodeInstruction.hasNext();){
                SSAInstruction ssaInstruction = allNodeInstruction.next();
                if(ssaInstruction instanceof SSAGetInstruction){
                    SSAGetInstruction ssaGetInstruction = (SSAGetInstruction) ssaInstruction;
                    if(ssaGetInstruction.getDeclaredField().equals(((SSAGetInstruction) defInstruction).getDeclaredField()) && ssaGetInstruction.getUse(0) == defInstruction.getUse(0)){
                        res.add(ssaGetInstruction.getDef());
                    }
                }
            }
        }else{
            res.add(useNum);
        }
        return res;
    }
    public static void dealwithFileContext(){
    }
    public static void successLog(CGNode curNode,SSAInvokeInstruction ssaInvokeInstructionCaller,SSAInvokeInstruction ssaInvokeInstructionCallee) throws IOException {
        if(!curNode.getMethod().getSignature().startsWith("org.apache." + Setting.analysisProjectName) && !curNode.getMethod().getSignature().startsWith("org.apache.hadoop." + Setting.analysisProjectName)){
            return;
        }
        IR ir = curNode.getIR();
        int lineNumber = getLineNumber(ir,ssaInvokeInstructionCallee);
        boolean hasEdge = false;
        ControlDependenceGraph<ISSABasicBlock> cdg = new ControlDependenceGraph(ir.getControlFlowGraph(),true);
        ISSABasicBlock issaBasicBlockCaller = ir.getBasicBlockForInstruction(ssaInvokeInstructionCaller);
        ISSABasicBlock issaBasicBlockCallee = ir.getBasicBlockForInstruction(ssaInvokeInstructionCallee);
        Iterator<ISSABasicBlock> var2 = ir.getBlocks();
        boolean res = hasEdge(cdg,issaBasicBlockCaller,issaBasicBlockCallee);
        logger.log(logger.State.SUCCESS,"Find result : " + String.valueOf(res) +
                " find " + curNode.getMethod().getSignature() + "use " + ssaInvokeInstructionCaller.getDeclaredTarget().getSignature() + "to create in line and setPermission " +
                getLineNumber(ir,ssaInvokeInstructionCaller) + " use " + curNode.getMethod().getSignature() + " in line " + lineNumber);
    }
    public static boolean hasEdge(ControlDependenceGraph<ISSABasicBlock> cdg,ISSABasicBlock caller , ISSABasicBlock callee){
        Iterator<ISSABasicBlock> succs = cdg.getSuccNodes(caller);
        int num = caller.getNumber();
        for (Iterator<ISSABasicBlock> it = succs; it.hasNext(); ) {
            ISSABasicBlock succ = it.next();
            if(succ.getNumber()<= num)
                continue;
            if(callee.equals(succ)){
                return true;
            }
            if(hasEdge(cdg,succ,callee))
                return true;
        }
        return false;
    }
    public static void errorLog(MethodReference callerMethodRef,List<Trace> traceStack,List<MethodReference> calleeMethodReference,CGNode curnode , SSAInvokeInstruction ssaInvokeInstruction) throws IOException {
        if(!callerMethodRef.getSignature().startsWith("org.apache." + Setting.analysisProjectName) && !callerMethodRef.getSignature().startsWith("org.apache.hadoop." + Setting.analysisProjectName)){
            return;
        }
        for(MethodReference methodReference : calleeMethodReference){
            if(!blackList(methodReference))
                return;
        }
        totalbugs++;
        logger.log(logger.State.ERROR,"Find result : Can not find " + callerMethodRef.getSignature() + " setPermission");
        IR ir = curnode.getIR();
        int lineNumber = getLineNumber(ir,ssaInvokeInstruction);
        logger.log(logger.State.ERROR,"\t" + curnode.getMethod().getSignature() + " : " + lineNumber);
        List<MethodReference> refstack = new ArrayList<>();
        refstack.addAll(calleeMethodReference);
        refstack.add(callerMethodRef);
        CGNode firstNode = gencallgraph.allNode.get(callerMethodRef.getSignature());
        SSAInstruction firstInstruction = traceStack.get(traceStack.size()-1).ssaInstruction;
        SSAInvokeInstruction ssaInvokeInstruction1 = (SSAInvokeInstruction) traceStack.get(1).ssaInstruction;
        if(ssaInvokeInstruction1.getDeclaredTarget().getSignature().startsWith("org.apache.hadoop.")){
            hdfsbugs++;
        }else if(ssaInvokeInstruction1.getDeclaredTarget().getSignature().startsWith("java.io.") || ssaInvokeInstruction1.getDeclaredTarget().getSignature().startsWith("sun.nio.")){
            javabugs++;
        }
        for(int i=1;i<refstack.size()-1;i++){
            CGNode node = gencallgraph.allNode.get(refstack.get(refstack.size()-1-i).getSignature());
            SSAInstruction ssaInstruction = traceStack.get(traceStack.size()-1-i).ssaInstruction;
            int lineNum = getLineNumber(node.getIR(),ssaInstruction);
            logger.log(logger.State.ERROR,"\t" + node.getMethod().getSignature() + " : " + lineNum);
        }
    }
    public static int getLineNumber(IR ir, SSAInstruction inst) {
        IBytecodeMethod method = (IBytecodeMethod) ir.getMethod();
        int bytecodeIndex = -1;
        try {
            bytecodeIndex = method.getBytecodeIndex(inst.iIndex());
        } catch (InvalidClassFileException e) {
            e.printStackTrace();
            System.exit(1);
        }
        return method.getLineNumber(bytecodeIndex);
    }
    public static boolean blackList(MethodReference methodReference){
        if(methodReference.getSignature().contains("Files.copy("))
            return false;
        else if(methodReference.getSignature().contains("Files.move("))
            return false;
        return true;
    }
}
