package piedpipers.group5;

import java.util.*;

import piedpipers.sim.Point;

public class Player extends piedpipers.sim.Player {
	static int npipers;
  static int nrats;
	
	static double pspeed = 0.49;
	static double mpspeed = 0.09;
	
	static Point target = new Point();
	static int[] thetas;
	static boolean finishround = true;
	static boolean initi = false;
	
	public void init() {
		thetas = new int[npipers];
		/*for (int i=0; i< npipers; i++) {
			Random random = new Random();
			int theta = random.nextInt(180);
			thetas[i]=theta;
			System.out.println(thetas[i]);
		}*/
	}

	static double distance(Point a, Point b) {
		return Math.sqrt((a.x - b.x) * (a.x - b.x) + (a.y - b.y) * (a.y - b.y));
	}

	// Return: the next position
	// my position: dogs[id-1]

	public Point move(Point[] pipers, // all pipers
                    Point[] rats) { // all rats
		npipers = pipers.length;
	  nrats = rats.length;
		System.out.println(initi);
    Point gate = new Point(dimension/2, dimension/2);
    Point rightMid = new Point(dimension, dimension/2);
    Point leftMid = new Point((dimension/2)-3, dimension/2);
		if (!initi) {
			this.init();
      initi = true;
		}
		Point current = pipers[id];
    boolean hasrats = hasAllRats(current, rats);
		double ox = 0, oy = 0;
    if (getSide(current) == 0 && !hasrats) {
			finishround = true;
			this.music = false;
			double dist = distance(current, gate);
			assert dist > 0;
			ox = (gate.x - current.x) / dist * pspeed;
			oy = (gate.y - current.y) / dist * pspeed;
			Random random = new Random();
			int theta = random.nextInt(180);
			thetas[id]=theta;
			System.out.println("move toward the right side");
		} 
    else if (hasrats) {
      this.music = true;
      double dist = distance(current, leftMid);
      ox = (leftMid.x - current.x) / dist * mpspeed;
      oy = (leftMid.y - current.y) / dist * mpspeed;
    }
		else if (!closetoWall(current) && finishround) {
      this.music = true;
      double dist = distance(current, rightMid);
      ox = (rightMid.x - current.x) / dist * mpspeed;
      oy = (rightMid.y - current.y) / dist * mpspeed;
		}
		else {
			finishround = false;
			this.music = true;
			double dist = distance(current, gate);
			assert dist > 0;
			ox = (gate.x - current.x) / dist * mpspeed;
			oy = (gate.y - current.y) / dist * mpspeed;
			System.out.println("move toward the left side");
		}
		
		current.x += ox;
		current.y += oy;
		return current;
	}
	boolean closetoWall (Point current) {
		boolean wall = false;
		if (Math.abs(current.x-dimension)<pspeed) {
			wall = true;
		}
		if (Math.abs(current.y-dimension)<pspeed) {
			wall = true;
		}
		if (Math.abs(current.y)<pspeed) {
			wall = true;
		}
		return wall;
	}
  boolean hasAllRats(Point current, Point[] rats) {
    for (int i = 0; i < rats.length; i++) {
      if (distance(current, rats[i]) > 10 && getSide(rats[i]) != 0)
        return false;
    }
    return true;
  }
	int getSide(double x, double y) {
		if (x < dimension * 0.5)
			return 0;
		else if (x > dimension * 0.5)
			return 1;
		else
			return 2;
	}

	int getSide(Point p) {
		return getSide(p.x, p.y);
	}

}
