#include "jagex3_jagmisc_jagmisc.h"

#include <stdbool.h>
#include <stdint.h>
#include <stdlib.h>
#include <windows.h>
#include <VersionHelpers.h>

#define WAIT_OBJECT_1 (WAIT_OBJECT_0 + 1)

#define JAGMISC_SPIN_COUNT 4000
#define JAGMISC_NANOS_PER_SEC 1000000000LL

// TODO(gpe): do any of these need to be volatile?
static bool jagmisc_unpinned;
static LARGE_INTEGER jagmisc_freq;
static CRITICAL_SECTION jagmisc_lock;
static HANDLE jagmisc_start_event;
static HANDLE jagmisc_time_request_event;
static HANDLE jagmisc_time_ready_event;
static HANDLE jagmisc_stop_event;
static HANDLE jagmisc_thread;
static int64_t jagmisc_time;

static inline int64_t jagmisc_to_nanos(LARGE_INTEGER count) {
	return count.QuadPart * JAGMISC_NANOS_PER_SEC / jagmisc_freq.QuadPart;
}

static DWORD WINAPI jagmisc_run(LPVOID parameter) {
	/* pin this thread to the system's first processor */
	HANDLE thread = GetCurrentThread();

	DWORD_PTR affinity_mask = 0x1;
	bool success = SetThreadAffinityMask(thread, affinity_mask);

	CloseHandle(thread);

	if (!success) {
		return EXIT_FAILURE;
	}

	/* notify Java_jagex3_jagmisc_jagmisc_init() that the thread has started successfully */
	if (!SetEvent(jagmisc_start_event)) {
		return EXIT_FAILURE;
	}

	/* serve time request and stop events */
	const HANDLE handles[] = { jagmisc_time_request_event, jagmisc_stop_event };
	for (;;) {
		switch (WaitForMultipleObjects(2, handles, FALSE, INFINITE)) {
		case WAIT_OBJECT_0:
			LARGE_INTEGER count;
			if (QueryPerformanceCounter(&count)) {
				jagmisc_time = jagmisc_to_nanos(count);
			} else {
				jagmisc_time = 0;
			}

			if (!SetEvent(jagmisc_time_ready_event)) {
				return EXIT_FAILURE;
			}
			break;
		case WAIT_OBJECT_1:
			return EXIT_SUCCESS;
		default:
			return EXIT_FAILURE;
		}
	}
}

static void jagmisc_stop(void) {
	// TODO(gpe): think about what to do if we fail to stop the thread. We
	// could call TerminateThread (which sounds dangerous based on the
	// documentation) or throw a Java exception. The current behaviour of
	// ignoring the failure is okay as long as jagmisc.init() is never called
	// again in the same JVM.
	if (SetEvent(jagmisc_stop_event)) {
		WaitForSingleObject(jagmisc_thread, INFINITE);
	}

	CloseHandle(jagmisc_thread);
	jagmisc_thread = NULL;

	CloseHandle(jagmisc_stop_event);
	jagmisc_stop_event = NULL;

	CloseHandle(jagmisc_time_ready_event);
	jagmisc_time_ready_event = NULL;

	CloseHandle(jagmisc_time_request_event);
	jagmisc_time_request_event = NULL;

	CloseHandle(jagmisc_start_event);
	jagmisc_start_event = NULL;
}

BOOL WINAPI DllMain(HINSTANCE hinstDLL, DWORD fdwReason, LPVOID lpvReserved) {
	if (fdwReason == DLL_PROCESS_ATTACH) {
		/*
		 * We only need to pin the timer thread to a single processor prior to
		 * Windows Vista.
		 */
		jagmisc_unpinned = IsWindowsVistaOrGreater();
		if (jagmisc_unpinned) {
			return TRUE;
		}

		if (!InitializeCriticalSectionAndSpinCount(&jagmisc_lock, JAGMISC_SPIN_COUNT)) {
			return FALSE;
		}
	}
	return TRUE;
}

