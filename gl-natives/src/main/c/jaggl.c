#include "jaggl_context.h"
#include "jaggl_opengl.h"

#include <GL/glx.h>
#include <jawt.h>
#include <jawt_md.h>
#include <stdbool.h>
#include <stddef.h>

#define PFNGLCLIENTACTIVETEXTUREPROC PFNGLCLIENTACTIVETEXTUREARBPROC
#define PFNGLMULTITEXCOORD2FPROC PFNGLMULTITEXCOORD2FARBPROC
#define PFNGLMULTITEXCOORD2IPROC PFNGLMULTITEXCOORD2IARBPROC

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

static PFNGLACTIVETEXTUREPROC jaggl_glActiveTexture;
static PFNGLACTIVETEXTUREARBPROC jaggl_glActiveTextureARB;
static PFNGLATTACHOBJECTARBPROC jaggl_glAttachObjectARB;
static PFNGLBINDBUFFERARBPROC jaggl_glBindBufferARB;
static PFNGLBINDFRAMEBUFFEREXTPROC jaggl_glBindFramebufferEXT;
static PFNGLBINDPROGRAMARBPROC jaggl_glBindProgramARB;
static PFNGLBINDRENDERBUFFEREXTPROC jaggl_glBindRenderbufferEXT;
static PFNGLCHECKFRAMEBUFFERSTATUSEXTPROC jaggl_glCheckFramebufferStatusEXT;
static PFNGLCLIENTACTIVETEXTUREPROC jaggl_glClientActiveTexture;
static PFNGLCLIENTACTIVETEXTUREARBPROC jaggl_glClientActiveTextureARB;
static PFNGLCOMPILESHADERARBPROC jaggl_glCompileShaderARB;
static PFNGLCREATEPROGRAMOBJECTARBPROC jaggl_glCreateProgramObjectARB;
static PFNGLCREATESHADEROBJECTARBPROC jaggl_glCreateShaderObjectARB;
static PFNGLDELETEOBJECTARBPROC jaggl_glDeleteObjectARB;
static PFNGLDETACHOBJECTARBPROC jaggl_glDetachObjectARB;
static PFNGLFRAMEBUFFERRENDERBUFFEREXTPROC jaggl_glFramebufferRenderbufferEXT;
static PFNGLFRAMEBUFFERTEXTURE2DEXTPROC jaggl_glFramebufferTexture2DEXT;
static PFNGLLINKPROGRAMARBPROC jaggl_glLinkProgramARB;
static PFNGLMULTITEXCOORD2FPROC jaggl_glMultiTexCoord2f;
static PFNGLMULTITEXCOORD2FARBPROC jaggl_glMultiTexCoord2fARB;
static PFNGLMULTITEXCOORD2IPROC jaggl_glMultiTexCoord2i;
static PFNGLMULTITEXCOORD2IARBPROC jaggl_glMultiTexCoord2iARB;
static PFNGLPOINTPARAMETERFARBPROC jaggl_glPointParameterfARB;
static PFNGLPROGRAMLOCALPARAMETER4FARBPROC jaggl_glProgramLocalParameter4fARB;
static PFNGLRENDERBUFFERSTORAGEEXTPROC jaggl_glRenderbufferStorageEXT;
static PFNGLUNIFORM1IARBPROC jaggl_glUniform1iARB;
static PFNGLUNIFORM3FARBPROC jaggl_glUniform3fARB;
static PFNGLUSEPROGRAMOBJECTARBPROC jaggl_glUseProgramObjectARB;
static PFNGLXSWAPINTERVALSGIPROC jaggl_glXSwapIntervalSGI;

