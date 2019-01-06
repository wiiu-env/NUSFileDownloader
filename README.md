# NUSFileDownloader - JNUSLib File Remote Decryptor
This is a little tool for downloading single files from the NUS Server based on [JNUSLib](https://github.com/Maschell/JNUSLib)

# Usage
Create a file `common.key` which contains the retail Wii U common key in binary.  
Example: Downloading the coreinit.rpl for system version 5.3.2 into the folder `tmp/532`.  
`java -jar FileDownloader.jar -titleID 000500101000400A -file '.*coreinit.rpl' -version 11464 -out tmp/532`  

You can use `java -jar FileDownloader.jar --help` for more usage instructions.

# Building
Requires maven and the Java 8 JDK.  
`mvn clean assembly:single package`
