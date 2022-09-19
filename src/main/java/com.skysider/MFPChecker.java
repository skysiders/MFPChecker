package com.skysider;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.CancelException;

import java.io.File;
import java.io.IOException;
import java.util.*;
public class MFPChecker {

    private static Set<IClass> LocalFileSystemClasses = new HashSet<>();
    private static Set<IClass> RemoteFileSystemClasses = new HashSet<>();
    public static String projName;
    public static void testforhadoop(String pathname,String projname) throws ClassHierarchyException, CancelException, IOException {
        long startTime = System.nanoTime();
        projName = projname;
        String path = gencallgraph.genJarPaths(Setting.analysisProjectDir);
        path += gencallgraph.genJarPaths(Setting.hadoopProjectDir);
        logger.log(logger.State.INFO,"Analysis Files :");
        for(String name : path.split(File.pathSeparator)){
            logger.log(logger.State.INFO,"\t"+name);
        }
        logger.log(logger.State.INFO,"Number of Files  : " + path.split(File.pathSeparator).length);
        gencallgraph cgs = new gencallgraph(path);
        logger.log(logger.State.INFO,"Number of Class  : " + gencallgraph.str2ClassRef.keySet().size());
        logger.log(logger.State.INFO,"Number of Method : " + gencallgraph.str2MethodRef.keySet().size());
        logger.log(logger.State.INFO,"Time for generate Call Graph : " + (System.nanoTime() - startTime));
        logger.classnum = gencallgraph.str2ClassRef.keySet().size();
        logger.methodnum = gencallgraph.str2MethodRef.keySet().size();
        RemoteFileSystemClasses.addAll(cgs.findSubclasses(gencallgraph.ref2Class.get(gencallgraph.str2ClassRef.get("<Application,Lorg/apache/hadoop/fs/FileSystem>"))));
        RemoteFileSystemClasses.remove(gencallgraph.ref2Class.get(gencallgraph.str2ClassRef.get("<Application,Lorg/apache/hadoop/fs/FileSystem>")));
        Map<MethodReference,Integer> hadoopEndMethod = new HashMap<>();
        List<MethodReference> permission = new ArrayList<>();
        boolean ishadoop = true;
        if(ishadoop) {
            hadoopEndMethod.put(gencallgraph.str2MethodRef.get("org.apache.hadoop.fs.FileContext.create(Lorg/apache/hadoop/fs/Path;Ljava/util/EnumSet;[Lorg/apache/hadoop/fs/Options$CreateOpts;)Lorg/apache/hadoop/fs/FSDataOutputStream;"), 1);
            hadoopEndMethod.put(gencallgraph.str2MethodRef.get("org.apache.hadoop.hdfs.DistributedFileSystem.primitiveCreate(Lorg/apache/hadoop/fs/Path;Lorg/apache/hadoop/fs/permission/FsPermission;Ljava/util/EnumSet;ISJLorg/apache/hadoop/util/Progressable;Lorg/apache/hadoop/fs/Options$ChecksumOpt;)Lorg/apache/hadoop/hdfs/client/HdfsDataOutputStream;"), 1);
            hadoopEndMethod.put(gencallgraph.str2MethodRef.get("org.apache.hadoop.hdfs.DistributedFileSystem.create(Lorg/apache/hadoop/fs/Path;Lorg/apache/hadoop/fs/permission/FsPermission;ZISJLorg/apache/hadoop/util/Progressable;[Ljava/net/InetSocketAddress;)Lorg/apache/hadoop/hdfs/client/HdfsDataOutputStream;"), 1);
            hadoopEndMethod.put(gencallgraph.str2MethodRef.get("org.apache.hadoop.hdfs.DistributedFileSystem.create(Lorg/apache/hadoop/fs/Path;Lorg/apache/hadoop/fs/permission/FsPermission;Ljava/util/EnumSet;ISJLorg/apache/hadoop/util/Progressable;Lorg/apache/hadoop/fs/Options$ChecksumOpt;)Lorg/apache/hadoop/fs/FSDataOutputStream;"), 1);
            hadoopEndMethod.put(gencallgraph.str2MethodRef.get("org.apache.hadoop.hdfs.DistributedFileSystem.createNonRecursive(Lorg/apache/hadoop/fs/Path;Lorg/apache/hadoop/fs/permission/FsPermission;Ljava/util/EnumSet;ISJLorg/apache/hadoop/util/Progressable;)Lorg/apache/hadoop/fs/FSDataOutputStream;"), 1);
            hadoopEndMethod.put(gencallgraph.str2MethodRef.get("org.apache.hadoop.hdfs.DistributedFileSystem.create(Lorg/apache/hadoop/fs/Path;Lorg/apache/hadoop/fs/permission/FsPermission;Ljava/util/EnumSet;ISJLorg/apache/hadoop/util/Progressable;Lorg/apache/hadoop/fs/Options$ChecksumOpt;[Ljava/net/InetSocketAddress;Ljava/lang/String;)Lorg/apache/hadoop/hdfs/client/HdfsDataOutputStream;"), 1);
            hadoopEndMethod.put(gencallgraph.str2MethodRef.get("org.apache.hadoop.hdfs.DistributedFileSystem.createNonRecursive(Lorg/apache/hadoop/fs/Path;Lorg/apache/hadoop/fs/permission/FsPermission;Ljava/util/EnumSet;ISJLorg/apache/hadoop/util/Progressable;Lorg/apache/hadoop/fs/Options$ChecksumOpt;[Ljava/net/InetSocketAddress;Ljava/lang/String;)Lorg/apache/hadoop/hdfs/client/HdfsDataOutputStream;"), 1);
            hadoopEndMethod.put(gencallgraph.str2MethodRef.get("org.apache.hadoop.fs.FileContext.mkdir(Lorg/apache/hadoop/fs/Path;Lorg/apache/hadoop/fs/permission/FsPermission;Z)V"), 1);
            hadoopEndMethod.put(gencallgraph.str2MethodRef.get("org.apache.hadoop.hdfs.DistributedFileSystem.mkdir(Lorg/apache/hadoop/fs/Path;Lorg/apache/hadoop/fs/permission/FsPermission;)Z"), 1);
            hadoopEndMethod.put(gencallgraph.str2MethodRef.get("org.apache.hadoop.hdfs.DistributedFileSystem.mkdirsInternal(Lorg/apache/hadoop/fs/Path;Lorg/apache/hadoop/fs/permission/FsPermission;Z)Z"), 1);
            hadoopEndMethod.put(gencallgraph.str2MethodRef.get("org.apache.hadoop.hdfs.DistributedFileSystem.primitiveMkdir(Lorg/apache/hadoop/fs/Path;Lorg/apache/hadoop/fs/permission/FsPermission;)Z"), 1);
            hadoopEndMethod.put(gencallgraph.str2MethodRef.get("org.apache.hadoop.hdfs.DistributedFileSystem.provisionEZTrash(Ljava/lang/String;Lorg/apache/hadoop/fs/permission/FsPermission;)V"), 1);
            hadoopEndMethod.put(gencallgraph.str2MethodRef.get("org.apache.hadoop.hdfs.DistributedFileSystem.mkdirs(Lorg/apache/hadoop/fs/Path;Lorg/apache/hadoop/fs/permission/FsPermission;)Z"), 1);
            permission.add(gencallgraph.str2MethodRef.get("org.apache.hadoop.fs.FileContext.setPermission(Lorg/apache/hadoop/fs/Path;Lorg/apache/hadoop/fs/permission/FsPermission;)V"));
            permission.add(gencallgraph.str2MethodRef.get("org.apache.hadoop.fs.FileSystem.setPermission(Lorg/apache/hadoop/fs/Path;Lorg/apache/hadoop/fs/permission/FsPermission;)V"));
        }
        hadoopEndMethod.put(gencallgraph.str2MethodRef.get("java.io.FileSystem.createFileExclusively(Ljava/lang/String;)Z"),1);
        hadoopEndMethod.put(gencallgraph.str2MethodRef.get("java.io.FileOutputStream.open0(Ljava/lang/String;Z)V"),1);
        hadoopEndMethod.put(gencallgraph.str2MethodRef.get("sun.nio.fs.UnixCopyFile.copyFile(Lsun/nio/fs/UnixPath;Lsun/nio/fs/UnixFileAttributes;Lsun/nio/fs/UnixPath;Lsun/nio/fs/UnixCopyFile$Flags;J)V"),2);
        hadoopEndMethod.put(gencallgraph.str2MethodRef.get("sun.nio.fs.UnixChannelFactory.open(ILsun/nio/fs/UnixPath;Ljava/lang/String;Lsun/nio/fs/UnixChannelFactory$Flags;I)Ljava/io/FileDescriptor;"),1);
        hadoopEndMethod.put(gencallgraph.str2MethodRef.get("java.io.FileSystem.createDirectory(Ljava/io/File;)Z"),1);
        hadoopEndMethod.put(gencallgraph.str2MethodRef.get("sun.nio.fs.UnixNativeDispatcher.mkdir(Lsun/nio/fs/UnixPath;I)V"),0);
        permission.add(gencallgraph.str2MethodRef.get("java.io.FileSystem.setPermission(Ljava/io/File;IZZ)Z"));
        permission.add(gencallgraph.str2MethodRef.get("sun.nio.fs.UnixFileAttributeViews$Posix.setPermissions(Ljava/util/Set;)V"));
        Set<MethodReference> localres = new HashSet<>();
        Set<MethodReference> killed = new HashSet<>();
        for(MethodReference methodReference : hadoopEndMethod.keySet()){
            Map<String,IClass> belong = new HashMap<>();
            belong.put("LocalFileSystem",null);
            belong.put("RemoteFileSystem",null);
            Trace t = new Trace(hadoopEndMethod.get(methodReference),null);
            List<Trace> traceStack = new ArrayList<>();
            traceStack.add(t);
            localres.addAll(cgs.getAllMethodFromEnd(methodReference,t, permission, killed,LocalFileSystemClasses,RemoteFileSystemClasses, belong, new ArrayList<>(), traceStack, 10, 0));
        }
        logger.log(logger.State.INFO,"Total bug : " + newDU.totalbugs);
        logger.log(logger.State.INFO,"HDFS  bug : " + newDU.hdfsbugs);
        logger.log(logger.State.INFO,"JAVA  bug : " + newDU.javabugs);
        logger.log(logger.State.INFO,"Time for MFPChecker : " + (System.nanoTime() - startTime));
        logger.timeuse = (System.nanoTime() - startTime);
        logger.totalbug = newDU.totalbugs;
        logger.hdfsbug =  newDU.hdfsbugs;
        logger.javabug = newDU.javabugs;
        logger.log(logger.State.INFO,"Time for MFPChecker : " + logger.classnum + " | " + logger.methodnum + " | "+ logger.timeuse + " | " + logger.totalbug + " | " + logger.hdfsbug + " | " + logger.javabug);
    }

    public static void main(String[] args) throws ClassHierarchyException, CancelException, IOException {
        Setting.setAnalysisProjectDir(args[0]);
        Setting.setAnalysisProjectName(args[1]);
        Setting.setExclution(args[2]);
        Setting.setLogDir(args[3]);
        logger.setInit(Setting.logDir,Setting.analysisProjectName);
        testforhadoop(Setting.analysisProjectDir,Setting.analysisProjectName);
    }
}
