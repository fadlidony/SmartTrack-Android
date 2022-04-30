#include <jni.h>
#include <cstring>
#include <termios.h>
#include <cstdio>
#include <cassert>
#include <unistd.h>
#include <cerrno>
#include "android/log.h"
#define LOG_TAG "javen"
#define LOGD(fmt, args...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, fmt, ##args)
#define LOGE(fmt, args...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, fmt, ##args)

template<typename T>
class scoped_local_ref {
public:
	scoped_local_ref(C_JNIEnv* env, T localRef = NULL) :
			mEnv(env), mLocalRef(localRef) {
	}

	~scoped_local_ref() {
		reset();
	}

	void reset(T localRef = NULL) {
		if (mLocalRef != NULL) {
			(*mEnv)->DeleteLocalRef(reinterpret_cast<JNIEnv*>(mEnv), mLocalRef);
			mLocalRef = localRef;
		}
	}

	T get() const {
		return mLocalRef;
	}

private:
	C_JNIEnv* mEnv;
	T mLocalRef;

	// Disallow copy and assignment.
	scoped_local_ref(const scoped_local_ref&);
	void operator=(const scoped_local_ref&);
};

int jniGetFDFromFileDescriptor(C_JNIEnv* env, jobject fileDescriptor) {
	JNIEnv* e = reinterpret_cast<JNIEnv*>(env);
	scoped_local_ref<jclass> localClass(env,
			e->FindClass("java/io/FileDescriptor"));
	static jfieldID fid = e->GetFieldID(localClass.get(), "descriptor", "I");
	if (fileDescriptor != nullptr) {
		return (*env)->GetIntField(e, fileDescriptor, fid);
	} else {
		return -1;
	}
}

static bool native_modify_fd_attribute(JNIEnv *env, jobject thiz,
		jobject fileDescriptor, jint baudrate, jint dataBit, jint stopBit,
		jint parityBit) {
	LOGD(
			"native_modify_fd_attribute start baudrate=%d,dataBit=%d,stopBit=%d,parityBit=%d",
			baudrate, dataBit, stopBit, parityBit);
	switch (baudrate) {
	case 50:
		baudrate = B50;
		break;
	case 75:
		baudrate = B75;
		break;
	case 110:
		baudrate = B110;
		break;
	case 134:
		baudrate = B134;
		break;
	case 150:
		baudrate = B150;
		break;
	case 200:
		baudrate = B200;
		break;
	case 300:
		baudrate = B300;
		break;
	case 600:
		baudrate = B600;
		break;
	case 1200:
		baudrate = B1200;
		break;
	case 1800:
		baudrate = B1800;
		break;
	case 2400:
		baudrate = B2400;
		break;
	case 4800:
		baudrate = B4800;
		break;
	case 9600:
		baudrate = B9600;
		break;
	case 19200:
		baudrate = B19200;
		break;
	case 38400:
		baudrate = B38400;
		break;
	case 57600:
		baudrate = B57600;
		break;
	case 115200:
		baudrate = B115200;
		break;
	case 230400:
		baudrate = B230400;
		break;
	case 460800:
		baudrate = B460800;
		break;
	case 500000:
		baudrate = B500000;
		break;
	case 576000:
		baudrate = B576000;
		break;
	case 921600:
		baudrate = B921600;
		break;
	case 1000000:
		baudrate = B1000000;
		break;
	case 1152000:
		baudrate = B1152000;
		break;
	case 1500000:
		baudrate = B1500000;
		break;
	case 2000000:
		baudrate = B2000000;
		break;
	case 2500000:
		baudrate = B2500000;
		break;
	case 3000000:
		baudrate = B3000000;
		break;
	case 3500000:
		baudrate = B3500000;
		break;
	case 4000000:
		baudrate = B4000000;
		break;
	default:
		LOGD("baudrate error %d", baudrate);
		return false;
	}
	int fd = jniGetFDFromFileDescriptor(&env->functions, fileDescriptor);
	struct termios tio;
	memset(&tio, 0, sizeof(tio));
	if (tcgetattr(fd, &tio) != JNI_OK) {
		LOGE("tcgetattr failed errno=%s", strerror(errno));
		return false;
	}


//	tio.c_cflag = baudrate | CS8 | CLOCAL | CREAD;
//	// Disable output processing, including messing with end-of-line characters.
//	tio.c_oflag &= ~OPOST;
//	tio.c_iflag = IGNPAR;
//	tio.c_lflag = 0; /* turn of CANON, ECHO*, etc */
//	/* no timeout but request at least one character per read */
//	tio.c_cc[VTIME] = 0;
//	tio.c_cc[VMIN] = 0;
//	if (tcsetattr(fd, TCSANOW, &tio) != JNI_OK) {
//		LOGE("tcsetattr failed errno=%s", strerror(errno));
//		return false;
//	}
//	if (tcflush(fd, TCIFLUSH) != JNI_OK) {
//		LOGE("tcflush failed errno=%s", strerror(errno));
//		return false;
//	}

	cfmakeraw(&tio);
	cfsetispeed(&tio, baudrate);
	cfsetospeed(&tio, baudrate);
	if (tcsetattr(fd, TCSANOW, &tio) != JNI_OK) {
		LOGE("tcsetattr failed errno=%s", strerror(errno));
		return false;
	}
	if (tcflush(fd, TCIFLUSH) != JNI_OK) {
		LOGE("tcflush failed errno=%s", strerror(errno));
		return false;
	}
	LOGD("native_modify_fd_attribute end");
	return true;
}
static JNINativeMethod method_table[] =
		{ { "nativeModifyFdAttribute", "(Ljava/io/FileDescriptor;IIII)Z",
				(void *) native_modify_fd_attribute } };

static bool registerNative(JNIEnv *env) {
	jclass clazz = env->FindClass("com/soten/libs/base/abstrat/InternalModel");
	if (clazz == nullptr) {
		LOGE("Can't find InternalModel");
		return JNI_FALSE;
	}
	if (env->RegisterNatives(clazz, method_table,
			sizeof(method_table) / sizeof(method_table[0])) != JNI_OK) {
		LOGE("Can't find InternalModel RegisterNatives failed");
		return JNI_FALSE;
	}
	return JNI_TRUE;
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM * vm, void * reserved) {
	LOGD("Model JNI_OnLoad start");
	JNIEnv *env = nullptr;

	if (vm->GetEnv((void**) &env, JNI_VERSION_1_6) != JNI_OK) {
		LOGE("GetEnv failed");
		return JNI_ERR;
	}
	assert(env != nullptr);
	if (registerNative(env) != JNI_TRUE) {
		LOGE("Model registerNative failed");
		return JNI_ERR;
	}
	LOGD("Model JNI_OnLoad end JNI_VERSION_1_6");
	return JNI_VERSION_1_6;
}
JNIEXPORT void JNICALL JNI_OnUnload(JavaVM * vm, void * reserved) {
	LOGD("Model JNI_OnUnload");
}

