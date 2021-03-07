.PHONY:all clean
SRC := src/main/c
TARGET := target/obj

C_FLAGS := -I /usr/lib/jvm/default-java/include/ -I /usr/lib/jvm/default-java/include/linux/  -c -fPIC 

all: $(TARGET)/libnative.so $(TARGET)/main

$(SRC)/com_github_ruediste_laserPcb_jni_HelloWorldJNI.h:src/main/java/com/github/ruediste/laserPcb/jni/HelloWorldJNI.java
	javac -h $(SRC) $?
	
$(TARGET)/impl.o: $(SRC)/impl.cpp $(SRC)/com_github_ruediste_laserPcb_jni_HelloWorldJNI.h ${SRC}/print.h ${SRC}/showImage.h
	mkdir -p $(TARGET)
	g++ ${C_FLAGS} -o $@ $<

$(TARGET)/showImage.o: $(SRC)/showImage.cpp $(SRC)/showImage.h
	mkdir -p $(TARGET)
	g++ ${C_FLAGS} -o $@ $<

$(TARGET)/libnative.so: $(TARGET)/impl.o $(TARGET)/showImage.o
	g++ -shared -fPIC -o $@ $^ -lc -lstdc++ -lCGAL -lCGAL_Core -lmpfr

$(TARGET)/main: $(TARGET)/impl.o $(TARGET)/showImage.o
	g++ -o $@ $^ -lc -lstdc++ -lCGAL -lCGAL_Core -lmpfr -lgmp -lpthread -lboost_system  -lboost_thread -lX11
	
clean:
	rm -rf $(TARGET)