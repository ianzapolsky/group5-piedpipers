package piedpipers.smartest_yet;

import java.util.*;

import piedpipers.sim.Point;

public class Player extends piedpipers.sim.Player {

  // REQUIRED STUFF FOR SIM
	static int npipers;
  static int nrats;
	static double pspeed = 0.49;
	static double mpspeed = 0.09;
	static boolean initi = false;
	static int[] thetas;

  // Shared across all pipers
  // 0 = moving to inital position
  // 1 = vertical sweep
  // 2 = handoff & greedy cleanup
  // 3 = #rats < #pipers
  static int phase = 0;
  // wanted rats array for optimization of the greedy piper behavior
  static int[] wantedRats = new int[7];
  static Point[] thePipers;
  static Point[] theRats;

  int magnet;
  boolean inPosition = false; // piper reached initial position
  boolean atBottom = false; // piper reached bottom of board in vertical sweep
  boolean backAtGate = false; // piper returned to the gate
  boolean gotWantedRat = false; // piper returning the assigned rat

	public void init() {
		thetas = new int[npipers];
    magnet = npipers / 2;
    phase = 0;
	}

	public Point move(Point[] pipers, // all pipers
                    Point[] rats) { // all rats
	  thePipers = pipers;
	  theRats = rats;
		npipers = pipers.length;
	  nrats = rats.length;

    // utility points
    Point gate = new Point(dimension/2, dimension/2);
    Point rightMid = new Point(dimension, dimension/2);
    Point leftMid = new Point(0, dimension/2);
		Point current = pipers[id];

    // vertical strip
    double areaSize = (dimension / 2) / npipers; 
    double areaLeft = (id * (areaSize)) + (dimension / 2);
    double areaRight = ((id + 1) * (areaSize)) + (dimension / 2);
    Point initialPosition = new Point((areaLeft + areaRight)/ 2, 5);

		if (!initi) {
			this.init();
      initi = true;
		}

    System.out.println("Player " + id + " is in phase " + phase);
  
    boolean allRatsCaptured = haveAllRats(pipers, rats);

    if (allRatsCaptured) {
      if (backAtGate) {
        return moveTo(current, leftMid);
      } else {
        if (distance(current, gate) < 3) {
          backAtGate = true;
        }
        return moveTo(current, gate);
      }
    }

    switch(phase) {

        case 0: return moveToInitialPosition(current, gate, initialPosition, pipers);

        case 1: return sweepVertically(current, rats, pipers, areaLeft, areaRight);
  
        case 2: return handoffPhase(current, rats, pipers);

        case 3: return chasingPhaseOne(current, rats, pipers);

        case 4: return chasingPhaseTwo(current, rats, pipers, gate);
    
        default: return moveTo(current, gate);
    }

  }

  public Point moveToInitialPosition(Point current, Point gate, Point position, Point[] pipers) {

    if (distance(current, position) < 5)
      phase = 1;

    /*
    boolean allSet = true; 
    for (int i = 0; i < pipers.length; i++)
      if (distance(current, position) > 5) {
        allSet = false;
        break;
      }
    if (allSet)
      phase = 1; 
    */

    // if we're still on the left side, go to the gate
    if (getSide(current) == 0) 
      return moveTo(current, gate);
    else
      return moveTo(current, position);
  }

  public Point sweepVertically(Point current, Point[] rats, Point[] pipers, double areaLeft, double areaRight) {
    if (current.y > dimension - 20)
      phase = 2;
    this.music = true;
    Point closestRat = closestRatBelowinHorizontalRange(current, rats, areaLeft, areaRight);
    return moveTo(current, closestRat);
  }

  public Point handoffPhase(Point current, Point[] rats, Point[] pipers) {

    boolean allSet = true;
    for (int i = 0; i < pipers.length; i++)
      if (distance(pipers[i], pipers[magnet]) > .5) {
        allSet = false;
        break;
      }

    if (allSet) {
      phase = 3;
      return current;
    }

    if (id == magnet) { // dropbox piper
      this.music = true;
      return current;
    } else { // all other pipers
      this.music = true;
      Point dropbox = pipers[magnet];
      return moveTo(current, dropbox);
    }
  }

  public Point chasingPhaseOne(Point current, Point[] rats, Point[] pipers) {
    if(ratsLeft(rats, pipers) < npipers-1){
        phase = 4;
        return current;
    }
    if (id == magnet) { // dropbox piper

	  		Point midPoint = new Point(dimension * (3.0 / 4.0), dimension/2);
	  		Point nearestRat = closestRat(current, rats);
	  		this.music = true;
	  		
	  		if(Math.abs(nearestRat.x - midPoint.x) < 40 && Math.abs(nearestRat.y - midPoint.y) < 40)
	  			return moveTo(current, nearestRat);
	  		else
	  			return moveTo(current, midPoint);

    } else { // all other pipers
        
        double quadrantSize = dimension/npipers;
        double top = id*quadrantSize;
        double bottom = (id+1)*quadrantSize;
        Point closestRat = closestRatinVerticalRange(current, rats, top, bottom);
        return moveTo(current, closestRat);

    }
  }

