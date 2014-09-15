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

  boolean inPosition = false; // piper reached initial position
  boolean atBottom = false; // piper reached bottom of board in vertical sweep
  boolean backAtGate = false; // piper returned to the gate
  boolean gotWantedRat = false; // piper returning the assigned rat



  
	public void init() {
		thetas = new int[npipers];
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
    Point leftMid = new Point((dimension/2)-3, dimension/2);
		Point current = pipers[id];

    boolean hasrats = hasAllRatsinHorizontalRange(current, rats, areaLeft, areaRight);

    // vertical strip
    double areaSize = (dimension / 2) / npipers; 
    double areaLeft = (id * (areaSize)) + (dimension / 2);
    double areaRight = ((id + 1) * (areaSize)) + (dimension / 2);
    Point initialPosition = new Point((areaLeft + areaRight)/ 2, 5);

		if (!initi) {
			this.init();
      initi = true;
		}

    switch(phase) {

        case 0: return moveToInitialPosition(current, initialPosition, pipers);

        case 1: return sweepVertically(current, rats, pipers, areaLeft, areaRight);

        case 2: return chasingPhaseOne(current, rats, pipers);

        case 3: return chasingPhaseTwo(current, rats, pipers, gate, id);
    
        default: return moveTo(current, gate);
    }

  }

  static Point moveToInitialPosition(Point current, Point position, Point[] pipers) {
    boolean allSet = true; 
    for (i = 0; i < pipers.length; i++)
      if (distance(current, position) > 5) {
        allSet = false;
        break;
      }
    if (allSet)
      phase = 1; 
    return moveTo(current, position);
  }

  static Point sweepVertically(Point current, Point[] rats, Point[] pipers, double areaLeft, double areaRight) {
    boolean allSet = true;
    for (i = 0; i < pipers.length; i++)
      if (current.y > dimension - 10) {
        allSet = false;
        break;
      }
    if (allSet) 
      phase = 2;
    Point closestRat = closestRatBelowinHorizontalRange(current, rats, areaLeft, areaRight);
    return moveTo(current, closestRat);
  }

  static Point chasingPhaseOne(Point current, Point[] rats, Point[] pipers) {
	switch(id){
		case 0: // dropbox piper

			Point midPoint = new Point(dimension * (3.0 / 4.0), dimension/2);
			Point nearestRat = closestRat(current, rats);
			this.music = true;
			
			// logic for switching phase
			int remainingRatsCounter = rats.length;
			for(int i = 0; i < rats.length; i++){
				if(distance(current, rats[i]) < 10){
					remainingRatsCounter--;
				}
			}
			if(remainingRatsCounter < pipers.length){
				phase = 3;
			}
			
			if(Math.abs(nearestRat.x - midPoint.x) < 40 && Math.abs(nearestRat.y - midPoint.y) < 40){
				return moveTo(current, nearestRat);
			}else{
				return moveTo(current, midPoint);
			}
		break;
		
		case 1: // team leader piper assign jobs
			for(int i = 0; i < wantedRats.length; i++){
				wantedRats[i] = closestRatIndex(pipers[i+1], rats);				
			}
			// intentially no break so leader piper could also pick up rat
		
		default: // the rest pipers are not thinkers :|
			if(!gotWantedRat){
				if(distance(current, rats[wantedRats[id-1]]) < 10){
					gotWantedRat = true;
					this.music = true;
				}else{
					this.music = false;
					return moveTo(current, rats[wantedRats[id-1]]);
				}
			}else{ // moving back to dropbox
				// handover logic
				if(distance(current, pipers[0]) < 0.5){
					music = false;
					gotWantedRat = false;
				}
				
				return moveTo(current,pipers[0]);
			}
	}
    
	return moveTo(current, current);
  
  }

  static Point chasingPhaseTwo(Point current, Point[] rats, Point[] pipers, Point gate, int id) {
      List<Point> remaining_rats = new ArrayList<Point>();
      for (Point p : rats) {
          if (p.x > gate.x) {
              // we're in the right side
              remaining_rats.add(p);
          }
      }

      Point destination;
      if (id == 0) {
          destination = new Point(gate.x - 20, gate.y);
      } else {
          destination = remaining_rats.get(id % remaining_rats.size());
      }

      return moveTo(current, destination);
  }

        
/**

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
    else if (!atBottom) {
      this.music = true;
      Point closestRat = closestRatBelowinHorizontalRange(current, rats, areaLeft, areaRight);
      if (current.y > dimension - 20)
          atBottom = true;
      return moveTo(current, closestRat);
    }
    else {
      this.music = true;
      Point closestRat = closestRatBelowinHorizontalRange(current, rats, areaLeft, areaRight);
      if (current.y < 20)
        atBottom = false;
      return moveTo(current, closestRat);
    }
	}

*/

	static double distance(Point a, Point b) {
		return Math.sqrt((a.x - b.x) * (a.x - b.x) + (a.y - b.y) * (a.y - b.y));
	}

  // returns the valid move 
  static Point moveTo(Point current, Point goal) {
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
  static Point closestRatIndex(Point current, Point[] rats) {
    Point closestRatIdx = -1;  
    double leastDist = Double.MAX_VALUE;
    for (int i = 0; i < rats.length; i++) {
      double currentDist = distance(current, rats[i]);
      if (currentDist < leastDist && currentDist > 10) {
		// Edward here: adding checking for other pipers' influence
		if(thePipers != null){
			boolean flag = false;
			for(int j = 0; j < thePipers.length; j++){
				if(distance(thePiper[j],rats[i]) < 10.0){
					flag = true;
					break;
				}
			}
			if(flag){
				continue;
			}else{
				closestRat = i;
		        leastDist = currentDist;
			}
			
		}
		// Edward out
 
      }
    }
    return closestRatIdx;
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
				if(distance(thePiper[j],rats[i]) < 10.0){
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

  static Point closestRatBelowinHorizontalRange(Point current, Point[] rats,
                                         double left, double right) {
    Point closestRat = rats[0];  
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
