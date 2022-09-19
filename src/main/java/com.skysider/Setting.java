package com.skysider;

public class Setting {
    // output will write to {logDir}/{analysisProjectName}
    public static String logDir = "";
    // Distributed File System need hadoop project
    public static String hadoopProjectDir = "/home/linux/MFPChecker/onlyhadoop";
    // Project location
    public static String analysisProjectDir = "";
    //Project name, such as hadoop,hbase,tez
    public static String analysisProjectName = "";

    public static String exclution = "";
    public static void setAnalysisProjectDir(String analysisProjectDir) {
        Setting.analysisProjectDir = analysisProjectDir;
    }

    public static void setAnalysisProjectName(String analysisProjectName) {
        Setting.analysisProjectName = analysisProjectName;
    }

    public static void setExclution(String exclution) {
        Setting.exclution = exclution;
    }

    public static void setLogDir(String logDir) {
        Setting.logDir = logDir;
    }
}
