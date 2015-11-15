#include "MyDebug.h"

#include <iostream>
#include <cstring>      // Needed for memset
#include <sys/socket.h> // Needed for the socket functions
#include <netdb.h>      // Needed for the socket functions

#include <errno.h>
#include <arpa/inet.h>
#include <pthread.h>


using namespace std;


static pthread_mutex_t commandsLock = PTHREAD_MUTEX_INITIALIZER;
static pthread_t serverThread = 0;


bool MyDebugEnabled = true;


void startClient(MyDebug *pDebug)
{
	MyDebug &debug = *pDebug;
	int status;
	struct addrinfo host_info;       // The struct that getaddrinfo() fills up with data.
	struct addrinfo *host_info_list; // Pointer to the to the linked list of host_info's.
	memset(&host_info, 0, sizeof host_info);
	
	//std::cout << "[MyDebug] Setting up the structs..." << std::endl;
	host_info.ai_family = AF_UNSPEC;     // IP version not specified. Can be both.
	host_info.ai_socktype = SOCK_STREAM; // Use SOCK_STREAM for TCP or SOCK_DGRAM for UDP.
	host_info.ai_flags = AI_PASSIVE;
	
	status = getaddrinfo(NULL, "1723", &host_info, &host_info_list);
	if (status != 0)
		std::cout << "[MyDebug] getaddrinfo error" << gai_strerror(status) << endl;
	
	//std::cout << "[MyDebug] Creating a socket..."  << std::endl;
	int socketfd = // The socket descripter
	socket(host_info_list->ai_family,
		   host_info_list->ai_socktype,
		   host_info_list->ai_protocol);
	if (socketfd == -1)
		std::cout << "[MyDebug] socket error!";
	
	//std::cout << "[MyDebug] Binding socket..."  << std::endl;
	// we make use of the setsockopt() function to make sure the port is not in use.
	// by a previous execution of our code. (see man page for more information)
	int yes = 1;
	status = setsockopt(socketfd, SOL_SOCKET, SO_REUSEADDR, &yes, sizeof(int));
	bind(socketfd, host_info_list->ai_addr, host_info_list->ai_addrlen);
	if (socketfd == -1)
		std::cout << "[MyDebug] bind error" << std::endl;
	
	//std::cout << "[MyDebug] Listen()ing for connections..."  << std::endl;
	status = listen(socketfd, 5);
	if (status == -1)
		std::cout << "[MyDebug] listen error" << std::endl;
	
	int csock;
	sockaddr_in sadr;
	socklen_t addr_size = sizeof(sockaddr_in);
	
	while (true)
	{
		cout << "[MyDebug] waiting for a connection..." << endl;
		if ((csock = accept(socketfd, (sockaddr*)&sadr, &addr_size)) != -1)
		{
			//printf("[MyDebug] Received connection from %s\n", inet_ntoa(sadr.sin_addr));
			char buffer[1024];
			ssize_t buffer_len = 1024;
			ssize_t bytecount;
			while (true)
			{
				memset(buffer, 0, buffer_len);
				if ((bytecount = recv(csock, buffer, buffer_len, 0)) == -1)
					fprintf(stderr, "[MyDebug] Error receiving data %d\n", errno);
				//printf("[MyDebug] Received bytes %ld\nReceived string \"%s\"\n", bytecount, buffer);
				if (strcmp(buffer, "quit") == 0)
					break;
				char frameStr[32];
				int frameNum = -1;
				sscanf(buffer, "%s %i", frameStr, &frameNum);
				if ((strcmp(frameStr, "frame") == 0) &&
					(frameNum >= 0))
				{
					// Копируем список команд
					pthread_mutex_lock(&commandsLock);
					vector<string> cmdsCopy = debug.commands;
					pthread_mutex_unlock(&commandsLock);
					
					// Отправляем число команд
					sprintf(buffer, "%li\n", cmdsCopy.size());
					if ((bytecount = send(csock, buffer, strlen(buffer), 0)) == -1)
						fprintf(stderr, "[MyDebug] Error sending data %d\n", errno);
					int i = 0;
					for (vector<string>::iterator it = cmdsCopy.begin(); it != cmdsCopy.end(); it++)
					{
						// Отправляем команду
						if ((bytecount = send(csock, it->data(), it->size(), 0)) == -1)
							fprintf(stderr, "[MyDebug] Error sending data %d\n", errno);
						// Отправляем Enter
						if ((bytecount = send(csock, "\n", 1, 0)) == -1)
							fprintf(stderr, "[MyDebug] Error sending data %d\n", errno);
						//printf("[MyDebug] Sending %s\n", it->data());
						i++;
					}
				}
			}
			close(csock);
		}
		else
		{
			fprintf(stderr, "[MyDebug] Error accepting %d\n", errno);
		}
		break;
	}
}

MyDebug::~MyDebug() {}

MyDebug::MyDebug() {}

void MyDebug::lockFrame()
{
	if (MyDebugEnabled)
	{
		pthread_mutex_lock(&commandsLock);
		commands.clear();
	}
}

void MyDebug::unlockFrame()
{
	if (MyDebugEnabled)
	{
		pthread_mutex_unlock(&commandsLock);
	}
}

void MyDebug::storeCommand(const char * format, ...)
{
	if (MyDebugEnabled)
	{
		if (!serverThread)
			pthread_create(&serverThread, NULL, (void *(*)(void *))startClient, this);
		
		va_list a_list;
		char buff[1024];
		va_start(a_list, format);
		vsprintf(buff, format, a_list);
		va_end(a_list);
		
		commands.push_back(string(buff));
	}
}


#pragma mark - Сохраняем различные команды

void MyDebug::setColor(int color)
{
	int r = ((color >> 16) % 256);
	int g = ((color >>  8) % 256);
	int b = ((color >>  0) % 256);
	storeCommand("setColor %i %i %i", r, g, b);
}

void MyDebug::drawLine(double x1, double y1, double x2, double y2)
{
	storeCommand("drawLine %.2lf %.2lf %.2lf %.2lf", x1, y1, x2, y2);
}

void MyDebug::fillCircle(double centerX, double centerY, double radius)
{
	storeCommand("fillCircle %.2lf %.2lf %.2lf", centerX, centerY, radius);
}
