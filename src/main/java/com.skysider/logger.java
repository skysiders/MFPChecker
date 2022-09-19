package com.skysider;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class logger {
    public static int classnum;
    public static int methodnum;
    public static long timeuse;
    public static int totalbug;
    public static int hdfsbug;
    public static int javabug;
    public enum State{
        SUCCESS,
        INFO,
        ERROR,

    }
    public static FileOutputStream successStream;
    public static FileOutputStream infoSTream;
    public static FileOutputStream errorStream;
    public static void log(String str) throws IOException {
        log(State.INFO,str);
    }
    public static void setInit(String basePath, String name){
        String basedir = basePath + name;
        File file = new File(basedir);
        if(!file.exists()){
            file.mkdirs();
        }else{
            file.delete();
            file.mkdirs();
        }
            try {
                successStream = new FileOutputStream(basedir + "/successlog.log");
                infoSTream = new FileOutputStream(basedir + "/info.log");
                errorStream = new FileOutputStream(basedir + "/errorlog.log");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
    }
    public static void log(State type, String str,boolean end) throws IOException {
        if(!str.endsWith("\n") && end)
            str += "\n";
        if(type == State.SUCCESS){
            successStream.write(str.getBytes());
        }else if(type == State.INFO){
            infoSTream.write(str.getBytes());
        }else if(type == State.ERROR){
            errorStream.write(str.getBytes());
        }
    }
    public static void log(FileOutputStream fos, String str,boolean end) throws IOException {
        if(!str.endsWith("\n") && end)
            str += "\n";
        fos.write(str.getBytes());
    }
    public static void log(State type,String str) throws IOException {
        log(type,str,true);
    }
}
