CC = gcc

CCFLAGS = -Wall -Werror -fPIC

INCLUDES = -I"/usr/lib/jvm/java-8-oracle/include/" -I"/usr/lib/jvm/java-8-oracle/include/linux" -I"../lib/libav-0.8.16/include"
LDFLAGS = -L../lib/libav-0.8.16/lib/ -lavformat -lavcodec -lavutil -lswscale
# -lavdevice -lavfilter 
 

# Define a variable for classpath
CLASS_PATH = ../bin

# Define a virtual path for .class in the bin directory
vpath %.class $(CLASS_PATH)

all : buildLibs

buildLibs : buildObj
	$(CC) -shared -Wl,-Bsymbolic -o libVideoState.so VideoState.o $(LDFLAGS)
	
buildObj : VideoState.c genNativeHeaders
	$(CC) $(INCLUDES) $(CCFLAGS) -c VideoState.c -o VideoState.o
	
genNativeHeaders : mobileworkloads/jlagmarker/video/VideoState.class
	javah -classpath $(CLASS_PATH) mobileworkloads.jlagmarker.video.VideoState
	
clean :
	rm libVideoState.so VideoState.o mobileworkloads_jlagmarker_video_VideoState.h
