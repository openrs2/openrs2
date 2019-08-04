#include "jaggl_context.h"
#include "jaggl_opengl.h"

#include <GL/glx.h>
#include <jawt.h>
#include <jawt_md.h>
#include <stdbool.h>
#include <stddef.h>

#define JAGGL_LOCK(env) \
	JAWT awt = { .version = JAWT_VERSION_1_4 }; \
	bool awt_valid = JAWT_GetAWT(env, &awt); \
	if (awt_valid) { \
		awt.Lock(env); \
	}

#define JAGGL_UNLOCK(env) \
	if (awt_valid) { \
		awt.Unlock(env); \
	}

static Display *jaggl_display;
static XVisualInfo *jaggl_visual_info;
static VisualID jaggl_visual_id;
static GLXContext jaggl_context;
static GLXDrawable jaggl_drawable;
static int jaggl_alpha_bits;
static bool jaggl_double_buffered;

static PFNGLXSWAPINTERVALSGIPROC jaggl_glXSwapIntervalSGI;

static void jaggl_init_proc_table(void) {
	jaggl_glXSwapIntervalSGI = (PFNGLXSWAPINTERVALSGIPROC) glXGetProcAddressARB((const GLubyte *) "glXSwapIntervalSGI");
}

JNIEXPORT jboolean JNICALL Java_jaggl_context_createContext(JNIEnv *env, jclass cls) {
	JAGGL_LOCK(env);

	GLXContext current = glXGetCurrentContext();
	if (current) {
		glXMakeCurrent(jaggl_display, None, NULL);
	}

	if (jaggl_context) {
		glXDestroyContext(jaggl_display, jaggl_context);
		jaggl_context = NULL;
	}

	jaggl_context = glXCreateContext(jaggl_display, jaggl_visual_info, NULL, True);

	JAGGL_UNLOCK(env);
	return jaggl_context != NULL;
}

JNIEXPORT jboolean JNICALL Java_jaggl_context_releaseContext(JNIEnv *env, jclass cls) {
	JAGGL_LOCK(env);

	jboolean result = JNI_TRUE;

	GLXContext current = glXGetCurrentContext();
	if (current) {
		result = (jboolean) glXMakeCurrent(jaggl_display, None, NULL);
	}

	JAGGL_UNLOCK(env);
	return result;
}

