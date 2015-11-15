import java.awt.*;
import java.io.*;
import java.util.Scanner;
import java.net.*;

import model.*;

import static java.lang.StrictMath.*;
import static java.lang.System.out;


public final class LocalTestRendererListener {
	private Graphics graphics;
	private World world;
	private Game game;
	
	private int canvasWidth;
	private int canvasHeight;
	
	private double left;
	private double top;
	private double width;
	private double height;
	
	private Socket clientSocket;
	PrintWriter outToServer;
	BufferedReader inFromServer;
	
	public void initClient()
	{
		if (clientSocket != null)
			return;
		
		try
		{
			clientSocket = new Socket("127.0.0.1", 1723);
			outToServer = new PrintWriter(clientSocket.getOutputStream(), true);
			inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
		}
		catch (Exception e)
		{
			try
			{
				Thread.sleep(100);
			}
			catch (Exception ex)
			{
			}
		}
	}
	
	public void executeMyDebugCommands()
	{
		if (clientSocket == null)
			return;
		
		String line;
		outToServer.println("frame " + world.getTick());
		outToServer.flush();
		
		line = readLine();
		int num = Integer.parseInt(line);
		//out.println("Start frame with " + num + " commands.");
		for (int i = 0; i < num; i++)
		{
			
			line = readLine();
			if (line == ".") {
				out.println("Line is dot!");
				break;
			}
			processMyDebugLine(line);
		}
		//out.println("End frame!");
	}
	
	public String readLine()
	{
		String line;
		try
		{
			line = inFromServer.readLine();
		}
		catch (Exception e)
		{
			line = ".";
			out.println("Exception!");
		}
		return line;
	}
	
	public void beforeDrawScene(Graphics graphics, World world, Game game, int canvasWidth, int canvasHeight,
								double left, double top, double width, double height)
	{
		initClient();
		
		updateFields(graphics, world, game, canvasWidth, canvasHeight, left, top, width, height);
		
		//out.println("Before... " + world.getTick());
		
		double trackTileSize = game.getTrackTileSize();
		long myId = -1;
		int wpId = 1;
		double nOffset = 60.0D;

		for (Player player : world.getPlayers()) {
			if (player.getName().equals("MyStrategy")) {
				myId = player.getId();
			}
		}

		for (int[] waypoint : world.getWaypoints()) {
			double x = waypoint[0] * trackTileSize + 100.0D;
			double y = waypoint[1] * trackTileSize + 100.0D;

			graphics.setColor(new Color(252, 255, 127));
			fillRect(x, y, trackTileSize - 200.0D, trackTileSize - 200.0D);
		}

		for (Car car : world.getCars()) {
			if (car.getPlayerId() == myId) {
				double x = car.getNextWaypointX() * trackTileSize + 100.0D;
				double y = car.getNextWaypointY() * trackTileSize + 100.0D;

				graphics.setColor(new Color(75, 255, 63));
				fillRect(x, y, trackTileSize - 200.0D, trackTileSize - 200.0D);
			}
		}

		for (int[] waypoint : world.getWaypoints()) {
			double x = waypoint[0] * trackTileSize + 320.0D;
			double y = waypoint[1] * trackTileSize + 490.0D;
			if (wpId >= 10) {
				x = x - nOffset;
			}

			graphics.setColor(Color.BLACK);
			drawString(wpId++ + "", x, y);
		}
			
		executeMyDebugCommands();
		
		for (Car car : world.getCars()) {
			drawCircle(car.getX(), car.getY(), hypot(car.getWidth(), car.getHeight()) / 2.0D);
		}
		
		graphics.setColor(Color.BLACK);
	}
	
	public void afterDrawScene(Graphics graphics, World world, Game game, int canvasWidth, int canvasHeight,
							   double left, double top, double width, double height)
	{
		updateFields(graphics, world, game, canvasWidth, canvasHeight, left, top, width, height);
		
		//out.println("After...");
		
		graphics.setColor(Color.BLACK);
	}
	
	private void updateFields(Graphics graphics, World world, Game game, int canvasWidth, int canvasHeight,
							  double left, double top, double width, double height) {
		this.graphics = graphics;
		this.world = world;
		this.game = game;
		
		this.canvasWidth = canvasWidth;
		this.canvasHeight = canvasHeight;
		
		this.left = left;
		this.top = top;
		this.width = width;
		this.height = height;
	}
	
	private void processMyDebugLine(String line)
	{
		String[] p = line.split(" ");
		switch (p[0])
		{
			case "setColor":
				int r = Integer.parseInt(p[1]);
				int g = Integer.parseInt(p[2]);
				int b = Integer.parseInt(p[3]);
				graphics.setColor(new Color(r, g, b));
				//out.println("Cmd: " + line);
				break;
			case "drawLine":
				double x1 = Double.parseDouble(p[1]);
				double y1 = Double.parseDouble(p[2]);
				double x2 = Double.parseDouble(p[3]);
				double y2 = Double.parseDouble(p[4]);
				drawLine(x1, y1, x2, y2);
				//out.println("Cmd: " + line);
				break;
			case "fillCircle":
				double centerX = Double.parseDouble(p[1]);
				double centerY = Double.parseDouble(p[2]);
				double radius  = Double.parseDouble(p[3]);
				fillCircle(centerX, centerY, radius);
				//out.println("Cmd: " + line);
				break;
		}
	}

