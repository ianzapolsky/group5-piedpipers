package piedpipers.twins;

import java.util.*;

import piedpipers.sim.Point;

public class Player extends piedpipers.sim.Player {

  // REQUIRED STUFF FOR SIM
  int npipers;
  int nrats;
  double pspeed = 0.49;
  double mpspeed = 0.09;
  static boolean initi = false;
  static int[] thetas;

  // enum for different phase
  enum Phase {
	INITIAL_PHASE,
	DENSE_PHASE,
	MIDDLE_PHASE,
	SPARSE_PHASE
  }

  Phase currentLocationPhase;
  
  // 0 = moving to inital position
  // 1 = vertical sweep
  // 2 = handoff & greedy cleanup
  // 3 = #rats < #pipers
  int phase = 0;
  
// wanted rats array for optimization of the greedy piper behavior
  int[] wantedRats;
  Point[] pipers;
  Point[] rats;
  boolean[] piperMusic;
  Point[] previousRats;
  
  int magnetId;

  boolean backAtGate = false; 	// piper returned to the gate
  int trackingCounter = 0;


  Point currentLocation;
  
  public void init() {
	thetas = new int[npipers];
    magnetId = npipers / 2;
    phase = 0;
	currentLocationPhase = Phase.INITIAL_PHASE;
    wantedRats = new int[npipers];
    previousRats = new Point[nrats];
	for(int i = 0; i < nrats; i++){
		previousRats[i] = new Point();
	}
  }

  public Point move(Point[] pipers, Point[] rats, boolean[] piperMusic) { // all rats
    currentLocation = pipers[id];
    this.pipers = pipers;
	this.rats = rats;
	this.piperMusic = piperMusic;
	npipers = pipers.length;
	nrats = rats.length;

    // utility points
    Point gate = new Point(dimension/2, dimension/2);
    Point rightMid = new Point(dimension, dimension/2);
    Point leftMid = new Point(0, dimension/2);
	Point currentLocation = pipers[id];

    // vertical strip
    double areaSize = (dimension / 2) / npipers; 
    double areaLeft = (id * (areaSize)) + (dimension / 2);
    double areaRight = ((id + 1) * (areaSize)) + (dimension / 2);
    Point initialPosition = new Point((areaLeft + areaRight)/ 2, 5);

		if (!initi) {
			this.init();
      initi = true;
		}

    //System.out.println("Player " + id + " is in phase " + phase);
  
    boolean allRatsCaptured = haveAllRats();

    if (allRatsCaptured) {
      if (backAtGate) {
        return moveTo(leftMid);
      } else {
        if (distance(currentLocation, gate) < 1) {
          backAtGate = true;
        }
        return moveTo(gate);
      }
    }

    Point nextMove;
    switch(phase) {

        case 0: nextMove = moveToInitialPosition(currentLocation, gate, initialPosition, pipers);
		  break;
        case 1: nextMove = sweepVertically(currentLocation, rats, pipers, areaLeft, areaRight);
  		  break;
        case 2: nextMove = handoffPhase(currentLocation, rats, pipers);
  	 	  break;
        case 3: nextMove = chasingPhaseOne();
		  break;
        case 4: nextMove = chasingPhaseTwo(gate);
          break;
        default: nextMove = moveTo(gate);
    }

	// sample rats' location differences to. used to tile direction
	if(++trackingCounter == 15){  // take sample every 1.5 second

	    // save current rats as previous rat movement
	    for(int i = 0; i < nrats; i++){
		  previousRats[i].x = rats[i].x;
		  previousRats[i].y = rats[i].y;
	 	}
	    trackingCounter = 0;
	}
	
    return nextMove;
  }


// ====================
// phase logic
// ====================

