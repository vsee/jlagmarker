# Define a variable for classpath
CLASS_PATH = ../bin

# Define a virtual path for .class in the bin directory
vpath %.class $(CLASS_PATH)

all : libhello.so

# $@ matches the target, $< matches the first dependancy
libhello.so : HelloJNI.o
	gcc -Wl,-soname,hello.so -shared -o $@ $<
	
# $@ matches the target, $< matches the first dependancy
HelloJNI.o : HelloJNI.c HelloJNI.h
	gcc -I"/usr/local/java/jdk1.8.0_25/include" -I"/usr/local/java/jdk1.8.0_25/include/linux" -Wall -Werror -fpic -c $< -o $@
	
# $* matches the target filename without the extension
HelloJNI.h : HelloJNI.class
	javah -classpath $(CLASS_PATH) $*
	
clean :
	rm HelloJNI.h HelloJNI.o libhello.so