	private void drawString(String text, double x1, double y1) {
		Point2I stringBegin = toCanvasPosition(x1, y1);

		graphics.setFont(new Font("Serif", Font.PLAIN, 42));
		graphics.drawString(text, stringBegin.getX(), stringBegin.getY());
	}

	private void drawLine(double x1, double y1, double x2, double y2) {
		Point2I lineBegin = toCanvasPosition(x1, y1);
		Point2I lineEnd = toCanvasPosition(x2, y2);
		
		graphics.drawLine(lineBegin.getX(), lineBegin.getY(), lineEnd.getX(), lineEnd.getY());
	}
	
	private void fillCircle(double centerX, double centerY, double radius) {
		Point2I topLeft = toCanvasPosition(centerX - radius, centerY - radius);
		Point2I size = toCanvasOffset(2.0D * radius, 2.0D * radius);
		
		graphics.fillOval(topLeft.getX(), topLeft.getY(), size.getX(), size.getY());
	}
	
	private void drawCircle(double centerX, double centerY, double radius) {
		Point2I topLeft = toCanvasPosition(centerX - radius, centerY - radius);
		Point2I size = toCanvasOffset(2.0D * radius, 2.0D * radius);
		
		graphics.drawOval(topLeft.getX(), topLeft.getY(), size.getX(), size.getY());
	}
	
	private void fillArc(double centerX, double centerY, double radius, int startAngle, int arcAngle) {
		Point2I topLeft = toCanvasPosition(centerX - radius, centerY - radius);
		Point2I size = toCanvasOffset(2.0D * radius, 2.0D * radius);
		
		graphics.fillArc(topLeft.getX(), topLeft.getY(), size.getX(), size.getY(), startAngle, arcAngle);
	}
	
	private void drawArc(double centerX, double centerY, double radius, int startAngle, int arcAngle) {
		Point2I topLeft = toCanvasPosition(centerX - radius, centerY - radius);
		Point2I size = toCanvasOffset(2.0D * radius, 2.0D * radius);
		
		graphics.drawArc(topLeft.getX(), topLeft.getY(), size.getX(), size.getY(), startAngle, arcAngle);
	}
	
	private void fillRect(double left, double top, double width, double height) {
		Point2I topLeft = toCanvasPosition(left, top);
		Point2I size = toCanvasOffset(width, height);
		
		graphics.fillRect(topLeft.getX(), topLeft.getY(), size.getX(), size.getY());
	}
	
	private void drawRect(double left, double top, double width, double height) {
		Point2I topLeft = toCanvasPosition(left, top);
		Point2I size = toCanvasOffset(width, height);
		
		graphics.drawRect(topLeft.getX(), topLeft.getY(), size.getX(), size.getY());
	}
	
	private void drawPolygon(Point2D... points) {
		int pointCount = points.length;
		
		for (int pointIndex = 1; pointIndex < pointCount; ++pointIndex) {
			Point2D pointA = points[pointIndex];
			Point2D pointB = points[pointIndex - 1];
			drawLine(pointA.getX(), pointA.getY(), pointB.getX(), pointB.getY());
		}
		
		Point2D pointA = points[0];
		Point2D pointB = points[pointCount - 1];
		drawLine(pointA.getX(), pointA.getY(), pointB.getX(), pointB.getY());
	}
	
	private Point2I toCanvasOffset(double x, double y) {
		return new Point2I(x * canvasWidth / width, y * canvasHeight / height);
	}
	
	private Point2I toCanvasPosition(double x, double y) {
		return new Point2I((x - left) * canvasWidth / width, (y - top) * canvasHeight / height);
	}
	
	private static final class Point2I {
		private int x;
		private int y;
		
		private Point2I(double x, double y) {
			this.x = toInt(round(x));
			this.y = toInt(round(y));
		}
		
		private Point2I(int x, int y) {
			this.x = x;
			this.y = y;
		}
		
		private Point2I() {
		}
		
		public int getX() {
			return x;
		}
		
		public void setX(int x) {
			this.x = x;
		}
		
		public int getY() {
			return y;
		}
		
		public void setY(int y) {
			this.y = y;
		}
		
		private static int toInt(double value) {
			@SuppressWarnings("NumericCastThatLosesPrecision") int intValue = (int) value;
			if (abs((double) intValue - value) < 1.0D) {
				return intValue;
			}
			throw new IllegalArgumentException("Can't convert double " + value + " to int.");
		}
	}
	
	private static final class Point2D {
		private double x;
		private double y;
		
		private Point2D(double x, double y) {
			this.x = x;
			this.y = y;
		}
		
		private Point2D() {
		}
		
		public double getX() {
			return x;
		}
		
		public void setX(double x) {
			this.x = x;
		}
		
		public double getY() {
			return y;
		}
		
		public void setY(double y) {
			this.y = y;
		}
	}
}