  public Point moveToInitialPosition(Point currentLocation, Point gate, Point position, Point[] pipers) {

    if (distance(currentLocation, position) < 5)
      phase = 1;

    /*
    boolean allSet = true; 
    for (int i = 0; i < pipers.length; i++)
      if (distance(currentLocation, position) > 5) {
        allSet = false;
        break;
      }
    if (allSet)
      phase = 1; 
    */

    // if we're still on the left side, go to the gate
    if (getSide(currentLocation) == 0) 
      return moveTo(gate);
    else
      return moveTo(position);
  }

  public Point sweepVertically(Point currentLocation, Point[] rats, Point[] pipers, double areaLeft, double areaRight) {
    if (currentLocation.y > dimension - 20)
      phase = 2;
    this.music = true;
    Point closestRat = closestRatBelowinHorizontalRange(areaLeft, areaRight);
    return moveTo(closestRat);
  }

  public Point handoffPhase(Point currentLocation, Point[] rats, Point[] pipers) {

    boolean allSet = true;
    for (int i = 0; i < pipers.length; i++)
      if (distance(pipers[i], pipers[magnetId]) > .5) {
        allSet = false;
        break;
      }

    if (allSet) {
      phase = 3;
      return currentLocation;
    }

    if (id == magnetId) { // dropbox piper
      this.music = true;
      return currentLocation;
    } else { // all other pipers
      this.music = true;
      Point dropbox = pipers[magnetId];
      return moveTo(dropbox);
    }
  }

  public Point chasingPhaseOne() {
    if(ratsLeft() < npipers-1){
        phase = 4;
        return currentLocation;
    }
    if (id == magnetId) { // dropbox piper

	  		Point midPoint = new Point(dimension * (3.0 / 4.0), dimension/2);
	  		Point nearestRat = closestRat();
	  		this.music = true;
	  		
	  		if(Math.abs(nearestRat.x - midPoint.x) < 40 && Math.abs(nearestRat.y - midPoint.y) < 40)
	  			return moveTo(nearestRat);
	  		else
	  			return moveTo(midPoint);

    } else { // all other pipers
        
        double quadrantSize = dimension/npipers;
        double top = id*quadrantSize;
        double bottom = (id+1)*quadrantSize;
        return moveTo(closestRatinVerticalRange(top, bottom));
		//int closestRat = closestRatIndex();
		
        //return chaseRat(closestRat);

    }
  }

  public Point chasingPhaseTwo(Point gate) {
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
          return moveTo(gate);
        }
    }
    return moveTo(destination);

  }



// ====================
// utility methods
// ====================
  double distance(Point a, Point b) {
	return Math.sqrt((a.x - b.x) * (a.x - b.x) + (a.y - b.y) * (a.y - b.y));
  }

