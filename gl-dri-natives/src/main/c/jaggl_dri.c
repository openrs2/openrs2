#include "jaggl_X11_dri.h"

#if defined(__unix__)
#include <dlfcn.h>
#include <stddef.h>

static void *jaggl_dri_handle;
#endif

JNIEXPORT jboolean JNICALL Java_jaggl_X11_dri_open(JNIEnv *env, jclass cls, jstring path) {
#if defined(__unix__)
	if (jaggl_dri_handle) {
		dlclose(jaggl_dri_handle);
		jaggl_dri_handle = NULL;
	}

	const char *path_str = (*env)->GetStringUTFChars(env, path, NULL);
	jaggl_dri_handle = dlopen(path_str, RTLD_LAZY | RTLD_GLOBAL);
	(*env)->ReleaseStringUTFChars(env, path, path_str);

	return jaggl_dri_handle != NULL;
#else
	return JNI_FALSE;
#endif
}

JNIEXPORT jboolean JNICALL Java_jaggl_X11_dri_close(JNIEnv *env, jclass cls) {
#if defined(__unix__)
	if (jaggl_dri_handle) {
		dlclose(jaggl_dri_handle);
		jaggl_dri_handle = NULL;
		return JNI_TRUE;
	}
#endif

	return JNI_FALSE;
}
