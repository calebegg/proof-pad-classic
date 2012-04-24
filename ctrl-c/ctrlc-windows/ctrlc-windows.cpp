// CtrlCWrapper.cpp : Defines the entry point for the console application.
//

#include <stdio.h>
#include <fcntl.h>
#include <io.h>
#include <Windows.h>
#include <string>
#include <iostream>

unsigned int edibleCtrlCs = 0;

DWORD WINAPI MonitorProcessCloseThread(LPVOID processParam)
{
	HANDLE process = (HANDLE)processParam;
	::WaitForSingleObject(process, INFINITE);
	CloseHandle(GetStdHandle(STD_INPUT_HANDLE));

	return 0;
}

BOOL MonitorProcessClose(HANDLE process)
{
	if (!process) return FALSE;

	HANDLE thread = ::CreateThread(NULL, 0, MonitorProcessCloseThread, process, 0, 0);

	return thread!=NULL;
}


BOOL CreatePipes(HANDLE * readPipe, HANDLE * writePipe)
{
	SECURITY_ATTRIBUTES saAttr;
	ZeroMemory(&saAttr, sizeof(SECURITY_ATTRIBUTES));
	saAttr.nLength = sizeof(SECURITY_ATTRIBUTES); 
	saAttr.bInheritHandle = TRUE; 
	saAttr.lpSecurityDescriptor = NULL; 

	// Create a pipe for the child process's STDOUT. 
	if ( !CreatePipe(readPipe, writePipe, &saAttr, 0) ) {
		return FALSE;
	}

	return TRUE;
}

BOOL SendFully(HANDLE sink, char *buffer, DWORD count)
{
	DWORD sent = 0;
	while (sent < count) {
		DWORD dwWritten = 0; 
		if (!WriteFile(sink, buffer + sent, count - sent, &dwWritten, NULL)) return FALSE;
		sent += dwWritten;
	} 
	return TRUE;
}

BOOL ProcessExited(HANDLE process)
{
	DWORD exitCode;
	if (!GetExitCodeProcess(process, &exitCode)) return TRUE;

	return exitCode != STILL_ACTIVE;
}

#define TERMINATE_SIGNAL 0
#define CTRL_C_SIGNAL 1

BOOL WriteToPipe(HANDLE sink, HANDLE process)
{
	char buffer[1024];

	HANDLE myStdIn = GetStdHandle(STD_INPUT_HANDLE);
	DWORD numberOfBytesRead = 0;

	while (true) {
		
		if (!ReadFile(myStdIn, &buffer, sizeof(buffer), &numberOfBytesRead, NULL)) {
			if (ProcessExited(process)) return TRUE;
			return FALSE;
		}

		DWORD sendStart = 0;
		DWORD sendEnd = 0;

		while (sendStart < numberOfBytesRead) {
			while (sendEnd < numberOfBytesRead && buffer[sendEnd]!=CTRL_C_SIGNAL && buffer[sendEnd]!=TERMINATE_SIGNAL) {
				sendEnd++;
			}

			if (!SendFully(sink, buffer + sendStart, sendEnd - sendStart)) return FALSE;

			if (sendEnd < numberOfBytesRead) {
				if (buffer[sendEnd] == TERMINATE_SIGNAL) {
					TerminateProcess(process, 0);
					return TRUE;
				} else if (buffer[sendEnd] == CTRL_C_SIGNAL) {
					// That means we hit a 0!
					edibleCtrlCs++;
					GenerateConsoleCtrlEvent(CTRL_C_EVENT, 0);
				}
			}
			sendStart = sendEnd + 1;
		}
	}
}


BOOL Go(const char *commandLine)
{
	HANDLE stdInRead, stdInWrite;

	if (!CreatePipes(&stdInRead, &stdInWrite)) return FALSE;


	PROCESS_INFORMATION piProcInfo; 
	STARTUPINFO siStartInfo;
	BOOL bSuccess = FALSE; 

	
	ZeroMemory( &piProcInfo, sizeof(PROCESS_INFORMATION) );

	ZeroMemory( &siStartInfo, sizeof(STARTUPINFO) );
	siStartInfo.cb = sizeof(STARTUPINFO); 
	siStartInfo.hStdError = GetStdHandle(STD_ERROR_HANDLE);
	siStartInfo.hStdOutput = GetStdHandle(STD_OUTPUT_HANDLE);
	siStartInfo.hStdInput = stdInRead;
	siStartInfo.dwFlags |= STARTF_USESTDHANDLES;

	siStartInfo.wShowWindow = SW_HIDE;
	siStartInfo.dwFlags |= STARTF_USESHOWWINDOW;
	// Create the child process. 

	if (!CreateProcess(NULL, 
		const_cast<char *>(commandLine),   // command line 
		NULL,          // process security attributes 
		NULL,          // primary thread security attributes 
		TRUE,          // handles are inherited 
		0,             // creation flags 
		NULL,          // use parent's environment 
		NULL,          // use parent's current directory 
		&siStartInfo,  // STARTUPINFO pointer 
		&piProcInfo))  // receives PROCESS_INFORMATION 
	{
		return FALSE;
	}

	HANDLE ghJob = CreateJobObject( NULL, NULL);

	JOBOBJECT_EXTENDED_LIMIT_INFORMATION jeli = { 0 };
	jeli.BasicLimitInformation.LimitFlags = JOB_OBJECT_LIMIT_KILL_ON_JOB_CLOSE;
	if( ghJob == NULL || SetInformationJobObject( ghJob, JobObjectExtendedLimitInformation, &jeli, sizeof(jeli)) == FALSE) {
		std::cerr << "Error initializing close-process job";
		return 1;
	}

	if (!AssignProcessToJobObject( ghJob, piProcInfo.hProcess)) {
		DWORD error = GetLastError();
		std::cerr << "AssignProcessToJobObject failed" << std::endl;
		return FALSE;
	}

	// Close handles to the child process and its primary thread.
	// Some applications might keep these handles to monitor the status
	// of the child process, for example. 

	CloseHandle(piProcInfo.hThread);
	
	MonitorProcessClose(piProcInfo.hProcess);

	WriteToPipe(stdInWrite, piProcInfo.hProcess);
	
	CloseHandle(piProcInfo.hProcess);
	CloseHandle(stdInRead);
	CloseHandle(stdInWrite);


	return 0;
}


BOOL CleanUpHandle(HANDLE & pipe)
{
	if (pipe)
	{
		BOOL success = CloseHandle(pipe);
		pipe = NULL;
		return success;
	}
	return TRUE;
}

BOOL WINAPI HandlerRoutine(__in DWORD dwCtrlType)
{
	if (edibleCtrlCs == 0 || dwCtrlType != CTRL_C_EVENT) return FALSE;

	edibleCtrlCs--;
	return TRUE;
}

int main(int argc, char* argv[])
{
	SetConsoleCtrlHandler(HandlerRoutine, TRUE);
	_setmode(_fileno(stdout), O_BINARY);
	_setmode(_fileno(stdin), O_BINARY);
	
	std::string args;
	for (int arg = 1; arg < argc; arg++) {
		if (arg > 1) args += " ";
		args += argv[arg];
	}
	
	return Go(args.c_str()) ? 0 : 1;
}