// tilt direction to avoid endless chasing
// using the predicted location instead of actual location
// tested: THIS IS AWESOME
  Point chaseRat(int ratIndex){
	Point adjustedLocation = new Point();
	adjustedLocation.x = rats[ratIndex].x + distance(currentLocation, rats[ratIndex]) * (rats[ratIndex].x - previousRats[ratIndex].x);
	adjustedLocation.y = rats[ratIndex].y + distance(currentLocation, rats[ratIndex]) * (rats[ratIndex].y - previousRats[ratIndex].y);
	return moveTo(adjustedLocation);
  }


  // returns the valid move 
  Point moveTo(Point goal) {
    double dist = distance(currentLocation, goal);
    double oy, ox;
    if (this.music) {
      ox = (goal.x - currentLocation.x) / dist * mpspeed;
      oy = (goal.y - currentLocation.y) / dist * mpspeed;
    } else {
      ox = (goal.x - currentLocation.x) / dist * pspeed;
      oy = (goal.y - currentLocation.y) / dist * pspeed;
    }
    currentLocation.x += ox;
    currentLocation.y += oy;
    return currentLocation;
  }

  int ratsLeft() {
    int ratsLeft = nrats;
    for (int i = 0; i < nrats; i++) {
      if (getSide(rats[i]) == 0) {
        ratsLeft -= 1;    
        continue;
      } else{
		for (int j = 0; j < npipers; j++)
          if (distance(rats[i], pipers[j]) < 10) {
            ratsLeft -= 1;
            break;
        }
      }
        
    }
    return ratsLeft;
  }

  // return the closest rat not under the influence of the piper
  int closestRatIndex() {
    int closestRatIndex = -1;
    double leastDist = Double.MAX_VALUE;
    for (int i = 0; i < rats.length; i++) {
      double currentLocationDist = distance(currentLocation, rats[i]);
      if (currentLocationDist < leastDist && currentLocationDist > 10) {
		    // Edward here: adding checking for other pipers' influence
		    if (pipers != null) {
		    	boolean flag = false;
		    	for (int j = 0; j < pipers.length; j++) {
		    		if (distance(pipers[j], rats[i]) < 10.0  && piperMusic[j]) {
		    			flag = true;
		    			break;
		    		}
		    	}
		    	if (flag) {
		    		continue;
		    	} else {
		    		closestRatIndex = i;
		        leastDist = currentLocationDist;
		    	}
		    }
		    // Edward out
      }
    }
    return closestRatIndex;
  }

  // wrapper functionreturn: return the closest rat(Point) not under the influence of the piper
  Point closestRat() {
    return rats[closestRatIndex()];
  }

  int closestRatinVerticalRangeIndex(double top, double bottom){
	int closestRat = 0;  
    double leastDist = Double.MAX_VALUE;
    for (int i = 0; i < nrats; i++) {
      if (rats[i].y >= top && rats[i].y <= bottom && getSide(rats[i]) != 0) {
        double currentLocationDist = distance(currentLocation, rats[i]);
        if (currentLocationDist < leastDist && currentLocationDist > 10) {
          closestRat = i;
          leastDist = currentLocationDist;
        }
      }
    }
    return closestRat;
  }
  
  Point closestRatinVerticalRange(double top, double bottom) {
    return rats[closestRatinVerticalRangeIndex(top, bottom)];
  }


  int closestRatBelowinHorizontalRangeIndex(double left, double right){
	int closestRat = 0;
    double leastDist = Double.MAX_VALUE;
    for (int i = 0; i < rats.length; i++) {
      if (rats[i].x >= left && rats[i].x <= right && rats[i].y > currentLocation.y) {
        double currentLocationDist = distance(currentLocation, rats[i]);
        if (currentLocationDist < leastDist && currentLocationDist > 10) {
          closestRat = i;
          leastDist = currentLocationDist;
        }
      }
    }
    return closestRat;
  }
  Point closestRatBelowinHorizontalRange(double left, double right) {
    return rats[closestRatBelowinHorizontalRangeIndex(left, right)];
  }

  boolean closetoWall() {
		boolean wall = false;
		if (Math.abs(currentLocation.x-dimension)<pspeed) {
			wall = true;
		}
		if (Math.abs(currentLocation.y-dimension)<pspeed) {
			wall = true;
		}
		if (Math.abs(currentLocation.y)<pspeed) {
			wall = true;
		}
		return wall;
  }


  // return true if the pipers collectively all have all the rats under their
  // influence
  boolean haveAllRats() {
    for (int i = 0; i < nrats; i++) {
	  if(getSide(rats[i]) == 0){  // only care about rats on the right fence
		continue;
	  }
      boolean isControlled = false;
      for (int j = 0; j < npipers; j++){
		if (distance(rats[i], pipers[j]) < 10  && piperMusic[j]){ // case: some piper got the rat 
		  isControlled = true;	
		  break;
		}
	  }
      if(!isControlled)
		return false;
    }
    return true;
  }

  // return location's side
  // 0 = left, 1 = right, 2 = invalid
  int getSide(Point p) {
	if (p.x < dimension * 0.5)
	  return 0;
	else if (p.x > dimension * 0.5)
	  return 1;
	else
	  return 2;
  }

}