JNIEXPORT jboolean JNICALL Java_jaggl_context_destroy(JNIEnv *env, jclass cls) {
	JAGGL_LOCK(env);

	GLXContext current = glXGetCurrentContext();
	if (current) {
		glXMakeCurrent(jaggl_display, None, NULL);
	}

	if (jaggl_context) {
		glXDestroyContext(jaggl_display, jaggl_context);
		jaggl_context = NULL;
	}

	if (jaggl_visual_info) {
		XFree(jaggl_visual_info);
		jaggl_visual_info = NULL;
	}

	jaggl_display = None;

	JAGGL_UNLOCK(env);
	return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL Java_jaggl_context_swapBuffers(JNIEnv *env, jclass cls) {
	JAGGL_LOCK(env);

	if (jaggl_double_buffered) {
		glXSwapBuffers(jaggl_display, jaggl_drawable);
	} else {
		glFlush();
	}

	JAGGL_UNLOCK(env);
	return JNI_TRUE;
}

JNIEXPORT jint JNICALL Java_jaggl_context_getLastError(JNIEnv *env, jclass cls) {
	return 0;
}

JNIEXPORT void JNICALL Java_jaggl_context_setSwapInterval(JNIEnv *env, jclass cls, jint interval) {
	JAGGL_LOCK(env);

	if (jaggl_glXSwapIntervalSGI) {
		jaggl_glXSwapIntervalSGI((int) interval);
	}

	JAGGL_UNLOCK(env);
}

JNIEXPORT jstring JNICALL Java_jaggl_context_getExtensionsString(JNIEnv *env, jclass cls) {
	JAGGL_LOCK(env);

	const char *extensions_str = glXQueryExtensionsString(jaggl_display, jaggl_visual_info->screen);
	jstring extensions = (*env)->NewStringUTF(env, extensions_str);

	JAGGL_UNLOCK(env);
	return extensions;
}

JNIEXPORT jint JNICALL Java_jaggl_context_getAlphaBits(JNIEnv *env, jclass cls) {
	return jaggl_alpha_bits;
}

JNIEXPORT jboolean JNICALL Java_jaggl_context_choosePixelFormat1(JNIEnv *env, jclass cls, jobject component, jint num_samples, jint alpha_bits) {
	JAGGL_LOCK(env);

	jboolean result = JNI_FALSE;

	if (!awt_valid) {
		goto awt_unlock;
	}

	JAWT_DrawingSurface *ds = awt.GetDrawingSurface(env, component);
	if (!ds) {
		goto awt_unlock;
	}

	jint lock_result = ds->Lock(ds);
	if ((lock_result & JAWT_LOCK_ERROR) != 0) {
		goto ds_free;
	}

	JAWT_DrawingSurfaceInfo *dsi = ds->GetDrawingSurfaceInfo(ds);
	if (!dsi) {
		goto ds_unlock;
	}

	JAWT_X11DrawingSurfaceInfo *platformInfo = (JAWT_X11DrawingSurfaceInfo *) dsi->platformInfo;
	if (!platformInfo) {
		goto dsi_free;
	}

	jaggl_display = platformInfo->display;
	jaggl_drawable = platformInfo->drawable;
	jaggl_visual_id = platformInfo->visualID;

	if (!glXQueryExtension(jaggl_display, NULL, NULL)) {
		goto dsi_free;
	}

	XWindowAttributes window_attribs;
	if (XGetWindowAttributes(jaggl_display, jaggl_drawable, &window_attribs)) {
		XVisualInfo visual_info_template = { .visualid = window_attribs.visual->visualid };
		int matches;
		jaggl_visual_info = XGetVisualInfo(jaggl_display, VisualIDMask, &visual_info_template, &matches);
		if (jaggl_visual_info) {
			int value;
			glXGetConfig(jaggl_display, jaggl_visual_info, GLX_DOUBLEBUFFER, &value);
			jaggl_double_buffered = value;

			glXGetConfig(jaggl_display, jaggl_visual_info, GLX_ALPHA_SIZE, &value);
			jaggl_alpha_bits = value;

			result = JNI_TRUE;
			goto dsi_free;
		}
	}

	for (int i = 0; i < 2; i++) {
		bool double_buffered = i == 0;
		int attribs[] = {
			GLX_RGBA,
			GLX_RED_SIZE,
			8,
			GLX_GREEN_SIZE,
			8,
			GLX_BLUE_SIZE,
			8,
			GLX_ALPHA_SIZE,
			alpha_bits,
			GLX_DEPTH_SIZE,
			24,
			GLX_SAMPLE_BUFFERS,
			num_samples ? True : False,
			GLX_SAMPLES,
			num_samples,
			double_buffered ? GLX_DOUBLEBUFFER : None,
			None
		};
		jaggl_visual_info = glXChooseVisual(jaggl_display, DefaultScreen(jaggl_display), attribs);
		if (jaggl_visual_info) {
			jaggl_double_buffered = double_buffered;
			jaggl_alpha_bits = alpha_bits;

			result = JNI_TRUE;
			goto dsi_free;
		}
	}

dsi_free:
	ds->FreeDrawingSurfaceInfo(dsi);
ds_unlock:
	ds->Unlock(ds);
ds_free:
	awt.FreeDrawingSurface(ds);
awt_unlock:
	JAGGL_UNLOCK(env);
	return result;
}

JNIEXPORT jboolean JNICALL Java_jaggl_context_makeCurrent1(JNIEnv *env, jclass cls) {
	JAGGL_LOCK(env);

	jboolean result = JNI_FALSE;

	if (!jaggl_context) {
		goto done;
	}

	GLXContext current = glXGetCurrentContext();
	if (jaggl_context == current) {
		result = JNI_TRUE;
		goto done;
	}

	glXMakeCurrent(jaggl_display, None, NULL);

	if (!glXMakeCurrent(jaggl_display, jaggl_drawable, jaggl_context)) {
		goto done;
	}

	jaggl_init_proc_table();
	result = JNI_TRUE;

done:
	JAGGL_UNLOCK(env);
	return result;
}

JNIEXPORT void JNICALL Java_jaggl_opengl_glActiveTexture(JNIEnv *env, jobject obj, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glAlphaFunc(JNIEnv *env, jobject obj, jint, jfloat);
JNIEXPORT void JNICALL Java_jaggl_opengl_glAttachObjectARB(JNIEnv *env, jobject obj, jint, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glBegin(JNIEnv *env, jobject obj, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glBindBufferARB(JNIEnv *env, jobject obj, jint, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glBindFramebufferEXT(JNIEnv *env, jobject obj, jint, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glBindProgramARB(JNIEnv *env, jobject obj, jint, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glBindRenderbufferEXT(JNIEnv *env, jobject obj, jint, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glBindTexture(JNIEnv *env, jobject obj, jint, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glBlendFunc(JNIEnv *env, jobject obj, jint, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glBufferDataARB0(JNIEnv *env, jobject obj, jint, jint, jobject, jint, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glBufferDataARB1(JNIEnv *env, jobject obj, jint, jint, jobject, jint, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glBufferSubDataARB0(JNIEnv *env, jobject obj, jint, jint, jint, jobject, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glBufferSubDataARB1(JNIEnv *env, jobject obj, jint, jint, jint, jobject, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glCallList(JNIEnv *env, jobject obj, jint);
JNIEXPORT jint JNICALL Java_jaggl_opengl_glCheckFramebufferStatusEXT(JNIEnv *env, jobject obj, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glClear(JNIEnv *env, jobject obj, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glClearColor(JNIEnv *env, jobject obj, jfloat, jfloat, jfloat, jfloat);
JNIEXPORT void JNICALL Java_jaggl_opengl_glClearDepth(JNIEnv *env, jobject obj, jfloat);
JNIEXPORT void JNICALL Java_jaggl_opengl_glClientActiveTexture(JNIEnv *env, jobject obj, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glColor3ub(JNIEnv *env, jobject obj, jbyte, jbyte, jbyte);
JNIEXPORT void JNICALL Java_jaggl_opengl_glColor4f(JNIEnv *env, jobject obj, jfloat, jfloat, jfloat, jfloat);
JNIEXPORT void JNICALL Java_jaggl_opengl_glColor4fv1(JNIEnv *env, jobject obj, jobject, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glColor4ub(JNIEnv *env, jobject obj, jbyte, jbyte, jbyte, jbyte);
JNIEXPORT void JNICALL Java_jaggl_opengl_glColorMask(JNIEnv *env, jobject obj, jboolean, jboolean, jboolean, jboolean);
JNIEXPORT void JNICALL Java_jaggl_opengl_glColorMaterial(JNIEnv *env, jobject obj, jint, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glColorPointer(JNIEnv *env, jobject obj, jint, jint, jint, jlong);
JNIEXPORT void JNICALL Java_jaggl_opengl_glColorPointer0(JNIEnv *env, jobject obj, jint, jint, jint, jobject, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glColorPointer1(JNIEnv *env, jobject obj, jint, jint, jint, jobject, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glCompileShaderARB(JNIEnv *env, jobject obj, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glCopyPixels(JNIEnv *env, jobject obj, jint, jint, jint, jint, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glCopyTexImage2D(JNIEnv *env, jobject obj, jint, jint, jint, jint, jint, jint, jint, jint);
JNIEXPORT jint JNICALL Java_jaggl_opengl_glCreateProgramObjectARB(JNIEnv *env, jobject obj);
JNIEXPORT jint JNICALL Java_jaggl_opengl_glCreateShaderObjectARB(JNIEnv *env, jobject obj, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glCullFace(JNIEnv *env, jobject obj, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glDeleteBuffersARB1(JNIEnv *env, jobject obj, jint, jobject, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glDeleteFramebuffersEXT1(JNIEnv *env, jobject obj, jint, jobject, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glDeleteLists(JNIEnv *env, jobject obj, jint, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glDeleteObjectARB(JNIEnv *env, jobject obj, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glDeleteRenderbuffersEXT1(JNIEnv *env, jobject obj, jint, jobject, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glDeleteTextures1(JNIEnv *env, jobject obj, jint, jobject, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glDepthFunc(JNIEnv *env, jobject obj, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glDepthMask(JNIEnv *env, jobject obj, jboolean);
JNIEXPORT void JNICALL Java_jaggl_opengl_glDetachObjectARB(JNIEnv *env, jobject obj, jint, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glDisable(JNIEnv *env, jobject obj, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glDisableClientState(JNIEnv *env, jobject obj, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glDrawArrays(JNIEnv *env, jobject obj, jint, jint, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glDrawBuffer(JNIEnv *env, jobject obj, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glDrawElements(JNIEnv *env, jobject obj, jint, jint, jint, jlong);
JNIEXPORT void JNICALL Java_jaggl_opengl_glDrawElements0(JNIEnv *env, jobject obj, jint, jint, jint, jobject, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glDrawElements1(JNIEnv *env, jobject obj, jint, jint, jint, jobject, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glDrawPixels0(JNIEnv *env, jobject obj, jint, jint, jint, jint, jobject, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glDrawPixels1(JNIEnv *env, jobject obj, jint, jint, jint, jint, jobject, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glEnable(JNIEnv *env, jobject obj, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glEnableClientState(JNIEnv *env, jobject obj, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glEnd(JNIEnv *env, jobject obj);
JNIEXPORT void JNICALL Java_jaggl_opengl_glEndList(JNIEnv *env, jobject obj);
JNIEXPORT void JNICALL Java_jaggl_opengl_glFogf(JNIEnv *env, jobject obj, jint, jfloat);
JNIEXPORT void JNICALL Java_jaggl_opengl_glFogfv1(JNIEnv *env, jobject obj, jint, jobject, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glFogi(JNIEnv *env, jobject obj, jint, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glFramebufferRenderbufferEXT(JNIEnv *env, jobject obj, jint, jint, jint, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glFramebufferTexture2DEXT(JNIEnv *env, jobject obj, jint, jint, jint, jint, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glGenBuffersARB1(JNIEnv *env, jobject obj, jint, jobject, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glGenFramebuffersEXT1(JNIEnv *env, jobject obj, jint, jobject, jint);
JNIEXPORT jint JNICALL Java_jaggl_opengl_glGenLists(JNIEnv *env, jobject obj, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glGenProgramsARB1(JNIEnv *env, jobject obj, jint, jobject, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glGenRenderbuffersEXT1(JNIEnv *env, jobject obj, jint, jobject, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glGenTextures1(JNIEnv *env, jobject obj, jint, jobject, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glGetFloatv0(JNIEnv *env, jobject obj, jint, jobject, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glGetFloatv1(JNIEnv *env, jobject obj, jint, jobject, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glGetInfoLogARB1(JNIEnv *env, jobject obj, jint, jint, jobject, jint, jbyteArray, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glGetIntegerv1(JNIEnv *env, jobject obj, jint, jobject, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glGetObjectParameterivARB1(JNIEnv *env, jobject obj, jint, jint, jobject, jint);
JNIEXPORT jstring JNICALL Java_jaggl_opengl_glGetString(JNIEnv *env, jobject obj, jint);
JNIEXPORT jint JNICALL Java_jaggl_opengl_glGetUniformLocation(JNIEnv *env, jobject obj, jint, jstring);
JNIEXPORT void JNICALL Java_jaggl_opengl_glHint(JNIEnv *env, jobject obj, jint, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glInterleavedArrays(JNIEnv *env, jobject obj, jint, jint, jlong);
JNIEXPORT void JNICALL Java_jaggl_opengl_glInterleavedArrays0(JNIEnv *env, jobject obj, jint, jint, jobject, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glInterleavedArrays1(JNIEnv *env, jobject obj, jint, jint, jobject, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glLightModelfv1(JNIEnv *env, jobject obj, jint, jobject, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glLightf(JNIEnv *env, jobject obj, jint, jint, jfloat);
JNIEXPORT void JNICALL Java_jaggl_opengl_glLightfv1(JNIEnv *env, jobject obj, jint, jint, jobject, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glLineWidth(JNIEnv *env, jobject obj, jfloat);
JNIEXPORT void JNICALL Java_jaggl_opengl_glLinkProgramARB(JNIEnv *env, jobject obj, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glLoadIdentity(JNIEnv *env, jobject obj);
JNIEXPORT void JNICALL Java_jaggl_opengl_glLoadMatrixf1(JNIEnv *env, jobject obj, jobject, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glMaterialfv1(JNIEnv *env, jobject obj, jint, jint, jobject, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glMatrixMode(JNIEnv *env, jobject obj, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glMultiTexCoord2f(JNIEnv *env, jobject obj, jint, jfloat, jfloat);
JNIEXPORT void JNICALL Java_jaggl_opengl_glMultiTexCoord2i(JNIEnv *env, jobject obj, jint, jint, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glNewList(JNIEnv *env, jobject obj, jint, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glNormal3f(JNIEnv *env, jobject obj, jfloat, jfloat, jfloat);
JNIEXPORT void JNICALL Java_jaggl_opengl_glNormalPointer(JNIEnv *env, jobject obj, jint, jint, jlong);
JNIEXPORT void JNICALL Java_jaggl_opengl_glNormalPointer0(JNIEnv *env, jobject obj, jint, jint, jobject, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glNormalPointer1(JNIEnv *env, jobject obj, jint, jint, jobject, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glOrtho(JNIEnv *env, jobject obj, jdouble, jdouble, jdouble, jdouble, jdouble, jdouble);
JNIEXPORT void JNICALL Java_jaggl_opengl_glPointParameterfARB(JNIEnv *env, jobject obj, jint, jfloat);
JNIEXPORT void JNICALL Java_jaggl_opengl_glPointParameterfvARB1(JNIEnv *env, jobject obj, jint, jobject, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glPointSize(JNIEnv *env, jobject obj, jfloat);
JNIEXPORT void JNICALL Java_jaggl_opengl_glPolygonMode(JNIEnv *env, jobject obj, jint, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glPopAttrib(JNIEnv *env, jobject obj);
JNIEXPORT void JNICALL Java_jaggl_opengl_glPopMatrix(JNIEnv *env, jobject obj);
JNIEXPORT void JNICALL Java_jaggl_opengl_glProgramLocalParameter4fARB(JNIEnv *env, jobject obj, jint, jint, jfloat, jfloat, jfloat, jfloat);
JNIEXPORT void JNICALL Java_jaggl_opengl_glProgramLocalParameter4fvARB0(JNIEnv *env, jobject obj, jint, jint, jobject, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glProgramLocalParameter4fvARB1(JNIEnv *env, jobject obj, jint, jint, jobject, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glProgramStringARB(JNIEnv *env, jobject obj, jint, jint, jint, jstring);
JNIEXPORT void JNICALL Java_jaggl_opengl_glPushAttrib(JNIEnv *env, jobject obj, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glPushMatrix(JNIEnv *env, jobject obj);
JNIEXPORT void JNICALL Java_jaggl_opengl_glRasterPos2i(JNIEnv *env, jobject obj, jint, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glReadBuffer(JNIEnv *env, jobject obj, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glRenderbufferStorageEXT(JNIEnv *env, jobject obj, jint, jint, jint, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glRotatef(JNIEnv *env, jobject obj, jfloat, jfloat, jfloat, jfloat);
JNIEXPORT void JNICALL Java_jaggl_opengl_glScalef(JNIEnv *env, jobject obj, jfloat, jfloat, jfloat);
JNIEXPORT void JNICALL Java_jaggl_opengl_glScissor(JNIEnv *env, jobject obj, jint, jint, jint, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glShadeModel(JNIEnv *env, jobject obj, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glShaderSourceARB0(JNIEnv *env, jobject obj, jint, jint, jobject, jintArray, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glTexCoord2f(JNIEnv *env, jobject obj, jfloat, jfloat);
JNIEXPORT void JNICALL Java_jaggl_opengl_glTexCoord2i(JNIEnv *env, jobject obj, jint, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glTexCoordPointer(JNIEnv *env, jobject obj, jint, jint, jint, jlong);
JNIEXPORT void JNICALL Java_jaggl_opengl_glTexCoordPointer0(JNIEnv *env, jobject obj, jint, jint, jint, jobject, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glTexCoordPointer1(JNIEnv *env, jobject obj, jint, jint, jint, jobject, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glTexEnvf(JNIEnv *env, jobject obj, jint, jint, jfloat);
JNIEXPORT void JNICALL Java_jaggl_opengl_glTexEnvfv1(JNIEnv *env, jobject obj, jint, jint, jobject, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glTexEnvi(JNIEnv *env, jobject obj, jint, jint, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glTexGenfv1(JNIEnv *env, jobject obj, jint, jint, jobject, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glTexGeni(JNIEnv *env, jobject obj, jint, jint, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glTexImage1D0(JNIEnv *env, jobject obj, jint, jint, jint, jint, jint, jint, jint, jobject, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glTexImage1D1(JNIEnv *env, jobject obj, jint, jint, jint, jint, jint, jint, jint, jobject, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glTexImage2D0(JNIEnv *env, jobject obj, jint, jint, jint, jint, jint, jint, jint, jint, jobject, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glTexImage2D1(JNIEnv *env, jobject obj, jint, jint, jint, jint, jint, jint, jint, jint, jobject, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glTexImage3D0(JNIEnv *env, jobject obj, jint, jint, jint, jint, jint, jint, jint, jint, jint, jobject, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glTexImage3D1(JNIEnv *env, jobject obj, jint, jint, jint, jint, jint, jint, jint, jint, jint, jobject, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glTexParameteri(JNIEnv *env, jobject obj, jint, jint, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glTranslatef(JNIEnv *env, jobject obj, jfloat, jfloat, jfloat);
JNIEXPORT void JNICALL Java_jaggl_opengl_glUniform1iARB(JNIEnv *env, jobject obj, jint, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glUniform3fARB(JNIEnv *env, jobject obj, jint, jfloat, jfloat, jfloat);
JNIEXPORT void JNICALL Java_jaggl_opengl_glUseProgramObjectARB(JNIEnv *env, jobject obj, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glVertex2f(JNIEnv *env, jobject obj, jfloat, jfloat);
JNIEXPORT void JNICALL Java_jaggl_opengl_glVertex2i(JNIEnv *env, jobject obj, jint, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glVertexPointer(JNIEnv *env, jobject obj, jint, jint, jint, jlong);
JNIEXPORT void JNICALL Java_jaggl_opengl_glVertexPointer0(JNIEnv *env, jobject obj, jint, jint, jint, jobject, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glVertexPointer1(JNIEnv *env, jobject obj, jint, jint, jint, jobject, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glViewport(JNIEnv *env, jobject obj, jint, jint, jint, jint);