JNIEXPORT jboolean JNICALL Java_jagex3_jagmisc_jagmisc_init(JNIEnv *env, jclass cls) {
	if (jagmisc_unpinned) {
		return (jboolean) QueryPerformanceFrequency(&jagmisc_freq);
	}

	EnterCriticalSection(&jagmisc_lock);
	jboolean success = JNI_FALSE;

	if (jagmisc_thread) {
		jagmisc_stop();
	}

	if (!QueryPerformanceFrequency(&jagmisc_freq)) {
		goto unlock;
	}

	jagmisc_start_event = CreateEventA(NULL, FALSE, FALSE, NULL);
	if (!jagmisc_start_event) {
		goto unlock;
	}

	jagmisc_time_request_event = CreateEventA(NULL, FALSE, FALSE, NULL);
	if (!jagmisc_time_request_event) {
		goto create_time_request_event_failed;
	}

	jagmisc_time_ready_event = CreateEventA(NULL, FALSE, FALSE, NULL);
	if (!jagmisc_time_ready_event) {
		goto create_time_ready_event_failed;
	}

	jagmisc_stop_event = CreateEventA(NULL, FALSE, FALSE, NULL);
	if (!jagmisc_stop_event) {
		goto create_stop_event_failed;
	}

	jagmisc_thread = CreateThread(NULL, 0, &jagmisc_run, NULL, 0, NULL);
	if (!jagmisc_thread) {
		goto create_thread_failed;
	}

	const HANDLE handles[] = { jagmisc_start_event, jagmisc_thread };
	DWORD result = WaitForMultipleObjects(2, handles, FALSE, INFINITE);
	if (result != WAIT_OBJECT_0) {
		goto start_failed;
	}

	success = JNI_TRUE;
	goto unlock;

start_failed:
	CloseHandle(jagmisc_thread);
	jagmisc_thread = NULL;
create_thread_failed:
	CloseHandle(jagmisc_stop_event);
	jagmisc_stop_event = NULL;
create_stop_event_failed:
	CloseHandle(jagmisc_time_ready_event);
	jagmisc_time_ready_event = NULL;
create_time_ready_event_failed:
	CloseHandle(jagmisc_time_request_event);
	jagmisc_time_request_event = NULL;
create_time_request_event_failed:
	CloseHandle(jagmisc_start_event);
	jagmisc_start_event = NULL;
unlock:
	LeaveCriticalSection(&jagmisc_lock);
	return success;
}

JNIEXPORT void JNICALL Java_jagex3_jagmisc_jagmisc_Quit0(JNIEnv *env, jclass cls) {
	if (jagmisc_unpinned) {
		return;
	}

	EnterCriticalSection(&jagmisc_lock);
	if (jagmisc_thread) {
		jagmisc_stop();
	}
	LeaveCriticalSection(&jagmisc_lock);
}

JNIEXPORT jlong JNICALL Java_jagex3_jagmisc_jagmisc_nanoTime(JNIEnv *env, jclass cls) {
	if (jagmisc_unpinned) {
		LARGE_INTEGER count;
		if (!QueryPerformanceCounter(&count)) {
			return 0;
		}
		return jagmisc_to_nanos(count);
	}

	EnterCriticalSection(&jagmisc_lock);
	jlong time = 0;

	if (!jagmisc_thread) {
		goto unlock;
	}

	if (!SetEvent(jagmisc_time_request_event)) {
		goto unlock;
	}

	const HANDLE handles[] = { jagmisc_time_ready_event, jagmisc_thread };
	DWORD result = WaitForMultipleObjects(2, handles, FALSE, INFINITE);
	if (result != WAIT_OBJECT_0) {
		goto unlock;
	}

	time = jagmisc_time;

unlock:
	LeaveCriticalSection(&jagmisc_lock);
	return time;
}
