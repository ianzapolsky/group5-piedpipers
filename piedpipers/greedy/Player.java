package piedpipers.greedy;

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
  static boolean inPosition = false;
  static boolean backAtGate = false;
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

	public Point move(Point[] pipers, // all pipers
                    Point[] rats) { // all rats
		npipers = pipers.length;
	  nrats = rats.length;

    double areaSize = dimension / npipers; 
    double areaTop = id * areaSize;
    double areaBot = (id + 1) * areaSize;
    Point position = new Point(((areaTop + areaBot) / 2) + 250, 5);

    Point gate = new Point(dimension/2, dimension/2);
    Point rightMid = new Point(dimension, dimension/2);
    Point leftMid = new Point((dimension/2)-3, dimension/2);

		if (!initi) {
			this.init();
      initi = true;
		}

		Point current = pipers[id];
    boolean hasrats = hasAllRatsinVerticalRange(current, rats, areaTop, areaBot);
    System.out.println(hasrats);

    if (getSide(current) == 0 && !hasrats && !backAtGate) {
			this.music = false;
      return moveTo(current, gate);
		} 

    else if (!inPosition) {
      this.music = false;
      if (distance(current, position) < 1.0)
        inPosition = true;
      return moveTo(current, position);
    }

    else if (hasrats) {
      this.music = true;
      if (!backAtGate)
        if (distance(current, gate) < 1.0) {
          backAtGate = true;
          return moveTo(current, leftMid);
        }
        else 
          return moveTo(current, gate);
      else
        return moveTo(current, leftMid);
    }

    else {
      this.music = true;
      Point closestRat = closestRatinVerticalRange(current, rats, areaTop, areaBot);
      return moveTo(current, closestRat);
    }

	}

  public Point moveTo(Point current, Point goal) {
    double dist = distance(current, goal);
    double oy, ox;
    if (this.music) {
      ox = (goal.x - current.x) / dist * mpspeed;
      oy = (goal.y - current.y) / dist * mpspeed;
    } else {
      ox = (goal.x - current.x) / dist * pspeed;
      oy = (goal.y - current.y) / dist * pspeed;
    }
    current.x += ox;
    current.y += oy;
    return current;
  }

  // return the closest rat not under the influence of the piper
  public Point closestRat(Point current, Point[] rats) {
    Point closestRat = rats[0];  
    double leastDist = Double.MAX_VALUE;
    for (int i = 0; i < rats.length; i++) {
      double currentDist = distance(current, rats[i]);
      if (currentDist < leastDist && currentDist > 10) {
        closestRat = rats[i];
        leastDist = currentDist;
      }
    }
    return closestRat;
  }

  public Point closestRatinVerticalRange(Point current, Point[] rats,
                                         double top, double bottom) {
    Point closestRat = rats[0];  
    double leastDist = Double.MAX_VALUE;
    for (int i = 0; i < rats.length; i++) {
      if (rats[i].y >= top && rats[i].y <= bottom) {
        double currentDist = distance(current, rats[i]);
        if (currentDist < leastDist && currentDist > 10) {
          closestRat = rats[i];
          leastDist = currentDist;
        }
      }
    }
    return closestRat;
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

  // return true if the current player has all the rats under its influence
  boolean hasAllRats(Point current, Point[] rats) {
    for (int i = 0; i < rats.length; i++) {
      if (distance(current, rats[i]) > 10 && getSide(rats[i]) != 0)
        return false;
    }
    return true;
  }

  boolean hasAllRatsinVerticalRange(Point current, Point[] rats,
                                    double top, double bottom) {
    for (int i = 0; i < rats.length; i++)
      if (rats[i].y >= top && rats[i].y <= bottom)
        if (distance(current, rats[i]) > 10 && getSide(rats[i]) != 0)
          return false;
    return true;
  }

  // return true if the pipers collectively all have all the rats under their
  // influence
  boolean haveAllRats(Point[] pipers, Point[] rats) {
    for (int i = 0; i < rats.length; i++) {
      boolean isControlled = true;
      for (int j = 0; j < pipers.length; j++)
        if (distance(rats[i], pipers[j]) > 10 && getSide(rats[i]) != 0)
          isControlled = false;
      if (!isControlled)
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
