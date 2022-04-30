#include <jni.h>
#include <stdio.h>
#include <assert.h>
#include <sys/epoll.h>
#include <unistd.h>
#include <fcntl.h>
#include <errno.h>
#include <string.h>
#include <linux/inotify.h>
#include <dirent.h>
#include "android/log.h"
#define LOG_TAG "javen"
#define LOGD(fmt, args...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, fmt, ##args)
#define LOGE(fmt, args...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, fmt, ##args)
#define EPOLL_MAX_EVENTS 16
#define EPOLL_SIZE_HINT 8
enum {
	POLL_WAKE = -1,

	POLL_TIMEOUT = -2,

	POLL_ERROR = -3,

	POLL_CALLBACK = -4
};
enum {
	EVENT_INPUT = 1 << 0,

	EVENT_OUTPUT = 1 << 1,

	EVENT_ERROR = 1 << 2,

	EVENT_HANGUP = 1 << 3,

	EVENT_INVALID = 1 << 4,

	EVENT_ET = 1 << 5
};
static struct stage {
	jfieldID read_pipe_fd;
	jfieldID write_pipe_fd;
	jfieldID epoll_fd;
} stage_t;

void awoken(int);

jint native_pollInner(JNIEnv *env, jobject thiz, jint timeout) {

	int readPipeFd = env->GetIntField(thiz, stage_t.read_pipe_fd);
	int writePipeFd = env->GetIntField(thiz, stage_t.write_pipe_fd);
	int epollFd = env->GetIntField(thiz, stage_t.epoll_fd);

	LOGD("Stage pollInner ReadPipeFd=%d,WritePipeFd=%d,EpollFd=%d",
			readPipeFd, writePipeFd, epollFd);
	jint result = POLL_WAKE;
	struct epoll_event eventItem[EPOLL_MAX_EVENTS];
	int timeoutMillis = timeout;
	LOGD("pollInner timeoutMillis=%d", timeout);
	int count = epoll_wait(epollFd, eventItem, EPOLL_MAX_EVENTS,
			timeoutMillis);
	if (count < 0) {
		result = POLL_ERROR;
		if (errno != EINTR) {
			LOGE("Poll failed with an unexpected error, errno=%s",
					strerror(errno));
		}
		goto Done;
	}
	if (count == 0) {
		result = POLL_TIMEOUT;
		LOGE("pollInner timeout %d", timeoutMillis);
	}
	for (int i = 0; i < count; i++) {
		int fd = eventItem[i].data.fd;
		uint32_t epollEvents = eventItem[i].events;
		if (fd == readPipeFd) {
			if (epollEvents & EPOLLIN) {
				LOGD("fd == readPipeFd");
				awoken(readPipeFd);
				result = POLL_CALLBACK;
			} else {
				LOGE("Ignoring unexpected epoll events 0x%x on wake read pipe.",
						epollEvents);
			}

		} else {
			result = fd;
		}
	}

	Done: ;
	return result;
}
bool native_init(JNIEnv *env, jobject thiz) {
	return true;
}
bool native_open(JNIEnv *env, jobject thiz) {
	int wakeFds[2];
	if (pipe(wakeFds) != JNI_OK) {
		LOGE("Stage Could not create wake pipe.error=%s", strerror(errno));
		return false;
	}

	int readPipeFd = wakeFds[0];
	int writePipeFd = wakeFds[1];
	if (fcntl(readPipeFd, F_SETFL, O_NONBLOCK) != JNI_OK) {
		LOGE("Stage fcntl(mWakeReadPipeFd) failed error=%s", strerror(errno));
		return false;
	}
	if (fcntl(writePipeFd, F_SETFL, O_NONBLOCK) != JNI_OK) {
		LOGE("Stage fcntl(mWakeWritePipeFd) failed error=%s", strerror(errno));
		return false;
	}

	int epollFd = epoll_create(EPOLL_SIZE_HINT);
	if (epollFd < 0) {
		LOGE("Stage Could not create epoll instance.error=%s", strerror(errno));
		return false;
	}

	struct epoll_event eventItem;
	memset(&eventItem, 0, sizeof(eventItem));

	eventItem.events = EPOLLIN;
	eventItem.data.fd = readPipeFd;

	if (epoll_ctl(epollFd, EPOLL_CTL_ADD, readPipeFd, &eventItem) != 0) {
		LOGE("StageCould not add wake read pipe to epoll instance.error=%s",
				strerror(errno));
		return false;
	}

	env->SetIntField(thiz, stage_t.read_pipe_fd, readPipeFd);
	env->SetIntField(thiz, stage_t.write_pipe_fd, writePipeFd);
	env->SetIntField(thiz, stage_t.epoll_fd, epollFd);

	LOGD("Stage open start ReadPipeFd=%d,WritePipeFd=%d,EpollFd=%d",
			readPipeFd, writePipeFd, epollFd);
	return true;
}
void native_close(JNIEnv *env, jobject thiz) {
	int readPipeFd = env->GetIntField(thiz, stage_t.read_pipe_fd);
	int writePipeFd = env->GetIntField(thiz, stage_t.write_pipe_fd);
	int epollFd = env->GetIntField(thiz, stage_t.epoll_fd);

	close(readPipeFd);
	close(writePipeFd);
	close(epollFd);

	env->SetIntField(thiz, stage_t.read_pipe_fd, -1);
	env->SetIntField(thiz, stage_t.write_pipe_fd, -1);
	env->SetIntField(thiz, stage_t.epoll_fd, -1);

	LOGD("Stage close ReadPipeFd=%d,WritePipeFd=%d,EpollFd=%d", readPipeFd,
			writePipeFd, epollFd);
}
bool native_addFd(JNIEnv *env, jobject thiz, jint fd, jint events) {
	int epollFd = env->GetIntField(thiz, stage_t.epoll_fd);
	int epollEvents = 0;
	if (events & EVENT_INPUT)
		epollEvents |= EPOLLIN;
	if (events & EVENT_OUTPUT)
		epollEvents |= EPOLLOUT;
	if (events & EVENT_ET)
		epollEvents |= EPOLLET;

	if (epollEvents == 0) {
		LOGE("native_addFd events not exist. fd=%d,events=%d", fd, events);
		return false;
	}
	LOGD("Stage native_addFd fd =%d,events=%d", fd, events);

	struct epoll_event eventItem;
	memset(&eventItem, 0, sizeof(eventItem));

	eventItem.events = epollEvents;
	eventItem.data.fd = fd;

	if (epoll_ctl(epollFd, EPOLL_CTL_ADD, fd, &eventItem) != JNI_OK) {
		LOGE("Error adding epoll events for fd %d, errno=%s", fd,
				strerror(errno));
		return false;
	}
	return true;
}
bool native_removeFd(JNIEnv *env, jobject thiz, jint fd) {
	LOGD("Stage removeFd fd=%d", fd);
	int epollFd = env->GetIntField(thiz, stage_t.epoll_fd);

	if (epoll_ctl(epollFd, EPOLL_CTL_DEL, fd, NULL) != JNI_OK) {
		LOGE("Error removing epoll events for fd %d, errno=%s", fd,
				strerror(errno));
		return false;
	}
	return true;
}
void native_wake(JNIEnv *env, jobject thiz) {
	LOGD("Stage wake");
	int writePipeFd = env->GetIntField(thiz, stage_t.write_pipe_fd);
	ssize_t nWrite;
	do {
		nWrite = write(writePipeFd, "W", 1);
	} while (nWrite != -1 && errno == EINTR);
	if (nWrite != 1) {
		if (errno != EAGAIN) {
			LOGE("Could not write wake signal, errno=%s", strerror(errno));
		}
	}
}
void awoken(int readPipeFd) {
	char buffer[16];
	ssize_t nRead;
	do {
		nRead = read(readPipeFd, buffer, sizeof(buffer));
	} while ((nRead == -1 && errno == EINTR) || nRead == sizeof(buffer));
}
static JNINativeMethod method_table[] = {
		{ "pollInner", "(I)I",(void *) native_pollInner },
		{ "open", "()Z", (void *) native_open },
		{ "addFd", "(II)Z", (void *) native_addFd },
		{ "removeFd", "(I)Z",(void *) native_removeFd },
		{ "close", "()V", (void *) native_close },
		{ "wake", "()V", (void *) native_wake } };

