CC = gcc

CCFLAGS = -Wall -Werror -fpic

#INCLUDES = -I"/usr/local/java/jdk1.8.0_25/include" -I"/usr/local/java/jdk1.8.0_25/include/linux"
INCLUDES = -I"/usr/lib/jvm/java-8-oracle/include/" -I"/usr/lib/jvm/java-8-oracle/include/linux"

# Define a variable for classpath
CLASS_PATH = ../bin

NATIVELIBNAMES = LagmarkerNative HelloJNI
NLIBS = $(addprefix lib, $(addsuffix .so, $(NATIVELIBNAMES)))
NLIBSRC = $(addsuffix .c, $(NATIVELIBNAMES))
NLIBOBJ = $(addsuffix .o, $(NATIVELIBNAMES))
NLIBHEADER = $(addsuffix .h, $(NATIVELIBNAMES))
NLIBCLASS = $(addsuffix .class, $(NATIVELIBNAMES))


# Define a virtual path for .class in the bin directory
vpath %.class $(CLASS_PATH)

all : buildLibs

# $@ matches the target, $< matches the first dependancy
buildLibs : buildObj
	for lib in $(NATIVELIBNAMES) ; do \
		$(CC) -shared -o $(addprefix lib, $(addsuffix .so, $$lib)) $(addsuffix .o, $$lib) ; \
	done
	
# $@ matches the target, $< matches the first dependancy
buildObj : $(NLIBSRC) genNativeHeaders
	for lib in $(NATIVELIBNAMES) ; do \
		$(CC) $(INCLUDES) $(CCFLAGS) -c $(addsuffix .c, $$lib) -o $(addsuffix .o, $$lib) ; \
	done
	
genNativeHeaders : $(NLIBCLASS)
	javah -classpath $(CLASS_PATH) $(NATIVELIBNAMES)
	
clean :
	rm $(NLIBS) $(NLIBOBJ) $(NLIBHEADER)