  public Point chasingPhaseTwo(Point current, Point[] rats, Point[] pipers, Point gate) {
    List<Point> remaining_rats = new ArrayList<Point>();
    for (Point p : rats) {
        boolean flag = true;
        for(int i = 0; i < npipers; i++){
          if(distance(pipers[i], p) < 10){
            flag = false;
            break;
          }
        }
        if(flag && getSide(p) == 1){
          remaining_rats.add(p);
        }
    }

    Point destination;
    if (id == 0) {
        destination = gate;
    } else {
        if(id < remaining_rats.size() + 1){
          destination = remaining_rats.get(id - 1); 
        }else{
          return moveTo(current, gate);
        }
    }
    return moveTo(current, destination);

  }

	static double distance(Point a, Point b) {
		return Math.sqrt((a.x - b.x) * (a.x - b.x) + (a.y - b.y) * (a.y - b.y));
	}

  // returns the valid move 
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

  public int getPhase() {
    return phase;
  }

  public int ratsLeft(Point[] rats, Point[] pipers) {
    
    int ratsLeft = rats.length;
    for (int i = 0; i < nrats; i++) {
      if (getSide(rats[i]) == 0) {
        ratsLeft -= 1;    
        continue;
      } else
        for (int j = 0; j < npipers; j++)
          if (distance(rats[i], pipers[j]) < 10) {
            ratsLeft -= 1;
            break;
          }
    }
    return ratsLeft;
  }

  // return the closest rat not under the influence of the piper
  public int closestRatIndex(Point current, Point[] rats) {
    int closestRatIndex = -1;
    double leastDist = Double.MAX_VALUE;
    for (int i = 0; i < rats.length; i++) {
      double currentDist = distance(current, rats[i]);
      if (currentDist < leastDist && currentDist > 10) {
		    // Edward here: adding checking for other pipers' influence
		    if (thePipers != null) {
		    	boolean flag = false;
		    	for (int j = 0; j < thePipers.length; j++) {
		    		if (distance(thePipers[j], rats[i]) < 10.0) {
		    			flag = true;
		    			break;
		    		}
		    	}
		    	if (flag) {
		    		continue;
		    	} else {
		    		closestRatIndex = i;
		        leastDist = currentDist;
		    	}
		    }
		    // Edward out
      }
    }
    return closestRatIndex;
  }

  // return the closest rat not under the influence of the piper
  static Point closestRat(Point current, Point[] rats) {
    Point closestRat = rats[0];  
    double leastDist = Double.MAX_VALUE;
    for (int i = 0; i < rats.length; i++) {
      double currentDist = distance(current, rats[i]);
      if (currentDist < leastDist && currentDist > 10) {
		    // Edward here: adding checking for other pipers' influence
		    if(thePipers != null){
		    	boolean flag = false;
		    	for(int j = 0; j < thePipers.length; j++){
		    		if(distance(thePipers[j], rats[i]) < 10.0){
		    			flag = true;
		    			break;
		    		}
		    	}
		    	if(flag){
		    		continue;
		    	}else{
		    		closestRat = rats[i];
		        leastDist = currentDist;
		    	}
		    }
        // Edward out
      }
    }
    return closestRat;
  }

  public Point closestRatinVerticalRange(Point current, Point[] rats,
                                         double top, double bottom) {
    Point closestRat = rats[0];  
    double leastDist = Double.MAX_VALUE;
    for (int i = 0; i < rats.length; i++) {
      if (rats[i].y >= top && rats[i].y <= bottom && getSide(rats[i]) != 0) {
        double currentDist = distance(current, rats[i]);
        if (currentDist < leastDist && currentDist > 10) {
          closestRat = rats[i];
          leastDist = currentDist;
        }
      }
    }
    return closestRat;
  }

  public Point closestRatBelowinHorizontalRange(Point current, Point[] rats,
                                         double left, double right) {
    Point closestRat = new Point(current.x, dimension);
    double leastDist = Double.MAX_VALUE;
    for (int i = 0; i < rats.length; i++) {
      if (rats[i].x >= left && rats[i].x <= right && rats[i].y > current.y) {
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

  boolean hasAllRatsinHorizontalRange(Point current, Point[] rats,
                                    double left, double right) {
    for (int i = 0; i < rats.length; i++)
      if (rats[i].x >= left && rats[i].x <= right)
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