static bool registerNative(JNIEnv *env) {
	jclass clazz = env->FindClass("com/soten/libs/base/abstrat/Stage");
	if (clazz == NULL) {
		LOGE("Stage Can't find Stage");
		return JNI_FALSE;
	}

	stage_t.write_pipe_fd = env->GetFieldID(clazz, "mWritePipeFd", "I");
	stage_t.read_pipe_fd = env->GetFieldID(clazz, "mReadPipeFd", "I");
	stage_t.epoll_fd = env->GetFieldID(clazz, "mEpollFd", "I");

	if (env->RegisterNatives(clazz, method_table,
			sizeof(method_table) / sizeof(method_table[0])) != JNI_OK) {
		LOGE("Can't find Stage RegisterNatives failed");
		return JNI_FALSE;
	}
	return JNI_TRUE;
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM * vm, void * reserved) {
	LOGD("Stage JNI_OnLoad start");
	JNIEnv *env = NULL;
	if (vm->GetEnv((void**) &env, JNI_VERSION_1_6) != JNI_OK) {
		LOGE("Stage GetEnv failed");
		return JNI_ERR;
	}
	assert(env != NULL);
	if (registerNative(env) != JNI_TRUE) {
		LOGE("Stage registerNative failed");
		return JNI_ERR;
	}
	LOGD("Stage JNI_OnLoad end JNI_VERSION_1_6");
	return JNI_VERSION_1_6;
}
JNIEXPORT void JNICALL JNI_OnUnload(JavaVM * vm, void * reserved) {
	LOGD("Stage JNI_OnUnload ");
}
