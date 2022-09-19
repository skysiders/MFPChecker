package com.skysider;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.types.FieldReference;
import java.util.Stack;
public class Trace {
    public FieldReference fieldReference;
    public SSAInstruction ssaInstruction;
    public int para;
    public Stack<SSAInstruction> traceintoStack = new Stack<>();
    public Trace(int para, FieldReference fieldReference){
        this.para = para;
        this.fieldReference = fieldReference;
    }
    public Trace(int para, FieldReference fieldReference,Stack<SSAInstruction> stack){
        this.para = para;
        this.fieldReference = fieldReference;
        this.traceintoStack.addAll(stack);
    }
    public Trace(int para, FieldReference fieldReference , SSAInstruction ssaInstruction,Stack<SSAInstruction> stack){
        this.para = para;
        this.fieldReference = fieldReference;
        this.ssaInstruction = ssaInstruction;
        this.traceintoStack.addAll(stack);
    }
    public boolean inStack(SSAInstruction ssaInstruction){
        for (SSAInstruction instruction : traceintoStack) {
            if (instruction.toString().equals(ssaInstruction.toString()))
                return true;
        }
        return false;
    }
    public void push(SSAInstruction ssaInstruction){
        traceintoStack.push(ssaInstruction);
    }
    public void pop(){
        traceintoStack.pop();
    }
}
