package piedpipers.monday_deliverable;

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
  Point[] futureRats;
  boolean[] piperMusic;
  Point[] previousRats;
  int chasingCutoff;

   // enum for different phase
  enum Phase {
    SWEEP_PHASE,
    HANDOFF_PHASE,
    GREEDY_PHASE
  }

  Phase currentPhase;
 
  int magnetId;

  boolean hunting = false;
  boolean inPosition = false;
  boolean sweepOver = false;
  boolean handoffComplete = false;
  boolean inSection = false;
  boolean backAtGate = false; 	// piper returned to the gate
  int trackingCounter = 0;

  double areaSize;
  double areaLeft;  
  double areaRight;

  Point initialPosition;
  Point sectionMidPoint;
  Point currentLocation;
  
  public void init() {
    thetas = new int[npipers];
    magnetId = npipers/2;
    wantedRats = new int[npipers];
    previousRats = new Point[nrats];
    futureRats = new Point[nrats];
    for (int i = 0; i < nrats; i++) {
      previousRats[i] = new Point();
      futureRats[i] = new Point();
    }
  }

  public Point move(Point[] pipers, Point[] rats, boolean[] piperMusic) { // all rats

    this.pipers = pipers;
	  this.rats = rats;
	  this.piperMusic = piperMusic;
	  npipers = pipers.length;
	  nrats = rats.length;
    chasingCutoff = 3;

    // vertical strip 
    areaSize = (dimension / 2) / npipers; 
    areaLeft = (id * (areaSize)) + (dimension / 2);
    areaRight = ((id + 1) * (areaSize)) + (dimension / 2);
    sectionMidPoint = new Point(dimension * (3.0 / 4.0), (id + 0.5) * (dimension / npipers));

    // utility points
    Point gate = new Point(dimension/2, dimension/2);
    Point rightMid = new Point(dimension, dimension/2);
    Point leftMid = new Point(0, dimension/2);
    initialPosition = new Point((areaLeft + areaRight)/ 2, 5);
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

    // calculate future rats' location before move
    for(int i = 0; i < nrats; i++){
      futureRats[i] = predictLocation(i);
    }

    Point nextMove = gate;
    // case:
    // 1. total coverage with piper line-up is more than 2/3 of the total width,
    // 2. rats per pier is greater than 100. dont have to do greedy
    // = SWEEP
/*    
    if (npipers * 20.0 > dimension * 0.3 && npipers * 100 > nrats) {
      System.out.println("sweep");
      if (!sweepOver) 
        nextMove = verticalSweeperMove(); 
      else if (!handoffComplete)
        nextMove = handoffMove();
    }

    // After sweep / no sweep
    // do the master/slave move
    if(id == magnetId){
      nextMove = masterMove();
    } else {
      if(remainingRats() <= chasingCutoff) {
        nextMove = slaveMove();
      }else{
        nextMove = masterMove();
      }
    }


*/


    // case with no sweep
    if (npipers * 20.0 < dimension * 0.3 || npipers * 100 < nrats) {
      if(!inPosition){
        nextMove = moveTo(sectionMidPoint);
        if(distance(currentLocation, sectionMidPoint) < 2.0){
          inPosition = true;
        }
      }else{
        if (id == magnetId) {
           nextMove = masterMove();
        } else {
           if (remainingRats() <= chasingCutoff) 
             nextMove = slaveMove();
           else
             nextMove = masterMove();
        }
      }
    } 

    // case with sweep
    else {
      if (id == magnetId) {
        if (!sweepOver) {
          nextMove = verticalSweeperMove();
        }
        else if (!handoffComplete){
          nextMove = handoffMove();
        }
        else{
          nextMove = masterMove();
          }
      } else {
        if (!sweepOver){ 
          nextMove = verticalSweeperMove();
        }
        else if (!handoffComplete || !inSection){
          nextMove = handoffMove();
        }
        else {
          if (remainingRats() <= chasingCutoff) {
            nextMove = slaveMove();
          }
          else {
            nextMove = masterMove();
          }
        }
      }
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
// masterMove
// ====================

  public Point masterMove() {
    Point midPoint = new Point(dimension * (3.0 / 4.0), dimension/2);
    int nearestRat = closestRatIndex();
    this.music = true;
    if (remainingRats() < chasingCutoff && npipers > 1 ) // move to middle only if there is at least one slave  
      return moveTo(midPoint);
    else 
      return chaseRat(nearestRat);
  }

// ====================
// slaveMove
// ====================

  public Point slaveMove() {
    if (hunting) {
      Point nearestRat = closestRatSlaveMode2();
      this.music = false;
      if (distance(currentLocation, nearestRat) < 10)
        hunting = false;
      return moveTo(nearestRat);
    } else {
      this.music = true;
      if (distance(currentLocation, pipers[magnetId]) < .5)
        hunting = true;
      return moveTo(pipers[magnetId]);
    }
  }

// ====================
// chaserMove
// ====================

  public Point chaserMove() {
    this.music = true;
    Point midPoint = new Point(dimension * (3.0 / 4.0), dimension/2);
    int closestRatIndex = closestRatIndex();
    if (closestRatIndex == -1)
      return moveTo(midPoint);
    return chaseRat(closestRatIndex());
  }

// ====================
// verticalSweeperMove
// ====================

  public Point verticalSweeperMove() {
    if (inPosition) {
      this.music = true;
      if (currentLocation.y > dimension - 5)
        sweepOver = true;
      int closestRatIndex = closestRatBelowinHorizontalRangeIndex(areaLeft, areaRight);
      if (closestRatIndex == -1)
        return moveTo(new Point((areaLeft + areaRight)/2, dimension));
      else
        return moveTo(rats[closestRatIndex]); 
    } else {
      this.music = false;
      if (distance(currentLocation, initialPosition) < 1)
        inPosition = true;
      return moveTo(initialPosition);  
    }
  }
    
// ====================
// handoffMove
// ====================

  public Point handoffMove() {
    this.music = true;
    if (id == magnetId) {

      if (!handoffComplete) {
        if (distance(currentLocation, pipers[0]) < 7)
          handoffComplete = true;
        return currentLocation;
      }
      return currentLocation;

    } else {

      if (!handoffComplete) {
        if (distance(currentLocation, pipers[magnetId]) < 7)
          handoffComplete = true;
        return moveTo(pipers[magnetId]);
      }
      else {
        if (!inSection) {
          this.music = false;
          if (distance(currentLocation, sectionMidPoint) < 1)
            inSection = true;
          return moveTo(sectionMidPoint);
        } else {
          return moveTo(sectionMidPoint);
        }
      }
    }
  }

// ====================
// utility methods
// ====================

  double distance(Point a, Point b) {
	  return Math.sqrt((a.x - b.x) * (a.x - b.x) + (a.y - b.y) * (a.y - b.y));
  }

  double ratsPerSquareMeter() {
    double area = dimension * (dimension/2);
    double rats = (double)remainingRats();
    return rats/area;
  }

  double portionOfWidthCovered() {
    double doubleDimension = (double)(dimension/2);
    double doublePipers = (double)npipers;
    return ((doublePipers * 20)/doubleDimension) * 100;
  }

  double portionOfHeightCovered() {
    double doubleDimension = (double)(dimension);
    double doublePipers = (double)npipers;
    return ((doublePipers * 20)/doubleDimension) * 100;
  }

  public int remainingRats() {
    int remainingRatsCounter = rats.length;
    for (int i = 0; i < rats.length; i++) {
      for (int j = 0; j < pipers.length; j++) {
        if ((distance(pipers[j], rats[i]) < 10 && piperMusic[j]) || getSide(rats[i]) == 0) {
	  remainingRatsCounter--;
          break;
        }
      }
    }
    System.out.println(remainingRatsCounter);
    return remainingRatsCounter;
  }

// tilt direction to avoid endless chasing
// using the predicted location instead of actual location
// tested: THIS IS AWESOME
  Point predictLocation(int ratIndex){
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

	  return adjustedLocation;
  }

  // tilt direction to avoid endless chasing
  // using the predicted location instead of actual location
  // tested: THIS IS AWESOME
  Point chaseRat(int ratIndex){
    return moveTo(futureRats[ratIndex]);
  }

  // returns the valid move 
  Point moveTo(Point goal) {
    //System.out.println("goal location: "+ goal.x + ":" + goal.y);
    double dist = distance(currentLocation, goal);
    double oy, ox;
    Point adjustedLocation = new Point();
    adjustedLocation.x = currentLocation.x;
    adjustedLocation.y = currentLocation.y;
    if (this.music) {
      ox = (goal.x - adjustedLocation.x) / dist * mpspeed;
      oy = (goal.y - adjustedLocation.y) / dist * mpspeed;
    } else {
      ox = (goal.x - adjustedLocation.x) / dist * pspeed;
      oy = (goal.y - adjustedLocation.y) / dist * pspeed;
    }
    adjustedLocation.x += ox;
    adjustedLocation.y += oy;
    //System.out.println(adjustedLocation.x + ":" + adjustedLocation.y);
    return adjustedLocation;
  }

  boolean movingTowardsMe(Point piper, Point previous_rat, Point rat, Point futureRat) {
      double x = (rat.x - previous_rat.x) * (futureRat.x - piper.x);
      double y = (rat.y - previous_rat.y) * (futureRat.y - piper.y);
      return x + y < 0.0;
  }

  // givn an idex, figure out if the rat is already controlled by a piper
  boolean isFreeRat(int index){
    for(int i = 0; i < npipers; i++){
      if(i == id)
        continue;
      // check:
      // 1. whether a rat is following another piper
      // 2. whether there is another piper with closer distance
      if((distance(rats[index], pipers[i]) < 10 && piperMusic[i]) || distance(rats[index], pipers[i]) < distance(futureRats[index], currentLocation)){
        return false;
      }
    }
    return true;
  }

  // return the closest rat not under the influence of the piper
  int closestRatIndex() {
    int closestRatIndex = -1;
    double leastDist = Double.MAX_VALUE;
    // super fancy future prediction logic
    for (int i = 0; i < rats.length; i++) {
      double currentLocationDist = distance(currentLocation, futureRats[i]);
      if ( getSide(rats[i]) == 1 &&
           Math.max(currentLocationDist, distance(rats[i], futureRats[i])) < leastDist && 
           distance(rats[i], currentLocation) > 10.0 && 
           isFreeRat(i) && 
           movingTowardsMe(pipers[id], previousRats[i], rats[i], futureRats[i])) {
        closestRatIndex = i;
	leastDist = currentLocationDist;
      }
    }
    // in case the optimized logic failed, use the dummy-chasing closest rat logic improve ticks by 4000 :|
    if(closestRatIndex == -1 || leastDist > dimension/2){
      closestRatIndex = 0;
      leastDist = Double.MAX_VALUE;
      for(int i = 0; i < nrats; i++){
        double currentLocationDist = distance(currentLocation, rats[i]);
        if(getSide(rats[i]) == 1 && currentLocationDist < leastDist && currentLocationDist > 10.0){
          closestRatIndex = i;
          leastDist = distance(currentLocation, rats[i]);
        }
      }
    }
    //System.out.println("==" + closestRatIndex + "==");
    //System.out.println("futureRatLocation: " + futureRats[closestRatIndex].x + ":" + futureRats[closestRatIndex].y);
    return closestRatIndex;
  }


  // wrapper functionreturn: return the closest rat(Point) not under the influence of the piper
  Point closestRat() {
    return rats[closestRatIndex()];
  }

  int closestRatSlaveModeIndex() {
    int counter = 0;
    int closestRatIndex = -1;
    double leastDist = Double.MAX_VALUE;
    for (int i = 0; i < rats.length; i++) {
      double currentLocationDist = distance(currentLocation, rats[i]);
      if (currentLocationDist < leastDist && distance(pipers[magnetId], rats[i]) > 10 && getSide(rats[i]) != 0) {
		    closestRatIndex = i;
        if (counter == id)
          return i;
        else {
          counter = counter + 1; 
          if (counter == magnetId)
            counter = counter + 1;
        }
		    leastDist = currentLocationDist;
		  }
    }
    return closestRatIndex;
  }
  
  boolean isMyRat(int index){
    double myDist = distance(currentLocation, rats[index]);
    for(int i = 0; i < npipers; i++){
      if(i != id && distance(pipers[i], rats[index]) < myDist - 5){
        return false;
      }
    }
    return true;
  }
  
  int closestRatSlaveModeIndex2(){
    int closestRatIndex = 0;
    double leastDist = Double.MAX_VALUE;
    for (int i = 0; i < rats.length; i++) {
      double currentLocationDist = distance(currentLocation, rats[i]);
      if (getSide(rats[i]) != 0 &&
          currentLocationDist < leastDist && 
          distance(pipers[magnetId], rats[i]) > 10 && 
          isMyRat(i)
          ) {
        closestRatIndex = i;
        leastDist = currentLocationDist;
      }
    }
    return closestRatIndex;

  }
  // wrapper function return: return the closest rat(Point) not under the influence of the piper, including rats within 10 meters of the piper
  Point closestRatSlaveMode() {
    return rats[closestRatSlaveModeIndex()];
  }

  Point closestRatSlaveMode2() {
    return rats[closestRatSlaveModeIndex2()];
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
    int closestRat = -1;
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
    int closestRatIndex = closestRatBelowinHorizontalRangeIndex(left, right);
    if (closestRatIndex == -1) {
      return new Point((left + right) / 2, dimension);
    } else {
      return rats[closestRatBelowinHorizontalRangeIndex(left, right)];
    }
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
