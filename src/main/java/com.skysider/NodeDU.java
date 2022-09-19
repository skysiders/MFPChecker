package com.skysider;
import com.google.common.collect.Lists;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ssa.*;
import com.ibm.wala.types.MethodReference;
import fj.P;
import java.io.IOException;
import java.lang.invoke.CallSite;
import java.util.*;
public class NodeDU {
    public CGNode node;
    public List<SSAInstruction> ssaInstructions;
    public int ParameterSize;
    public DefUse defUse;
    public NodeDU(CGNode node){
        this.node = node;
        Iterator<SSAInstruction> var = this.node.getIR().iterateAllInstructions();
        for (Iterator<SSAInstruction> it = var; it.hasNext(); ) {
            SSAInstruction ssa = it.next();
            ssaInstructions.add(ssa);
        }
        this.ParameterSize = node.getMethod().getNumberOfParameters();
        this.defUse = node.getDU();
    }
    public static List<SSAInstruction> getSSAInstruction(CGNode node){
        if(node == null)
            return null;
        List<SSAInstruction> ssaInstructions = new ArrayList<>();
        Iterator<SSAInstruction> ssaInstructionIterator = node.getIR().iterateAllInstructions();
        for (Iterator<SSAInstruction> it = ssaInstructionIterator; it.hasNext(); ) {
            SSAInstruction ssaInstruction = it.next();
            ssaInstructions.add(ssaInstruction);
        }
        return ssaInstructions;
    }
    public static int getdef(CGNode node , MethodReference methodReference){
        List<SSAInstruction> res = getSSAInstruction(node);
        if(methodReference.isInit()){
        }
        for (SSAInstruction instruction : res) {
            if (instruction instanceof SSAInvokeInstruction) {
                if (((SSAInvokeInstruction) instruction).getDeclaredTarget()
                        .getSelector().equals(methodReference.getSelector())) {
                    System.out.println("curr ins is " + instruction + " idx " + instruction.getNumberOfDefs() + " " + instruction.getNumberOfUses());
                    int exception = ((SSAInvokeInstruction) instruction).getException();
                    for (int j = 0; j < instruction.getNumberOfDefs(); j++)
                        System.out.println(instruction.getDef(j));
                    return ((SSAInvokeInstruction) instruction).getDef(0);
                }
            }
        }
        return 0;
    }
    public static int specialmethod(DefUse defUse , int defination , int nodeparanums){
        return 0;
    }
    public static int isreachpara(DefUse defUse,int defination,int nodeparanums){
        System.out.println("cur defination is " + defination);
        SSAInstruction ssaInstruction = defUse.getDef(defination);
        if(defination == 1)
            return 0;
        if(defination <= nodeparanums && defination != 1)
            return defination;
        if(ssaInstruction == null){
            return 0;
        }
        System.out.println(ssaInstruction);
        System.out.println(ssaInstruction.getClass());
        System.out.println(ssaInstruction.getNumberOfUses());
        for(int i=0;i<ssaInstruction.getNumberOfUses();i++){
            System.out.print(ssaInstruction.getUse(i));
        }
        System.out.println("\n");
        if(ssaInstruction instanceof SSAPhiInstruction){
            System.out.println("in phi instruction use has " + ssaInstruction.getNumberOfUses() + " def has " + ssaInstruction.getNumberOfDefs());
            for(int i=0;i<ssaInstruction.getNumberOfUses();i++){
                System.out.println("in phi instruction current idx is " + ssaInstruction.getUse(i));
                int cur = isreachpara(defUse,ssaInstruction.getUse(i),nodeparanums);
                if(0 != cur){
                    return cur;
                }
            }
            return 0;
        }
        if(ssaInstruction instanceof SSANewInstruction){
            System.out.println("into new ins");
            Iterator<SSAInstruction> var = defUse.getUses(defination);
            for (Iterator<SSAInstruction> it = var; it.hasNext(); ) {
                SSAInstruction ssaInstruction1 = it.next();
                if(ssaInstruction1 instanceof SSAInvokeInstruction &&
                        ((SSAInvokeInstruction) ssaInstruction1).getDeclaredTarget().isInit() &&
                        defination == ssaInstruction1.getUse(0)){
                    SSAInvokeInstruction ssaInvokeInstruction = (SSAInvokeInstruction) ssaInstruction1;
                    System.out.println("sig is "+ssaInvokeInstruction.getDeclaredTarget().getSignature());
                    if(ssaInvokeInstruction.getDeclaredTarget().getSignature().contains("java.io.File.<init>")){
                        System.out.println("end find for File init " + ssaInvokeInstruction.getDeclaredTarget().getSignature());
                        return 0;
                    }
                    for(int i=1;i<ssaInstruction1.getNumberOfUses();i++){
                        if(ssaInstruction1.getUse(i) == defination)
                            continue;
                        int res = isreachpara(defUse,ssaInstruction1.getUse(i),nodeparanums);
                        if(0 != res){
                            return res;
                        }
                    }
                }
            }
            return 0;
        }
        if(ssaInstruction instanceof SSAGetInstruction){
            SSAGetInstruction ssaGetInstruction = (SSAGetInstruction) ssaInstruction;
            int thisnum = ssaGetInstruction.getUse(0);
            if(thisnum != 1){
            }else{
                System.out.println(ssaGetInstruction.getUse(0) + " with " + ssaGetInstruction.getDeclaredField());
                return 1;
            }
        }
        if(ssaInstruction instanceof SSAInvokeInstruction){
            System.out.println("into invoke ");
            SSAInvokeInstruction ssaInvokeInstruction = (SSAInvokeInstruction) ssaInstruction;
            invokeVitrual(ssaInvokeInstruction);
            if(ssaInvokeInstruction.getDeclaredTarget().getNumberOfParameters() == 0){
                return 0;
            }
            if(ssaInvokeInstruction.getDeclaredTarget().isInit()){
                System.out.println(ssaInvokeInstruction + " is init");
                for(int i=1;i<ssaInvokeInstruction.getNumberOfUses();i++){
                    int res = isreachpara(defUse,ssaInvokeInstruction.getUse(i),nodeparanums);
                    if(0 != res)
                        return res;
                }
                System.out.println(ssaInvokeInstruction + " init will return 0");
                return 0;
            }else{
                System.out.println(ssaInvokeInstruction + " is not init");
                for(int i=0;i<ssaInvokeInstruction.getNumberOfUses();i++){
                    int res = isreachpara(defUse,ssaInvokeInstruction.getUse(i),nodeparanums);
                    if(0 != res)
                        return res;
                }
                System.out.println(ssaInvokeInstruction + " not init will return 0");
                return 0;
            }
        }
        if(ssaInstruction.getNumberOfUses() == 0){
            System.out.println("into 0 uses");
            Iterator<SSAInstruction> ssaInstructionIterator = defUse.getUses(defination);
            for (Iterator<SSAInstruction> it = ssaInstructionIterator; it.hasNext(); ) {
                SSAInstruction ssaInstruction1 = it.next();
                if(ssaInstruction1 instanceof SSAInvokeInstruction &&
                        ((SSAInvokeInstruction) ssaInstruction1).isSpecial()){
                    for(int i=0;i<ssaInstruction1.getNumberOfUses();i++){
                        System.out.println("in line 81 goto next tern");
                        System.out.println(ssaInstruction1);
                        System.out.println(i);
                        if(ssaInstruction1.getUse(i) == 1)
                            continue;
                        int cur = isreachpara(defUse,ssaInstruction1.getUse(i),nodeparanums);
                        if(0 != cur)
                            return cur;
                    }
                }
            }
        }else{
            for(int i=0;i<ssaInstruction.getNumberOfUses();i++){
                if(ssaInstruction.getUse(i) == 1)
                    continue;
                int cur = isreachpara(defUse,ssaInstruction.getUse(i),nodeparanums);
                if(0 != cur)
                    return cur;
            }
        }
        return 0;
    }
    public static int isreachpara(CGNode node,int defination){
        int nodeparanums = node.getMethod().getNumberOfParameters();
        System.out.println("node has " + nodeparanums);
        System.out.println("def is " + defination);
        int cur = defination;
        return isreachpara(node.getDU(), cur, nodeparanums);
    }
    public static int isreachpara(CGNode callernode,MethodReference calleeMethodReference,int paraidx){
        int defidx = getdef(callernode,calleeMethodReference);
        if(defidx == 0){
            System.out.println("error in NodeDU isreachpara");
            return 0;
        }
        System.out.println("curr def idx is " + defidx);
        SSAInstruction ssaInstruction = callernode.getDU().getDef(defidx);
        if(ssaInstruction instanceof SSAInvokeInstruction){
            SSAInvokeInstruction ssaInvokeInstruction = (SSAInvokeInstruction) ssaInstruction;
            if(ssaInvokeInstruction.isStatic()){
                return isreachpara(callernode,ssaInvokeInstruction.getUse(paraidx));
            }else if(ssaInvokeInstruction.isSpecial()){
                return isreachpara(callernode,ssaInvokeInstruction.getUse(paraidx));
            }else{
                return isreachpara(callernode,ssaInvokeInstruction.getUse(paraidx));
            }
        }else{
            System.out.println("error in is reach " + ssaInstruction.getClass() + " ins is " + ssaInstruction);
            return 0;
        }
    }
    public static SSAInstruction getInstruction(CGNode node,MethodReference methodReference){
        Iterator<SSAInstruction> ssaInstructionIterator = node.getIR().iterateAllInstructions();
        for (Iterator<SSAInstruction> it = ssaInstructionIterator; it.hasNext(); ) {
            SSAInstruction ssaInstruction = it.next();
            if(ssaInstruction instanceof SSAInvokeInstruction){
                SSAInvokeInstruction ssaInvokeInstruction = (SSAInvokeInstruction) ssaInstruction;
                if(ssaInvokeInstruction.getDeclaredTarget().getSelector().equals(methodReference.getSelector())){
                    return ssaInstruction;
                }
            }
        }
        return null;
    }
    public static SSAInstruction traceReturnVaule(CGNode node){
        DefUse defUse = node.getDU();
        Iterator<SSAInstruction> ssaInstructionIterator = node.getIR().iterateAllInstructions();
        for (Iterator<SSAInstruction> it = ssaInstructionIterator; it.hasNext(); ) {
            SSAInstruction ssaInstruction = it.next();
            if(ssaInstruction instanceof SSAReturnInstruction){
                SSAInstruction Instruction = defUse.getDef(((SSAReturnInstruction) ssaInstruction).getResult());
                System.out.println("find return value " + Instruction);
                if(Instruction instanceof SSAGetInstruction){
                    System.out.println("find return value whth get ins");
                    SSAGetInstruction ssaGetInstruction = (SSAGetInstruction) Instruction;
                    return ssaGetInstruction;
                }else if (Instruction instanceof SSAInvokeInstruction){
                    System.out.println("find return value with invoke");
                    invokeVitrual((SSAInvokeInstruction) Instruction);
                }
            }
        }
        return null;
    }
    public static void invokeVitrual(SSAInvokeInstruction ssaInvokeInstruction){
        System.out.println("in method invoke Vitrual");
        if(ssaInvokeInstruction.isSpecial()){
            System.out.println(ssaInvokeInstruction);
        }else if(ssaInvokeInstruction.isStatic()) {
            int usenum = ssaInvokeInstruction.getNumberOfUses();
            System.out.println("goto ssa invoke static method");
            traceReturnVaule(gencallgraph.allNode.get(ssaInvokeInstruction.getDeclaredTarget().getSignature()));
        }else{
            System.out.println("is invoke virtual");
            CGNode node = gencallgraph.allNode.get(ssaInvokeInstruction.getDeclaredTarget().getSignature());
            SSAInstruction ssaInstruction = traceReturnVaule(node);
            int use = ssaInvokeInstruction.getUse(ssaInstruction.getUse(0)-1);
            while(use >= node.getMethod().getNumberOfParameters()){
                System.out.println("in while");
                SSAInstruction ssaInstruction1 = node.getDU().getDef(use);
                System.out.println("before check use is " + use + " ssains is " + ssaInstruction1);
                if(ssaInstruction1 instanceof SSAInvokeInstruction){
                    SSAInvokeInstruction ssaInvokeInstruction1 = (SSAInvokeInstruction) ssaInstruction1;
                    ssaInstruction = traceReturnVaule(gencallgraph.allNode.get(ssaInvokeInstruction1.getDeclaredTarget().getSignature()));
                    use = ssaInvokeInstruction.getUse(ssaInstruction.getUse(0)-1);
                    System.out.println("in while " + use + " " + ssaInstruction);
                }else{
                    break;
                }
            }
            if(use >= node.getMethod().getNumberOfParameters()){
                SSAInstruction ssaInstruction1 = node.getDU().getDef(use);
                if(ssaInstruction1 instanceof SSAInvokeInstruction){
                    SSAInvokeInstruction ssaInvokeInstruction1 = (SSAInvokeInstruction) ssaInstruction1;
                    traceReturnVaule(gencallgraph.allNode.get(ssaInvokeInstruction1.getDeclaredTarget().getSignature()));
                }
            }
            System.out.println("end for while use is " + use + " ssaIns is " + ssaInstruction);
            if(ssaInstruction instanceof  SSAGetInstruction){
                System.out.println(ssaInvokeInstruction.getUse(ssaInstruction.getUse(0)-1) + " " + ((SSAGetInstruction) ssaInstruction).getDeclaredField());
            }
        }
    }
}