static void jaggl_init_proc_table(void) {
	jaggl_glActiveTexture = (PFNGLACTIVETEXTUREPROC) glXGetProcAddressARB((const GLubyte *) "glActiveTexture");
	jaggl_glActiveTextureARB = (PFNGLACTIVETEXTUREARBPROC) glXGetProcAddressARB((const GLubyte *) "glActiveTextureARB");
	jaggl_glAttachObjectARB = (PFNGLATTACHOBJECTARBPROC) glXGetProcAddressARB((const GLubyte *) "glAttachObjectARB");
	jaggl_glBindBufferARB = (PFNGLBINDBUFFERARBPROC) glXGetProcAddressARB((const GLubyte *) "glBindBufferARB");
	jaggl_glBindFramebufferEXT = (PFNGLBINDFRAMEBUFFEREXTPROC) glXGetProcAddressARB((const GLubyte *) "glBindFramebufferEXT");
	jaggl_glBindProgramARB = (PFNGLBINDPROGRAMARBPROC) glXGetProcAddressARB((const GLubyte *) "glBindProgramARB");
	jaggl_glBindRenderbufferEXT = (PFNGLBINDRENDERBUFFEREXTPROC) glXGetProcAddressARB((const GLubyte *) "glBindRenderbufferEXT");
	jaggl_glCheckFramebufferStatusEXT = (PFNGLCHECKFRAMEBUFFERSTATUSEXTPROC) glXGetProcAddressARB((const GLubyte *) "glCheckFramebufferStatusEXT");
	jaggl_glClientActiveTexture = (PFNGLCLIENTACTIVETEXTUREPROC) glXGetProcAddressARB((const GLubyte *) "glClientActiveTexture");
	jaggl_glClientActiveTextureARB = (PFNGLCLIENTACTIVETEXTUREARBPROC) glXGetProcAddressARB((const GLubyte *) "glClientActiveTextureARB");
	jaggl_glCompileShaderARB = (PFNGLCOMPILESHADERARBPROC) glXGetProcAddressARB((const GLubyte *) "glCompileShaderARB");
	jaggl_glCreateProgramObjectARB = (PFNGLCREATEPROGRAMOBJECTARBPROC) glXGetProcAddressARB((const GLubyte *) "glCreateProgramObjectARB");
	jaggl_glCreateShaderObjectARB = (PFNGLCREATESHADEROBJECTARBPROC) glXGetProcAddressARB((const GLubyte *) "glCreateShaderObjectARB");
	jaggl_glDeleteObjectARB = (PFNGLDELETEOBJECTARBPROC) glXGetProcAddressARB((const GLubyte *) "glDeleteObjectARB");
	jaggl_glDetachObjectARB = (PFNGLDETACHOBJECTARBPROC) glXGetProcAddressARB((const GLubyte *) "glDetachObjectARB");
	jaggl_glFramebufferRenderbufferEXT = (PFNGLFRAMEBUFFERRENDERBUFFEREXTPROC) glXGetProcAddressARB((const GLubyte *) "glFramebufferRenderbufferEXT");
	jaggl_glFramebufferTexture2DEXT = (PFNGLFRAMEBUFFERTEXTURE2DEXTPROC) glXGetProcAddressARB((const GLubyte *) "glFramebufferTexture2DEXT");
	jaggl_glLinkProgramARB = (PFNGLLINKPROGRAMARBPROC) glXGetProcAddressARB((const GLubyte *) "glLinkProgramARB");
	jaggl_glMultiTexCoord2f = (PFNGLMULTITEXCOORD2FPROC) glXGetProcAddressARB((const GLubyte *) "glMultiTexCoord2f");
	jaggl_glMultiTexCoord2fARB = (PFNGLMULTITEXCOORD2FARBPROC) glXGetProcAddressARB((const GLubyte *) "glMultiTexCoord2fARB");
	jaggl_glMultiTexCoord2i = (PFNGLMULTITEXCOORD2IPROC) glXGetProcAddressARB((const GLubyte *) "glMultiTexCoord2i");
	jaggl_glMultiTexCoord2iARB = (PFNGLMULTITEXCOORD2IARBPROC) glXGetProcAddressARB((const GLubyte *) "glMultiTexCoord2iARB");
	jaggl_glPointParameterfARB = (PFNGLPOINTPARAMETERFARBPROC) glXGetProcAddressARB((const GLubyte *) "glPointParameterfARB");
	jaggl_glProgramLocalParameter4fARB = (PFNGLPROGRAMLOCALPARAMETER4FARBPROC) glXGetProcAddressARB((const GLubyte *) "glProgramLocalParameter4fARB");
	jaggl_glRenderbufferStorageEXT = (PFNGLRENDERBUFFERSTORAGEEXTPROC) glXGetProcAddressARB((const GLubyte *) "glRenderbufferStorageEXT");
	jaggl_glUniform1iARB = (PFNGLUNIFORM1IARBPROC) glXGetProcAddressARB((const GLubyte *) "glUniform1iARB");
	jaggl_glUniform3fARB = (PFNGLUNIFORM3FARBPROC) glXGetProcAddressARB((const GLubyte *) "glUniform3fARB");
	jaggl_glUseProgramObjectARB = (PFNGLUSEPROGRAMOBJECTARBPROC) glXGetProcAddressARB((const GLubyte *) "glUseProgramObjectARB");
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

JNIEXPORT void JNICALL Java_jaggl_opengl_glActiveTexture(JNIEnv *env, jobject obj, jint texture) {
	JAGGL_LOCK(env);

	if (jaggl_glActiveTexture) {
		jaggl_glActiveTexture((GLenum) texture);
	} else if (jaggl_glActiveTextureARB) {
		jaggl_glActiveTextureARB((GLenum) texture);
	}

	JAGGL_UNLOCK(env);
}

JNIEXPORT void JNICALL Java_jaggl_opengl_glAlphaFunc(JNIEnv *env, jobject obj, jint func, jfloat ref) {
	JAGGL_LOCK(env);

	glAlphaFunc((GLenum) func, (GLclampf) ref);

	JAGGL_UNLOCK(env);
}

JNIEXPORT void JNICALL Java_jaggl_opengl_glAttachObjectARB(JNIEnv *env, jobject obj, jint program, jint shader) {
	JAGGL_LOCK(env);

	if (jaggl_glAttachObjectARB) {
		jaggl_glAttachObjectARB((GLhandleARB) program, (GLhandleARB) shader);
	}

	JAGGL_UNLOCK(env);
}

JNIEXPORT void JNICALL Java_jaggl_opengl_glBegin(JNIEnv *env, jobject obj, jint mode) {
	JAGGL_LOCK(env);

	glBegin((GLenum) mode);

	JAGGL_UNLOCK(env);
}

JNIEXPORT void JNICALL Java_jaggl_opengl_glBindBufferARB(JNIEnv *env, jobject obj, jint target, jint buffer) {
	JAGGL_LOCK(env);

	if (jaggl_glBindBufferARB) {
		jaggl_glBindBufferARB((GLenum) target, (GLuint) buffer);
	}

	JAGGL_UNLOCK(env);
}

JNIEXPORT void JNICALL Java_jaggl_opengl_glBindFramebufferEXT(JNIEnv *env, jobject obj, jint target, jint framebuffer) {
	JAGGL_LOCK(env);

	if (jaggl_glBindFramebufferEXT) {
		jaggl_glBindFramebufferEXT((GLenum) target, (GLuint) framebuffer);
	}

	JAGGL_UNLOCK(env);
}

JNIEXPORT void JNICALL Java_jaggl_opengl_glBindProgramARB(JNIEnv *env, jobject obj, jint target, jint program) {
	JAGGL_LOCK(env);

	if (jaggl_glBindProgramARB) {
		jaggl_glBindProgramARB((GLenum) target, (GLuint) program);
	}

	JAGGL_UNLOCK(env);
}

JNIEXPORT void JNICALL Java_jaggl_opengl_glBindRenderbufferEXT(JNIEnv *env, jobject obj, jint target, jint renderbuffer) {
	JAGGL_LOCK(env);

	if (jaggl_glBindRenderbufferEXT) {
		jaggl_glBindRenderbufferEXT((GLenum) target, (GLuint) renderbuffer);
	}

	JAGGL_UNLOCK(env);
}

JNIEXPORT void JNICALL Java_jaggl_opengl_glBindTexture(JNIEnv *env, jobject obj, jint target, jint texture) {
	JAGGL_LOCK(env);

	glBindTexture((GLenum) target, (GLuint) texture);

	JAGGL_UNLOCK(env);
}

JNIEXPORT void JNICALL Java_jaggl_opengl_glBlendFunc(JNIEnv *env, jobject obj, jint sfactor, jint dfactor) {
	JAGGL_LOCK(env);

	glBlendFunc((GLenum) sfactor, (GLenum) dfactor);

	JAGGL_UNLOCK(env);
}

JNIEXPORT void JNICALL Java_jaggl_opengl_glBufferDataARB0(JNIEnv *env, jobject obj, jint, jint, jobject, jint, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glBufferDataARB1(JNIEnv *env, jobject obj, jint, jint, jobject, jint, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glBufferSubDataARB0(JNIEnv *env, jobject obj, jint, jint, jint, jobject, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glBufferSubDataARB1(JNIEnv *env, jobject obj, jint, jint, jint, jobject, jint);

JNIEXPORT void JNICALL Java_jaggl_opengl_glCallList(JNIEnv *env, jobject obj, jint list) {
	JAGGL_LOCK(env);

	glCallList((GLuint) list);

	JAGGL_UNLOCK(env);
}

JNIEXPORT jint JNICALL Java_jaggl_opengl_glCheckFramebufferStatusEXT(JNIEnv *env, jobject obj, jint target) {
	JAGGL_LOCK(env);

	GLenum result;
	if (jaggl_glCheckFramebufferStatusEXT) {
		result = jaggl_glCheckFramebufferStatusEXT((GLuint) target);
	} else {
		result = 0;
	}

	JAGGL_UNLOCK(env);
	return (jint) result;
}

JNIEXPORT void JNICALL Java_jaggl_opengl_glClear(JNIEnv *env, jobject obj, jint mask) {
	JAGGL_LOCK(env);

	glClear((GLbitfield) mask);

	JAGGL_UNLOCK(env);
}

JNIEXPORT void JNICALL Java_jaggl_opengl_glClearColor(JNIEnv *env, jobject obj, jfloat red, jfloat green, jfloat blue, jfloat alpha) {
	JAGGL_LOCK(env);

	glClearColor((GLclampf) red, (GLclampf) green, (GLclampf) blue, (GLclampf) alpha);

	JAGGL_UNLOCK(env);
}

JNIEXPORT void JNICALL Java_jaggl_opengl_glClearDepth(JNIEnv *env, jobject obj, jfloat depth) {
	JAGGL_LOCK(env);

	glClearDepth((GLclampd) depth);

	JAGGL_UNLOCK(env);
}

JNIEXPORT void JNICALL Java_jaggl_opengl_glClientActiveTexture(JNIEnv *env, jobject obj, jint texture) {
	JAGGL_LOCK(env);

	if (jaggl_glClientActiveTexture) {
		jaggl_glClientActiveTexture((GLenum) texture);
	} else if (jaggl_glClientActiveTextureARB) {
		jaggl_glClientActiveTextureARB((GLenum) texture);
	}

	JAGGL_UNLOCK(env);
}

JNIEXPORT void JNICALL Java_jaggl_opengl_glColor3ub(JNIEnv *env, jobject obj, jbyte red, jbyte green, jbyte blue) {
	JAGGL_LOCK(env);

	glColor3ub((GLubyte) red, (GLubyte) green, (GLubyte) blue);

	JAGGL_UNLOCK(env);
}

JNIEXPORT void JNICALL Java_jaggl_opengl_glColor4f(JNIEnv *env, jobject obj, jfloat red, jfloat green, jfloat blue, jfloat alpha) {
	JAGGL_LOCK(env);

	glColor4f((GLfloat) red, (GLfloat) green, (GLfloat) blue, (GLfloat) alpha);

	JAGGL_UNLOCK(env);
}

JNIEXPORT void JNICALL Java_jaggl_opengl_glColor4fv1(JNIEnv *env, jobject obj, jobject, jint);

JNIEXPORT void JNICALL Java_jaggl_opengl_glColor4ub(JNIEnv *env, jobject obj, jbyte red, jbyte green, jbyte blue, jbyte alpha) {
	JAGGL_LOCK(env);

	glColor4ub((GLubyte) red, (GLubyte) green, (GLubyte) blue, (GLubyte) alpha);

	JAGGL_UNLOCK(env);
}

JNIEXPORT void JNICALL Java_jaggl_opengl_glColorMask(JNIEnv *env, jobject obj, jboolean red, jboolean green, jboolean blue, jboolean alpha) {
	JAGGL_LOCK(env);

	glColorMask((GLboolean) red, (GLboolean) green, (GLboolean) blue, (GLboolean) alpha);

	JAGGL_UNLOCK(env);
}

JNIEXPORT void JNICALL Java_jaggl_opengl_glColorMaterial(JNIEnv *env, jobject obj, jint face, jint mode) {
	JAGGL_LOCK(env);

	glColorMaterial((GLenum) face, (GLenum) mode);

	JAGGL_UNLOCK(env);
}

JNIEXPORT void JNICALL Java_jaggl_opengl_glColorPointer(JNIEnv *env, jobject obj, jint, jint, jint, jlong);
JNIEXPORT void JNICALL Java_jaggl_opengl_glColorPointer0(JNIEnv *env, jobject obj, jint, jint, jint, jobject, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glColorPointer1(JNIEnv *env, jobject obj, jint, jint, jint, jobject, jint);

JNIEXPORT void JNICALL Java_jaggl_opengl_glCompileShaderARB(JNIEnv *env, jobject obj, jint shader) {
	JAGGL_LOCK(env);

	if (jaggl_glCompileShaderARB) {
		jaggl_glCompileShaderARB((GLhandleARB) shader);
	}

	JAGGL_UNLOCK(env);
}

JNIEXPORT void JNICALL Java_jaggl_opengl_glCopyPixels(JNIEnv *env, jobject obj, jint x, jint y, jint width, jint height, jint type) {
	JAGGL_LOCK(env);

	glCopyPixels((GLint) x, (GLint) y, (GLsizei) width, (GLsizei) height, (GLenum) type);

	JAGGL_UNLOCK(env);
}

JNIEXPORT void JNICALL Java_jaggl_opengl_glCopyTexImage2D(JNIEnv *env, jobject obj, jint target, jint level, jint internalformat, jint x, jint y, jint width, jint height, jint border) {
	JAGGL_LOCK(env);

	glCopyTexImage2D((GLenum) target, (GLint) level, (GLenum) internalformat, (GLint) x, (GLint) y, (GLsizei) width, (GLsizei) height, (GLint) border);

	JAGGL_UNLOCK(env);
}

JNIEXPORT jint JNICALL Java_jaggl_opengl_glCreateProgramObjectARB(JNIEnv *env, jobject obj) {
	JAGGL_LOCK(env);

	GLhandleARB result;
	if (jaggl_glCreateProgramObjectARB) {
		result = jaggl_glCreateProgramObjectARB();
	} else {
		result = 0;
	}

	JAGGL_UNLOCK(env);
	return (jint) result;
}

JNIEXPORT jint JNICALL Java_jaggl_opengl_glCreateShaderObjectARB(JNIEnv *env, jobject obj, jint type) {
	JAGGL_LOCK(env);

	GLhandleARB result;
	if (jaggl_glCreateShaderObjectARB) {
		result = jaggl_glCreateShaderObjectARB((GLenum) type);
	} else {
		result = 0;
	}

	JAGGL_UNLOCK(env);
	return (jint) result;
}

JNIEXPORT void JNICALL Java_jaggl_opengl_glCullFace(JNIEnv *env, jobject obj, jint mode) {
	JAGGL_LOCK(env);

	glCullFace((GLenum) mode);

	JAGGL_UNLOCK(env);
}

JNIEXPORT void JNICALL Java_jaggl_opengl_glDeleteBuffersARB1(JNIEnv *env, jobject obj, jint, jobject, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glDeleteFramebuffersEXT1(JNIEnv *env, jobject obj, jint, jobject, jint);

JNIEXPORT void JNICALL Java_jaggl_opengl_glDeleteLists(JNIEnv *env, jobject obj, jint list, jint range) {
	JAGGL_LOCK(env);

	glDeleteLists((GLuint) list, (GLsizei) range);

	JAGGL_UNLOCK(env);
}

JNIEXPORT void JNICALL Java_jaggl_opengl_glDeleteObjectARB(JNIEnv *env, jobject obj, jint object) {
	JAGGL_LOCK(env);

	if (jaggl_glDeleteObjectARB) {
		jaggl_glDeleteObjectARB((GLhandleARB) object);
	}

	JAGGL_UNLOCK(env);
}

JNIEXPORT void JNICALL Java_jaggl_opengl_glDeleteRenderbuffersEXT1(JNIEnv *env, jobject obj, jint, jobject, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glDeleteTextures1(JNIEnv *env, jobject obj, jint, jobject, jint);

JNIEXPORT void JNICALL Java_jaggl_opengl_glDepthFunc(JNIEnv *env, jobject obj, jint func) {
	JAGGL_LOCK(env);

	glDepthFunc((GLenum) func);

	JAGGL_UNLOCK(env);
}

JNIEXPORT void JNICALL Java_jaggl_opengl_glDepthMask(JNIEnv *env, jobject obj, jboolean flag) {
	JAGGL_LOCK(env);

	glDepthMask((GLboolean) flag);

	JAGGL_UNLOCK(env);
}

JNIEXPORT void JNICALL Java_jaggl_opengl_glDetachObjectARB(JNIEnv *env, jobject obj, jint container_obj, jint attached_obj) {
	JAGGL_LOCK(env);

	if (jaggl_glDetachObjectARB) {
		jaggl_glDetachObjectARB((GLhandleARB) container_obj, (GLhandleARB) attached_obj);
	}

	JAGGL_UNLOCK(env);
}

JNIEXPORT void JNICALL Java_jaggl_opengl_glDisable(JNIEnv *env, jobject obj, jint cap) {
	JAGGL_LOCK(env);

	glDisable((GLenum) cap);

	JAGGL_UNLOCK(env);
}

JNIEXPORT void JNICALL Java_jaggl_opengl_glDisableClientState(JNIEnv *env, jobject obj, jint cap) {
	JAGGL_LOCK(env);

	glDisableClientState((GLenum) cap);

	JAGGL_UNLOCK(env);
}

JNIEXPORT void JNICALL Java_jaggl_opengl_glDrawArrays(JNIEnv *env, jobject obj, jint mode, jint first, jint count) {
	JAGGL_LOCK(env);

	glDrawArrays((GLenum) mode, (GLint) first, (GLsizei) count);

	JAGGL_UNLOCK(env);
}

JNIEXPORT void JNICALL Java_jaggl_opengl_glDrawBuffer(JNIEnv *env, jobject obj, jint mode) {
	JAGGL_LOCK(env);

	glDrawBuffer((GLenum) mode);

	JAGGL_UNLOCK(env);
}

JNIEXPORT void JNICALL Java_jaggl_opengl_glDrawElements(JNIEnv *env, jobject obj, jint, jint, jint, jlong);
JNIEXPORT void JNICALL Java_jaggl_opengl_glDrawElements0(JNIEnv *env, jobject obj, jint, jint, jint, jobject, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glDrawElements1(JNIEnv *env, jobject obj, jint, jint, jint, jobject, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glDrawPixels0(JNIEnv *env, jobject obj, jint, jint, jint, jint, jobject, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glDrawPixels1(JNIEnv *env, jobject obj, jint, jint, jint, jint, jobject, jint);

JNIEXPORT void JNICALL Java_jaggl_opengl_glEnable(JNIEnv *env, jobject obj, jint cap) {
	JAGGL_LOCK(env);

	glEnable((GLenum) cap);

	JAGGL_UNLOCK(env);
}

JNIEXPORT void JNICALL Java_jaggl_opengl_glEnableClientState(JNIEnv *env, jobject obj, jint cap) {
	JAGGL_LOCK(env);

	glEnableClientState((GLenum) cap);

	JAGGL_UNLOCK(env);
}

JNIEXPORT void JNICALL Java_jaggl_opengl_glEnd(JNIEnv *env, jobject obj) {
	JAGGL_LOCK(env);

	glEnd();

	JAGGL_UNLOCK(env);
}

JNIEXPORT void JNICALL Java_jaggl_opengl_glEndList(JNIEnv *env, jobject obj) {
	JAGGL_LOCK(env);

	glEndList();

	JAGGL_UNLOCK(env);
}

JNIEXPORT void JNICALL Java_jaggl_opengl_glFogf(JNIEnv *env, jobject obj, jint pname, jfloat param) {
	JAGGL_LOCK(env);

	glFogf((GLenum) pname, (GLfloat) param);

	JAGGL_UNLOCK(env);
}

JNIEXPORT void JNICALL Java_jaggl_opengl_glFogfv1(JNIEnv *env, jobject obj, jint, jobject, jint);

JNIEXPORT void JNICALL Java_jaggl_opengl_glFogi(JNIEnv *env, jobject obj, jint pname, jint param) {
	JAGGL_LOCK(env);

	glFogi((GLenum) pname, (GLint) param);

	JAGGL_UNLOCK(env);
}

JNIEXPORT void JNICALL Java_jaggl_opengl_glFramebufferRenderbufferEXT(JNIEnv *env, jobject obj, jint target, jint attachment, jint renderbuffer_target, jint renderbuffer) {
	JAGGL_LOCK(env);

	if (jaggl_glFramebufferRenderbufferEXT) {
		jaggl_glFramebufferRenderbufferEXT((GLenum) target, (GLenum) attachment, (GLenum) renderbuffer_target, (GLuint) renderbuffer);
	}

	JAGGL_UNLOCK(env);
}

JNIEXPORT void JNICALL Java_jaggl_opengl_glFramebufferTexture2DEXT(JNIEnv *env, jobject obj, jint target, jint attachment, jint tex_target, jint texture, jint level) {
	JAGGL_LOCK(env);

	if (jaggl_glFramebufferTexture2DEXT) {
		jaggl_glFramebufferTexture2DEXT((GLenum) target, (GLenum) attachment, (GLenum) tex_target, (GLuint) texture, (GLint) level);
	}

	JAGGL_UNLOCK(env);
}

JNIEXPORT void JNICALL Java_jaggl_opengl_glGenBuffersARB1(JNIEnv *env, jobject obj, jint, jobject, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glGenFramebuffersEXT1(JNIEnv *env, jobject obj, jint, jobject, jint);

JNIEXPORT jint JNICALL Java_jaggl_opengl_glGenLists(JNIEnv *env, jobject obj, jint range) {
	JAGGL_LOCK(env);

	GLuint result = glGenLists((GLsizei) range);

	JAGGL_UNLOCK(env);
	return (jint) result;
}

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

JNIEXPORT void JNICALL Java_jaggl_opengl_glHint(JNIEnv *env, jobject obj, jint target, jint mode) {
	JAGGL_LOCK(env);

	glHint((GLenum) target, (GLenum) mode);

	JAGGL_UNLOCK(env);
}

JNIEXPORT void JNICALL Java_jaggl_opengl_glInterleavedArrays(JNIEnv *env, jobject obj, jint, jint, jlong);
JNIEXPORT void JNICALL Java_jaggl_opengl_glInterleavedArrays0(JNIEnv *env, jobject obj, jint, jint, jobject, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glInterleavedArrays1(JNIEnv *env, jobject obj, jint, jint, jobject, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glLightModelfv1(JNIEnv *env, jobject obj, jint, jobject, jint);

JNIEXPORT void JNICALL Java_jaggl_opengl_glLightf(JNIEnv *env, jobject obj, jint light, jint pname, jfloat param) {
	JAGGL_LOCK(env);

	glLightf((GLenum) light, (GLenum) pname, (GLfloat) param);

	JAGGL_UNLOCK(env);
}

JNIEXPORT void JNICALL Java_jaggl_opengl_glLightfv1(JNIEnv *env, jobject obj, jint, jint, jobject, jint);

JNIEXPORT void JNICALL Java_jaggl_opengl_glLineWidth(JNIEnv *env, jobject obj, jfloat width) {
	JAGGL_LOCK(env);

	glLineWidth((GLfloat) width);

	JAGGL_UNLOCK(env);
}

JNIEXPORT void JNICALL Java_jaggl_opengl_glLinkProgramARB(JNIEnv *env, jobject obj, jint program_obj) {
	JAGGL_LOCK(env);

	if (jaggl_glLinkProgramARB) {
		jaggl_glLinkProgramARB((GLhandleARB) program_obj);
	}

	JAGGL_UNLOCK(env);
}

JNIEXPORT void JNICALL Java_jaggl_opengl_glLoadIdentity(JNIEnv *env, jobject obj) {
	JAGGL_LOCK(env);

	glLoadIdentity();

	JAGGL_UNLOCK(env);
}

JNIEXPORT void JNICALL Java_jaggl_opengl_glLoadMatrixf1(JNIEnv *env, jobject obj, jobject, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glMaterialfv1(JNIEnv *env, jobject obj, jint, jint, jobject, jint);

JNIEXPORT void JNICALL Java_jaggl_opengl_glMatrixMode(JNIEnv *env, jobject obj, jint mode) {
	JAGGL_LOCK(env);

	glMatrixMode((GLenum) mode);

	JAGGL_UNLOCK(env);
}

JNIEXPORT void JNICALL Java_jaggl_opengl_glMultiTexCoord2f(JNIEnv *env, jobject obj, jint target, jfloat s, jfloat t) {
	JAGGL_LOCK(env);

	if (jaggl_glMultiTexCoord2f) {
		jaggl_glMultiTexCoord2f((GLenum) target, (GLfloat) s, (GLfloat) t);
	} else if (jaggl_glMultiTexCoord2fARB) {
		jaggl_glMultiTexCoord2fARB((GLenum) target, (GLfloat) s, (GLfloat) t);
	}

	JAGGL_UNLOCK(env);
}

JNIEXPORT void JNICALL Java_jaggl_opengl_glMultiTexCoord2i(JNIEnv *env, jobject obj, jint target, jint s, jint t) {
	JAGGL_LOCK(env);

	if (jaggl_glMultiTexCoord2i) {
		jaggl_glMultiTexCoord2i((GLenum) target, (GLint) s, (GLint) t);
	} else if (jaggl_glMultiTexCoord2iARB) {
		jaggl_glMultiTexCoord2iARB((GLenum) target, (GLint) s, (GLint) t);
	}

	JAGGL_UNLOCK(env);
}

JNIEXPORT void JNICALL Java_jaggl_opengl_glNewList(JNIEnv *env, jobject obj, jint list, jint mode) {
	JAGGL_LOCK(env);

	glNewList((GLuint) list, (GLenum) mode);

	JAGGL_UNLOCK(env);
}

JNIEXPORT void JNICALL Java_jaggl_opengl_glNormal3f(JNIEnv *env, jobject obj, jfloat nx, jfloat ny, jfloat nz) {
	JAGGL_LOCK(env);

	glNormal3f((GLfloat) nx, (GLfloat) ny, (GLfloat) nz);

	JAGGL_UNLOCK(env);
}

JNIEXPORT void JNICALL Java_jaggl_opengl_glNormalPointer(JNIEnv *env, jobject obj, jint, jint, jlong);
JNIEXPORT void JNICALL Java_jaggl_opengl_glNormalPointer0(JNIEnv *env, jobject obj, jint, jint, jobject, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glNormalPointer1(JNIEnv *env, jobject obj, jint, jint, jobject, jint);

JNIEXPORT void JNICALL Java_jaggl_opengl_glOrtho(JNIEnv *env, jobject obj, jdouble l, jdouble r, jdouble b, jdouble t, jdouble n, jdouble f) {
	JAGGL_LOCK(env);

	glOrtho((GLdouble) l, (GLdouble) r, (GLdouble) b, (GLdouble) t, (GLdouble) n, (GLdouble) f);

	JAGGL_UNLOCK(env);
}

JNIEXPORT void JNICALL Java_jaggl_opengl_glPointParameterfARB(JNIEnv *env, jobject obj, jint pname, jfloat param) {
	JAGGL_LOCK(env);

	if (jaggl_glPointParameterfARB) {
		jaggl_glPointParameterfARB((GLenum) pname, (GLfloat) param);
	}

	JAGGL_UNLOCK(env);
}

JNIEXPORT void JNICALL Java_jaggl_opengl_glPointParameterfvARB1(JNIEnv *env, jobject obj, jint, jobject, jint);

JNIEXPORT void JNICALL Java_jaggl_opengl_glPointSize(JNIEnv *env, jobject obj, jfloat size) {
	JAGGL_LOCK(env);

	glPointSize((GLfloat) size);

	JAGGL_UNLOCK(env);
}

JNIEXPORT void JNICALL Java_jaggl_opengl_glPolygonMode(JNIEnv *env, jobject obj, jint face, jint mode) {
	JAGGL_LOCK(env);

	glPolygonMode((GLenum) face, (GLenum) mode);

	JAGGL_UNLOCK(env);
}

JNIEXPORT void JNICALL Java_jaggl_opengl_glPopAttrib(JNIEnv *env, jobject obj) {
	JAGGL_LOCK(env);

	glPopAttrib();

	JAGGL_UNLOCK(env);
}

JNIEXPORT void JNICALL Java_jaggl_opengl_glPopMatrix(JNIEnv *env, jobject obj) {
	JAGGL_LOCK(env);

	glPopMatrix();

	JAGGL_UNLOCK(env);
}

JNIEXPORT void JNICALL Java_jaggl_opengl_glProgramLocalParameter4fARB(JNIEnv *env, jobject obj, jint target, jint index, jfloat x, jfloat y, jfloat z, jfloat w) {
	JAGGL_LOCK(env);

	if (jaggl_glProgramLocalParameter4fARB) {
		jaggl_glProgramLocalParameter4fARB((GLenum) target, (GLuint) index, (GLfloat) x, (GLfloat) y, (GLfloat) z, (GLfloat) w);;
	}

	JAGGL_UNLOCK(env);
}

JNIEXPORT void JNICALL Java_jaggl_opengl_glProgramLocalParameter4fvARB0(JNIEnv *env, jobject obj, jint, jint, jobject, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glProgramLocalParameter4fvARB1(JNIEnv *env, jobject obj, jint, jint, jobject, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glProgramStringARB(JNIEnv *env, jobject obj, jint, jint, jint, jstring);

JNIEXPORT void JNICALL Java_jaggl_opengl_glPushAttrib(JNIEnv *env, jobject obj, jint mask) {
	JAGGL_LOCK(env);

	glPushAttrib((GLbitfield) mask);

	JAGGL_UNLOCK(env);
}

JNIEXPORT void JNICALL Java_jaggl_opengl_glPushMatrix(JNIEnv *env, jobject obj) {
	JAGGL_LOCK(env);

	glPushMatrix();

	JAGGL_UNLOCK(env);
}

JNIEXPORT void JNICALL Java_jaggl_opengl_glRasterPos2i(JNIEnv *env, jobject obj, jint x, jint y) {
	JAGGL_LOCK(env);

	glRasterPos2i((GLint) x, (GLint) y);

	JAGGL_UNLOCK(env);
}

JNIEXPORT void JNICALL Java_jaggl_opengl_glReadBuffer(JNIEnv *env, jobject obj, jint mode) {
	JAGGL_LOCK(env);

	glReadBuffer((GLenum) mode);

	JAGGL_UNLOCK(env);
}

JNIEXPORT void JNICALL Java_jaggl_opengl_glRenderbufferStorageEXT(JNIEnv *env, jobject obj, jint target, jint internalformat, jint width, jint height) {
	JAGGL_LOCK(env);

	if (jaggl_glRenderbufferStorageEXT) {
		jaggl_glRenderbufferStorageEXT((GLenum) target, (GLenum) internalformat, (GLsizei) width, (GLsizei) height);
	}

	JAGGL_UNLOCK(env);
}

JNIEXPORT void JNICALL Java_jaggl_opengl_glRotatef(JNIEnv *env, jobject obj, jfloat angle, jfloat x, jfloat y, jfloat z) {
	JAGGL_LOCK(env);

	glRotatef((GLfloat) angle, (GLfloat) x, (GLfloat) y, (GLfloat) z);

	JAGGL_UNLOCK(env);
}

JNIEXPORT void JNICALL Java_jaggl_opengl_glScalef(JNIEnv *env, jobject obj, jfloat x, jfloat y, jfloat z) {
	JAGGL_LOCK(env);

	glScalef((GLfloat) x, (GLfloat) y, (GLfloat) z);

	JAGGL_UNLOCK(env);
}

JNIEXPORT void JNICALL Java_jaggl_opengl_glScissor(JNIEnv *env, jobject obj, jint x, jint y, jint width, jint height) {
	JAGGL_LOCK(env);

	glScissor((GLint) x, (GLint) y, (GLsizei) width, (GLsizei) height);

	JAGGL_UNLOCK(env);
}

JNIEXPORT void JNICALL Java_jaggl_opengl_glShadeModel(JNIEnv *env, jobject obj, jint mode) {
	JAGGL_LOCK(env);

	glShadeModel((GLenum) mode);

	JAGGL_UNLOCK(env);
}

JNIEXPORT void JNICALL Java_jaggl_opengl_glShaderSourceARB0(JNIEnv *env, jobject obj, jint, jint, jobject, jintArray, jint);

JNIEXPORT void JNICALL Java_jaggl_opengl_glTexCoord2f(JNIEnv *env, jobject obj, jfloat s, jfloat t) {
	JAGGL_LOCK(env);

	glTexCoord2f((GLfloat) s, (GLfloat) t);

	JAGGL_UNLOCK(env);
}

JNIEXPORT void JNICALL Java_jaggl_opengl_glTexCoord2i(JNIEnv *env, jobject obj, jint s, jint t) {
	JAGGL_LOCK(env);

	glTexCoord2i((GLint) s, (GLint) t);

	JAGGL_UNLOCK(env);
}

JNIEXPORT void JNICALL Java_jaggl_opengl_glTexCoordPointer(JNIEnv *env, jobject obj, jint, jint, jint, jlong);
JNIEXPORT void JNICALL Java_jaggl_opengl_glTexCoordPointer0(JNIEnv *env, jobject obj, jint, jint, jint, jobject, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glTexCoordPointer1(JNIEnv *env, jobject obj, jint, jint, jint, jobject, jint);

JNIEXPORT void JNICALL Java_jaggl_opengl_glTexEnvf(JNIEnv *env, jobject obj, jint target, jint pname, jfloat param) {
	JAGGL_LOCK(env);

	glTexEnvf((GLenum) target, (GLenum) pname, (GLfloat) param);

	JAGGL_UNLOCK(env);
}

JNIEXPORT void JNICALL Java_jaggl_opengl_glTexEnvfv1(JNIEnv *env, jobject obj, jint, jint, jobject, jint);

JNIEXPORT void JNICALL Java_jaggl_opengl_glTexEnvi(JNIEnv *env, jobject obj, jint target, jint pname, jint param) {
	JAGGL_LOCK(env);

	glTexEnvi((GLenum) target, (GLenum) pname, (GLint) param);

	JAGGL_UNLOCK(env);
}

JNIEXPORT void JNICALL Java_jaggl_opengl_glTexGenfv1(JNIEnv *env, jobject obj, jint, jint, jobject, jint);

JNIEXPORT void JNICALL Java_jaggl_opengl_glTexGeni(JNIEnv *env, jobject obj, jint coord, jint pname, jint param) {
	JAGGL_LOCK(env);

	glTexGeni((GLenum) coord, (GLenum) pname, (GLint) param);

	JAGGL_UNLOCK(env);
}

JNIEXPORT void JNICALL Java_jaggl_opengl_glTexImage1D0(JNIEnv *env, jobject obj, jint, jint, jint, jint, jint, jint, jint, jobject, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glTexImage1D1(JNIEnv *env, jobject obj, jint, jint, jint, jint, jint, jint, jint, jobject, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glTexImage2D0(JNIEnv *env, jobject obj, jint, jint, jint, jint, jint, jint, jint, jint, jobject, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glTexImage2D1(JNIEnv *env, jobject obj, jint, jint, jint, jint, jint, jint, jint, jint, jobject, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glTexImage3D0(JNIEnv *env, jobject obj, jint, jint, jint, jint, jint, jint, jint, jint, jint, jobject, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glTexImage3D1(JNIEnv *env, jobject obj, jint, jint, jint, jint, jint, jint, jint, jint, jint, jobject, jint);

JNIEXPORT void JNICALL Java_jaggl_opengl_glTexParameteri(JNIEnv *env, jobject obj, jint target, jint pname, jint param) {
	JAGGL_LOCK(env);

	glTexParameteri((GLenum) target, (GLenum) pname, (GLint) param);

	JAGGL_UNLOCK(env);
}

JNIEXPORT void JNICALL Java_jaggl_opengl_glTranslatef(JNIEnv *env, jobject obj, jfloat x, jfloat y, jfloat z) {
	JAGGL_LOCK(env);

	glTranslatef((GLfloat) x, (GLfloat) y, (GLfloat) z);

	JAGGL_UNLOCK(env);
}

JNIEXPORT void JNICALL Java_jaggl_opengl_glUniform1iARB(JNIEnv *env, jobject obj, jint location, jint v0) {
	JAGGL_LOCK(env);

	if (jaggl_glUniform1iARB) {
		jaggl_glUniform1iARB((GLint) location, (GLint) v0);
	}

	JAGGL_UNLOCK(env);
}

JNIEXPORT void JNICALL Java_jaggl_opengl_glUniform3fARB(JNIEnv *env, jobject obj, jint location, jfloat v0, jfloat v1, jfloat v2) {
	JAGGL_LOCK(env);

	if (jaggl_glUniform3fARB) {
		jaggl_glUniform3fARB((GLint) location, (GLfloat) v0, (GLfloat) v1, (GLfloat) v2);
	}

	JAGGL_UNLOCK(env);
}

JNIEXPORT void JNICALL Java_jaggl_opengl_glUseProgramObjectARB(JNIEnv *env, jobject obj, jint program_obj) {
	JAGGL_LOCK(env);

	if (jaggl_glUseProgramObjectARB) {
		jaggl_glUseProgramObjectARB((GLhandleARB) program_obj);
	}

	JAGGL_UNLOCK(env);
}

JNIEXPORT void JNICALL Java_jaggl_opengl_glVertex2f(JNIEnv *env, jobject obj, jfloat x, jfloat y) {
	JAGGL_LOCK(env);

	glVertex2f((GLfloat) x, (GLfloat) y);

	JAGGL_UNLOCK(env);
}

JNIEXPORT void JNICALL Java_jaggl_opengl_glVertex2i(JNIEnv *env, jobject obj, jint x, jint y) {
	JAGGL_LOCK(env);

	glVertex2i((GLint) x, (GLint) y);

	JAGGL_UNLOCK(env);
}

JNIEXPORT void JNICALL Java_jaggl_opengl_glVertexPointer(JNIEnv *env, jobject obj, jint, jint, jint, jlong);
JNIEXPORT void JNICALL Java_jaggl_opengl_glVertexPointer0(JNIEnv *env, jobject obj, jint, jint, jint, jobject, jint);
JNIEXPORT void JNICALL Java_jaggl_opengl_glVertexPointer1(JNIEnv *env, jobject obj, jint, jint, jint, jobject, jint);

JNIEXPORT void JNICALL Java_jaggl_opengl_glViewport(JNIEnv *env, jobject obj, jint x, jint y, jint width, jint height) {
	JAGGL_LOCK(env);

	glViewport((GLint) x, (GLint) y, (GLsizei) width, (GLsizei) height);

	JAGGL_UNLOCK(env);
}
