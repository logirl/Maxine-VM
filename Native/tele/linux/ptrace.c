/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product
 * that is described in this document. In particular, and without limitation, these intellectual property
 * rights may include one or more of the U.S. patents listed at http://www.sun.com/patents and one or
 * more additional patents or pending patent applications in the U.S. and in other countries.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun
 * Microsystems, Inc. standard license agreement and applicable provisions of the FAR and its
 * supplements.
 *
 * Use is subject to license terms. Sun, Sun Microsystems, the Sun logo, Java and Solaris are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other countries. All SPARC trademarks
 * are used under license and are trademarks or registered trademarks of SPARC International, Inc. in the
 * U.S. and other countries.
 *
 * UNIX is a registered trademark in the U.S. and other countries, exclusively licensed through X/Open
 * Company, Ltd.
 */

#include <stdlib.h>
#include <sys/types.h>
#include <sys/ptrace.h>
#include <errno.h>
#include <unistd.h>
#include <string.h>
#include <syscall.h>

#include "word.h"
#include "log.h"

/* Some Linux versions are missing the following constants in <sys/ptrace.h>  */
#ifndef PTRACE_EVENT_FORK

#define PTRACE_O_TRACESYSGOOD   0x00000001
#define PTRACE_O_TRACEFORK      0x00000002
#define PTRACE_O_TRACEVFORK     0x00000004
#define PTRACE_O_TRACECLONE     0x00000008
#define PTRACE_O_TRACEEXEC      0x00000010
#define PTRACE_O_TRACEVFORKDONE 0x00000020
#define PTRACE_O_TRACEEXIT      0x00000040
#define PTRACE_O_MASK           0x0000007f

#define PTRACE_EVENT_FORK       1
#define PTRACE_EVENT_VFORK      2
#define PTRACE_EVENT_CLONE      3
#define PTRACE_EVENT_EXEC       4
#define PTRACE_EVENT_VFORK_DONE 5
#define PTRACE_EVENT_EXIT       6

#endif

static const char* requestToString(int request, char *unknownRequestNameBuf, int unknownRequestNameBufLength) {
#define CASE(req) case req: return STRINGIZE(req)
    switch (request) {
        CASE(PT_TRACE_ME);
        CASE(PT_READ_I);
        CASE(PT_READ_D);
        CASE(PT_READ_U);
        CASE(PT_WRITE_I);
        CASE(PT_WRITE_D);
        CASE(PT_WRITE_U);
        CASE(PT_CONTINUE);
        CASE(PT_KILL);
        CASE(PT_STEP);
        CASE(PT_ATTACH);
        CASE(PT_DETACH);
        CASE(PT_GETREGS);
        CASE(PT_SETREGS);
        CASE(PT_GETFPREGS);
        CASE(PT_SETOPTIONS);
        CASE(PT_GETEVENTMSG);
        CASE(PT_GETSIGINFO);
        CASE(PT_SETSIGINFO);
    }
    snprintf(unknownRequestNameBuf, unknownRequestNameBufLength, "<unknown:%d>", request);
    return unknownRequestNameBuf;
#undef CASE
}

const char* ptraceEventName(int event) {
    switch (event) {
        case 0: return "NONE";
        case 1: return "PTRACE_EVENT_FORK";
        case 2: return "PTRACE_EVENT_VFORK";
        case 3: return "PTRACE_EVENT_CLONE";
        case 4: return "PTRACE_EVENT_EXEC";
        case 5: return "PTRACE_EVENT_VFORK_DONE";
        case 6: return "PTRACE_EVENT_EXIT";
    }
    return "<unknown>";
}

#if 0
/* Linux ptrace() is unreliable, but apparently retrying after descheduling helps. */
long ptrace_withRetries(int request, int processID, void *address, void *data) {
    int microSeconds = 100000;
    int i = 0;
    while (true) {
        long result = ptrace(request, processID, address, data);
        int error = errno;
        if (error == 0) {
            return result;
        }
        if (error != ESRCH || i >= 150) {
            return -1;
        }
        usleep(microSeconds);
        i++;
        if (i % 10 == 0) {
            log_println("ptrace retrying");
        }
    }
}
#else
#define ptrace_withRetries ptrace
#endif

/*
 * Used to enforce the constraint that all access of the ptraced process from the same task/thread.
 * This value is initialized in linuxTask.c.
 */
static pid_t _ptracerTask = 0;

#define POS_PARAMS const char *file, int line
#define POS __FILE__, __LINE__

void ptrace_check_tracer(POS_PARAMS, pid_t pid) {
    pid_t tid = syscall(__NR_gettid);
    if (_ptracerTask == 0) {
        _ptracerTask = tid;
    } else if(_ptracerTask != tid) {
        log_exit(11, "%s:%d: Can only ptrace %d from task %d, not task %d", file, line, pid, _ptracerTask, tid);
    }
}

long _ptrace(POS_PARAMS, int request, pid_t pid, void *address, void *data) {
    if (request != PT_TRACE_ME) {
        ptrace_check_tracer(POS, pid);
    }
    long result;
    static int lastRequest = 0;

    Boolean trace = log_TELE && (request != PT_READ_D || lastRequest != PT_READ_D);

    const char *requestName = NULL;
    char unknownRequestNameBuf[100];

    if (trace) {
        requestName = requestToString(request, unknownRequestNameBuf, sizeof(unknownRequestNameBuf));
        log_print("%s:%d ptrace(%s, %d, %p, %p)", file, line, requestName, pid, address, data);
    }
    errno = 0;
    result = ptrace_withRetries(request, pid, address, data);
    int error = errno;
    if (trace) {
        if (request == PT_READ_D || request == PT_READ_I || request == PT_READ_U) {
            log_println(" = %p", result);
        } else {
            log_print_newline();
        }
    }
    if (error != 0) {
        requestName = requestToString(request, unknownRequestNameBuf, sizeof(unknownRequestNameBuf));
        log_println("%s:%d ptrace(%s, %d, %p, %d) caused an error [%s]", file, line, requestName, pid, address, data, strerror(error));
    }
    lastRequest = request;

    /* Reset errno to its state immediately after the real ptrace call. */
    errno = error;

    return result;
}
