package piedpipers.master_slave;

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

  // wanted rats array for optimization of the greedy piper behavior
  int[] wantedRats;
  Point[] pipers;
  Point[] rats;
  boolean[] piperMusic;
  Point[] previousRats;
  double targetDelta;
  int ignoreRatIndex = -1;

  // (not used here)
  int phase = 0;
  
  int magnetId;

  boolean hunting = false;
  boolean backAtGate = false; 	// piper returned to the gate
  int trackingCounter = 0;

  Point currentLocation;
  
  public void init() {
	  thetas = new int[npipers];
    magnetId = 0;
    wantedRats = new int[npipers];
    previousRats = new Point[nrats];
	  for (int i = 0; i < nrats; i++) {
		  previousRats[i] = new Point();
	  }
  }

  public Point move(Point[] pipers, Point[] rats, boolean[] piperMusic) { // all rats

    this.pipers = pipers;
	  this.rats = rats;
	  this.piperMusic = piperMusic;
	  npipers = pipers.length;
	  nrats = rats.length;

    // vertical strip (not used here)
    double areaSize = (dimension / 2) / npipers; 
    double areaLeft = (id * (areaSize)) + (dimension / 2);
    double areaRight = ((id + 1) * (areaSize)) + (dimension / 2);

    // utility points
    Point gate = new Point(dimension/2, dimension/2);
    Point rightMid = new Point(dimension, dimension/2);
    Point leftMid = new Point(0, dimension/2);
    Point initialPosition = new Point((areaLeft + areaRight)/ 2, 5);
    currentLocation = pipers[id];

		if (!initi) {
			this.init();
      initi = true;
		}

    boolean haveRats = haveAllRats();
  
    // startgame
    if (getSide(currentLocation) == 0 && !haveRats)
      return moveTo(gate);

    // endgame
    if (haveAllRats()) {
      if (backAtGate) {
        return moveTo(leftMid);
      }
      else {
        if (distance(currentLocation, gate) < .5)
          backAtGate = true;
        return moveTo(gate);
      }
    }

    Point nextMove;

    switch(id) {

        case 0: nextMove = masterMove();
                break;
        case 1: if (remainingRats() <= 10) 
                  nextMove = slaveMove();
                else
                  nextMove = masterMove();
  		          break;
        default: nextMove = chaserMove();
    }

	  // sample rats' location differences to. used to tile direction
	  if(++trackingCounter == 15){  // take sample every 1.5 second
	    // save current rats as previous rat movement
	    for(int i = 0; i < nrats; i++){
	  	  previousRats[i].x = rats[i].x;
	  	  previousRats[i].y = rats[i].y;
	   	}
	    trackingCounter = 0;
      targetDelta = distance(currentLocation, rats[currentlyPursuedRat]);
	  }
    return nextMove;
  }

// ====================
// masterMove
// ====================

  public Point masterMove() {

	  Point midPoint = new Point(dimension * (3.0 / 4.0), dimension/2);
	  int nearestRat = closestRatIndex();
	  this.music = true;

    // if the hunter piper is bringing a rat, stop moving to avoid chase
    // if (piperMusic[1])
    //   return currentLocation;
	  //if (Math.abs(rats[nearestRat].x - midPoint.x) < 40 && Math.abs(rats[nearestRat].y - midPoint.y) < 40)
	  //	return chaseRat(nearestRat);
	  //else
	  //	return moveTo(midPoint);

    if (remainingRats() > 10)
      return chaseRat(nearestRat);
    else 
      return moveTo(midPoint);
  }

// ====================
// slaveMove
// ====================

  public Point slaveMove() {
    if (hunting) {
      Point nearestRat = closestRatIgnoreRadius();
      this.music = false;
      if (distance(currentLocation, nearestRat) < 10)
        hunting = false;
      return moveTo(nearestRat); 
    } else {
      this.music = true;
      if (distance(currentLocation, this.pipers[magnetId]) < 5)
        hunting = true;
      return moveTo(this.pipers[magnetId]);
    }
  }

// ====================
// chaserMove
// ====================

  public Point chaserMove() {
    this.music = true;
    int closestRatIndex = closestRatIndex();
    return chaseRat(closestRatIndex());
  }
    
// ====================
// utility methods
// ====================
  double distance(Point a, Point b) {
	  return Math.sqrt((a.x - b.x) * (a.x - b.x) + (a.y - b.y) * (a.y - b.y));
  }

  public int remainingRats() {
		int remainingRatsCounter = rats.length;
		for (int i = 0; i < rats.length; i++) {
      for (int j = 0; j < pipers.length; j++) {
			  if ((distance(pipers[j], rats[i]) < 10 && piperMusic[j]) || getSide(rats[i]) == 0) {
			  	remainingRatsCounter--;
			  }
      }
		}
    return remainingRatsCounter;
  }


// tilt direction to avoid endless chasing
// using the predicted location instead of actual location
// tested: THIS IS AWESOME
  Point chaseRat(int ratIndex){
	  Point adjustedLocation = new Point();
	  adjustedLocation.x = rats[ratIndex].x + distance(currentLocation, rats[ratIndex]) * (rats[ratIndex].x - previousRats[ratIndex].x);
	  adjustedLocation.y = rats[ratIndex].y + distance(currentLocation, rats[ratIndex]) * (rats[ratIndex].y - previousRats[ratIndex].y);

    if (adjustedLocation.y > dimension)
      adjustedLocation.y = dimension - (adjustedLocation.y - dimension);
    if (adjustedLocation.y < 0)
      adjustedLocation.y = Math.abs(adjustedLocation.y);
    if (adjustedLocation.x > dimension)
      adjustedLocation.x = dimension - (adjustedLocation.x - dimension);
    if (adjustedLocation.x < dimension/2) 
      adjustedLocation.x = dimension/2 + (dimension/2 - adjustedLocation.x);

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
    for (int i = 0; i < rats.length && i != ignoreRatIndex; i++) {
      double currentLocationDist = distance(currentLocation, rats[i]);
      if (currentLocationDist < leastDist && currentLocationDist > 10) {
		    // Edward here: adding checking for other pipers' influence
		    if (pipers != null) {
		    	boolean flag = false;
		    	for (int j = 0; j < pipers.length; j++) {
		    		if (j != id && (distance(currentLocation, rats[i]) > distance(pipers[j], rats[i])) ) {
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

  int closestRatIgnoreRadiusIndex() {
    int closestRatIndex = -1;
    double leastDist = Double.MAX_VALUE;
    for (int i = 0; i < rats.length; i++) {
      double currentLocationDist = distance(currentLocation, rats[i]);
      if (currentLocationDist < leastDist) {
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

  // wrapper function return: return the closest rat(Point) not under the influence of the piper, including rats within 10 meters of the piper
  Point closestRatIgnoreRadius() {
    return rats[closestRatIgnoreRadiusIndex()];
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
