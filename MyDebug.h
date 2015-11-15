#ifndef MyDebug_h
#define MyDebug_h


#include <iostream>
#include <fstream>
#include <vector>
#include <map>


#define DEUBG_FILENAME "MyDebug.txt"


extern bool MyDebugEnabled;


class MyDebug
{
public:
	
	MyDebug();
	~MyDebug();
	
	void setColor(int color);
	void drawLine(double x1, double y1, double x2, double y2);
	void fillCircle(double centerX, double centerY, double radius);

	void lockFrame();
	void unlockFrame();
	
	std::vector<std::string> commands;
	
private:
	
	void storeCommand(const char * format, ...);
	
};


#endif /* MyDebug_h */
