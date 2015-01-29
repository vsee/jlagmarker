CC = gcc

CCFLAGS = -Wall -Werror -fPIC

INCLUDES = -I"/usr/lib/jvm/java-8-oracle/include/" -I"/usr/lib/jvm/java-8-oracle/include/linux" -I"../lib/libav-0.8.16/include"
LDFLAGS = -L../lib/libav-0.8.16/lib/ -lavformat -lavcodec -lavutil -lswscale
# -lavdevice -lavfilter 
 

# Define a variable for classpath
CLASS_PATH = ../bin

NATIVELIBNAMES = VideoState
NLIBS = $(addprefix lib, $(addsuffix .so, $(NATIVELIBNAMES)))
NLIBSRC = $(addsuffix .c, $(NATIVELIBNAMES))
NLIBOBJ = $(addsuffix .o, $(NATIVELIBNAMES))
NLIBHEADER = $(addsuffix .h, $(NATIVELIBNAMES))
NLIBCLASS = $(addsuffix .class, $(NATIVELIBNAMES))


# Define a virtual path for .class in the bin directory
vpath %.class $(CLASS_PATH)

all : buildLibs

buildLibs : buildObj
	for lib in $(NATIVELIBNAMES) ; do \
		$(CC) -shared -Wl,-Bsymbolic -o $(addprefix lib, $(addsuffix .so, $$lib)) $(addsuffix .o, $$lib) $(LDFLAGS) ; \
	done
	
buildObj : $(NLIBSRC) genNativeHeaders
	for lib in $(NATIVELIBNAMES) ; do \
		$(CC) $(INCLUDES) $(CCFLAGS) -c $(addsuffix .c, $$lib) -o $(addsuffix .o, $$lib) ; \
	done
	
genNativeHeaders : $(NLIBCLASS)
	javah -classpath $(CLASS_PATH) $(NATIVELIBNAMES)
	
clean :
	rm $(NLIBS) $(NLIBOBJ) $(NLIBHEADER)
