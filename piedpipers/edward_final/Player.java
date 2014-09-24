package piedpipers.edward_final;

import java.util.*;

import piedpipers.sim.Point;

public class Player extends piedpipers.sim.Player {

  // REQUIRED STUFF FOR SIM
  int npipers;
  int nrats;
  double pspeed = 0.49;
  double mpspeed = 0.09;
  static boolean initi = false;
  int[] thetas;

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
  int masterId;
  int slaveId;

  boolean hunting = false;
  boolean inPosition = false;
  boolean sweepOver = false;
  boolean handoffComplete = false;
  boolean inSection = false;
  boolean backAtGate = false; 	// piper returned to the gate
  boolean masterOn = false;
  int trackingCounter = 0;

  double areaSize;
  double areaLeft;  
  double areaRight;

  Point initialPosition;
  Point sectionMidPoint;
  Point currentLocation;
  Point intersectionLocation; 

  public void init() {
    magnetId = npipers/2;
    wantedRats = new int[npipers];
    previousRats = new Point[nrats];
    futureRats = new Point[nrats];
    for (int i = 0; i < nrats; i++) {
      previousRats[i] = new Point();
      futureRats[i] = new Point();
    }
  }

  public Point move(Point[] pipers, Point[] rats, boolean[] piperMusic, int[] thetas) { // all rats
    // variable setting
    this.pipers = pipers;
    this.rats = rats;
    this.piperMusic = piperMusic;
    this.thetas = new int[thetas.length];
    for(int i = 0; i < this.thetas.length; i++){
      //System.out.print(thetas[i] + ":");
      this.thetas[i] = ((thetas[i] + 360) % 360) ;
      //System.out.println(thetas[i] );
    }
    npipers = pipers.length;
    nrats = rats.length;

    masterId = id == npipers - 1 ? id - 1 : id + 1;
    slaveId = id - 1;// == 0 ? 1 : id -1;
    // vertical strip 
    areaSize = (dimension / 2) / npipers; 
    areaLeft = (id * (areaSize)) + (dimension / 2);
    areaRight = ((id + 1) * (areaSize)) + (dimension / 2);
    sectionMidPoint = new Point(dimension * 3.0/4.0, (id + 0.5) * (dimension / npipers));

    // utility points
    Point gate = new Point(dimension/2, dimension/2);
    Point rightMid = new Point(dimension, dimension/2);
    Point leftMid = new Point(0, dimension/2);
    initialPosition = new Point((areaLeft + areaRight)/ 2, 5);
    currentLocation = pipers[id];
    
    chasingCutoff = 3;

    if (!initi) {
      this.init();
      initi = true;
    }


    // startgame
    if (getSide(currentLocation) == 0 && !haveAllRats())
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
    
    // single piper mode:
    if(npipers == 1) {
      return lonerMove();
    }

    
    if (npipers * 20.0 < dimension * 0.45) { // case with no sweep
      if(!inPosition){
        nextMove = moveTo(sectionMidPoint);
        if(distance(currentLocation, sectionMidPoint) < 2.0){
          inPosition = true;
        }
      }else{ 
        if (remainingRats() > npipers / 2) {
          nextMove = masterMove();
        }else{
          // go to master slave 
          // turn on the master switch
          masterOn = true;
          if(id % 2 == 0){
            // slave
            nextMove = slaveMove();
          }else{
            // master 
            nextMove = masterMove();
          }
        } 
      }
    } 
    else { // case with sweep
      if (!sweepOver) {
        System.out.println("sweeping");
        nextMove = verticalSweeperMove();
      } else {
        // sweep over, turning on master swtich
        masterOn = true;
        if(id % 2 == 0){
          // slave
          nextMove = slaveMove();
        }else{
          // master 
          nextMove = masterMove();
        } 
      }
    }
    return nextMove;
  }

// ====================
// masterMove
// ====================
  
  public Point lonerMove() {
    this.music = true;
    int nearestRat = closestRatIndex();
    return chaseRat(nearestRat);
  }
    
// ====================
// masterMove
// ====================

  public Point masterMove() {
    this.music = true;
    if (masterOn && id % 2 == 1) {
      return moveTo(pipers[slaveId]);
    } else {
      return chaseRat(closestRatIndexSectioned());
    } 
  }

// ====================
// slaveMove
// ====================

  public Point slaveMove() {
    if (hunting) { // going after rats
      System.out.println("ID: " + id + " I am hunting");
      Point nearestRat = closestRatSectionedSlaveMode();
      this.music = false;
      if (distance(currentLocation, nearestRat) < 3.0){
        System.out.println("currentLocation: " + currentLocation.x + ":" + currentLocation.y);
        System.out.println("ratLocation: " + nearestRat.x + ":" + nearestRat.y);
        System.out.println("distance: " + distance(currentLocation, nearestRat));
        this.music = true;
        hunting = false;
      }
      return moveTo(nearestRat);
    } else { // returning rats
      System.out.println("ID: " + id + " I am returning the rat");
      if (distance(currentLocation, pipers[masterId]) < 2.0){
        System.out.println(distance(currentLocation, pipers[masterId]) + " close enough");
        this.music = false;
        hunting = true;
      }
      this.music = true;
      return moveTo(pipers[masterId]);
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
      if (currentLocation.y > dimension - 15)
        sweepOver = true;
      Point closestRat = closestRatBelowinHorizontalRange(areaLeft, areaRight);
      return moveTo(closestRat); 
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
        if (distance(currentLocation, pipers[0]) < 2)
          handoffComplete = true;
        return currentLocation;
      }
      return currentLocation;

    } else {

      if (!handoffComplete) {
        if (distance(currentLocation, pipers[magnetId]) < 2)
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

  public boolean hasArrivedAt(Point traveler, Point destination, double distance) {
    if (distance(traveler, destination) < .5)
      return true;
    return false;
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
    //System.out.println(remainingRatsCounter);
    return remainingRatsCounter;
  }

// tilt direction to avoid endless chasing
// using the predicted location instead of actual location
// tested: THIS IS AWESOME

  Point predictLocation(int ratIndex){
    
    Point returnPoint = new Point();
    Point ratPoint = new Point();
    ratPoint.x = rats[ratIndex].x;
    ratPoint.y = rats[ratIndex].y;

    double radian = Math.toRadians(thetas[ratIndex]);

    if(!movingTowardsMe(ratIndex)){
      // warning: ugly code ahead
      double cos = (Math.cos(radian - Math.PI / 2));
      double sin = (Math.sin(radian - Math.PI / 2));
      if(cos >= 0){ // moving left
        if(sin >= 0){ // moving up-left
          if(ratPoint.x / Math.abs(cos) > ratPoint.y / Math.abs(sin)){ // hit top first
            // inverse vertically
            ratPoint.y = -ratPoint.y;
            radian = Math.PI/2 - radian;            
            if(!movingTowardsMe(ratPoint, radian)){ // if still not moving towards me, inverse horizontally
              radian = -radian;
              ratPoint.x = dimension/2 - ratPoint.x;
            }
          }else{ // hit left first
            // inverse horizontally 
            ratPoint.x = dimension/2 - ratPoint.x;
            radian = -radian;
            if(!movingTowardsMe(ratPoint, radian)){ // if still not moving towards me, inverse vertically
              radian = Math.PI/2 - radian;
              ratPoint.y = -ratPoint.y;
            }
          }
        }else{ // moving down-left
          if(ratPoint.x / Math.abs(cos) > (dimension - ratPoint.y) / Math.abs(sin)){ // hit bottom first
            // inverse vertically
            radian = Math.PI/2 - radian;
            ratPoint.y = dimension + (dimension - ratPoint.y); 
            if(!movingTowardsMe(ratPoint, radian)){ // if still not moving towards me, inverse horizontally
              radian = -radian;
              ratPoint.x = dimension/2 - ratPoint.x;
            }
          }else{ // hit left first
            // inverse horizontally
            radian = -radian;
            ratPoint.x = dimension/2 - ratPoint.x;
            if(!movingTowardsMe(ratPoint, radian)){ // if still not moving towards me, inverse vertically
              radian = Math.PI/2 - radian;
              ratPoint.y = dimension + (dimension - ratPoint.y);
            }
          }
        }
      }else{ // moving right
        if(sin >= 0){ // moving up-right
          if((dimension - ratPoint.x) / Math.abs(cos) > ratPoint.y / Math.abs(sin)){ // hit top first
            // inverse vertically
            radian = Math.PI/2 - radian; 
            ratPoint.y = -ratPoint.y;
            if(!movingTowardsMe(ratPoint, radian)){ // if still not moving towards me, inverse horizontally
              radian = -radian;
              ratPoint.x = dimension + (dimension - ratPoint.x);
            }
          }else{ // hit right first
            // inverse horizontally
            radian = -radian;
            ratPoint.x = dimension + (dimension - ratPoint.x); 
            if(!movingTowardsMe(ratPoint, radian)){ // if still not moving towards me, inverse vertically 
              radian = Math.PI/2 - radian;
              ratPoint.y = -ratPoint.y;
            }
          }

        }else{ // moving down-right
          if((dimension -ratPoint.x) / Math.abs(cos) > (dimension - ratPoint.y) / Math.abs(sin)){ // hit bottom first
            // inverse vertically
            radian = Math.PI/2 - radian;
            ratPoint.y = dimension + (dimension - ratPoint.y);
            if(!movingTowardsMe(ratPoint, radian)){ // if still not moving towards me, inverse horizontally
              radian = -radian;
              ratPoint.x = dimension + (dimension - ratPoint.x);
            }
          }else{ // hit right first
            // inverse horizontally
            radian = -radian;
            ratPoint.x = dimension + (dimension - ratPoint.x);
            if(!movingTowardsMe(ratPoint, radian)){ // if still not moving towards me, inverse vertically 
              radian = Math.PI - radian;
              ratPoint.y = dimension + (dimension - ratPoint.y);
            }            

          }

        }
      }
      
      // right now we have ratPoint and radian as the correct location and radian
    }

    // compute future location based on radian and point 
    double offSet;
    double ratToCurrent= Math.atan2(currentLocation.x - ratPoint.x ,currentLocation.y - ratPoint.y);
    ratToCurrent = (ratToCurrent + 2 * Math.PI) % (2 * Math.PI);
    radian = (radian + 2 * Math.PI) % (2 * Math.PI); 
    double radianDifference = Math.abs(ratToCurrent - radian);
    //System.out.println("ratToCurrent: " + Math.toDegrees(ratToCurrent) + "; radian: " + Math.toDegrees(radian) + "diff: " + Math.toDegrees(radianDifference));
    offSet = distance(currentLocation, ratPoint) /(2.0 * Math.cos(radianDifference));
    //System.out.println("offset = " + offSet); 
    returnPoint.x = ratPoint.x + offSet * Math.cos(radian - Math.PI / 2);
    returnPoint.y = ratPoint.y - offSet * Math.sin(radian - Math.PI / 2);
    if (returnPoint.y > dimension)
      returnPoint.y = dimension - (returnPoint.y - dimension);
    if (returnPoint.y < 0)
      returnPoint.y = Math.abs(returnPoint.y);
    if (returnPoint.x > dimension)
      returnPoint.x = dimension - (returnPoint.x - dimension);
    if (returnPoint.x < dimension/2) 
      returnPoint.x = dimension/2 + (dimension/2 - returnPoint.x);
    return returnPoint;
  }

// tilt direction to avoid endless chasing
// using the predicted location instead of actual location
// tested: THIS IS AWESOME
  Point chaseRat(int ratIndex){
    return moveTo(futureRats[ratIndex]);
  }

  // returns the valid move 
  Point moveTo(Point goal) {
    double dist = distance(currentLocation, goal);
    double oy, ox;
    Point adjustedLocation = new Point();
    adjustedLocation.x = currentLocation.x;
    adjustedLocation.y = currentLocation.y;
    if (this.music) {
      if(dist < mpspeed){
        return goal;
      }
      ox = (goal.x - adjustedLocation.x) / dist * mpspeed;
      oy = (goal.y - adjustedLocation.y) / dist * mpspeed;
    } else {
      if(dist < pspeed){
        return goal;
      }
      ox = (goal.x - adjustedLocation.x) / dist * pspeed;
      oy = (goal.y - adjustedLocation.y) / dist * pspeed;
    }
    adjustedLocation.x += ox;
    adjustedLocation.y += oy;
    return adjustedLocation;
  }

  boolean movingTowardsMe(Point location, double radian){
    double currentToRat = Math.atan2(location.x - currentLocation.x , location.y - currentLocation.y);
    currentToRat = (currentToRat + 2 * Math.PI) % (2 * Math.PI);
    return Math.abs(currentToRat - radian) > Math.PI / 2; 
  }

  boolean movingTowardsMe(int ratIndex){
    double ratToCurrent= Math.atan2(currentLocation.x - rats[ratIndex].x ,currentLocation.y - rats[ratIndex].y);
    double currentToRat = Math.atan2(rats[ratIndex].x - currentLocation.x , rats[ratIndex].y - currentLocation.y);
    currentToRat = (currentToRat + 2 * Math.PI) % (2 * Math.PI);
    ratToCurrent = (ratToCurrent + 2 * Math.PI) % (2 * Math.PI);
    double radians = Math.toRadians(thetas[ratIndex]);
    return movingTowardsMe(rats[ratIndex], radians);
  }

  // givn an idex, figure out if the rat is already controlled by a piper
  boolean isFreeRat(int index){
    double myDist = distance(rats[index], currentLocation);
    for(int i = 0; i < npipers; i++){
      if(i == id)
        continue;
      // check:
      // 1. whether a rat is following another piper
      // 2. whether there is another piper with closer distance
      double otherDist = distance(rats[index], pipers[i]);
      if(otherDist < myDist){
        return false;
      }
    }
    return true;
  }
  // givn an idex, figure out if the rat is already controlled by a piper
  boolean isFreeRatSlaveMode(int index){
    double myDist = distance(rats[index], currentLocation);
    for(int i = 0; i < npipers; i++){
      if(i == id || i % 2 == 1) // ignore all master pipers 
        continue;
      // check:
      // 1. whether a rat is following another piper
      // 2. whether there is another piper with closer distance
      double otherDist = distance(rats[index], pipers[i]);
      if(otherDist < myDist){
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
           distance(rats[i], currentLocation) > 10.0 && 
           currentLocationDist < leastDist && 
           isFreeRat(i) && 
           movingTowardsMe(i)) {
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
    return closestRatIndex;
  }

  // For Slave: return the closest rat not under the influence of the piper for 2 sectors
  int closestRatIndexSectionedSlaveMode() {
    int closestRatIndex = -1;
    double leastDist = Double.MAX_VALUE;
    double upperY = (npipers==0)? 0 : id * dimension / npipers;
    double lowerY = (npipers==0)? 0 : (id+2) * dimension / npipers;
    System.out.println("masterId: " + masterId);
    System.out.println(">upperY: " + (rats[60].y >= upperY));
    System.out.println("<lowerY: " + (rats[60].y <= lowerY));
    System.out.println("masterDistance: " + distance(pipers[masterId], rats[60]));
    System.out.println("isFreeRat: " + isFreeRat(60));
    
    // super fancy future prediction logic
    for (int i = 0; i < rats.length; i++) {
      double masterDistance = distance(pipers[masterId], rats[i]);
      if ( getSide(rats[i]) == 1 && rats[i].y >= upperY && rats[i].y <= lowerY && 
           masterDistance > 10.0 && masterDistance < leastDist && 
           isFreeRatSlaveMode(i)) {
        closestRatIndex = i;
	      leastDist = masterDistance;
      }
    }
    System.out.println(closestRatIndex);
    return closestRatIndex;
  }
  
  Point closestRatSectionedSlaveMode(){
    int index = closestRatIndexSectionedSlaveMode();
    if( index < 0 ){
      return moveTo(new Point(dimension/2, dimension/2));
    }else{
      return rats[index];
    }
  }
  // return the closest rat not under the influence of the piper
  int closestRatIndexSectioned() {
    int closestRatIndex = -1;
    double leastDist = Double.MAX_VALUE;
    double upperY = (npipers==0)? 0 : id * dimension / npipers;
    double lowerY = (npipers==0)? 0 : (id+1) * dimension / npipers;
    // super fancy future prediction logic
    for (int i = 0; i < rats.length; i++) {
      double currentLocationDist = distance(currentLocation, futureRats[i]);
      if ( getSide(rats[i]) == 1 &&
           rats[i].y <= lowerY && rats[i].y >= upperY && 
           distance(rats[i], currentLocation) > 10.0 && 
           currentLocationDist < leastDist && 
           isFreeRat(i) && 
           movingTowardsMe(i)) {
        closestRatIndex = i;
	      leastDist = currentLocationDist;
      }
    }
    if(closestRatIndex == -1){
      for (int i = 0; i < rats.length; i++) {
      double currentLocationDist = distance(currentLocation, futureRats[i]);
        if ( getSide(rats[i]) == 1 &&
           distance(rats[i], currentLocation) > 10.0 && 
           currentLocationDist < leastDist && 
           isFreeRat(i) && 
           movingTowardsMe(i)) {
          closestRatIndex = i;
	        leastDist = currentLocationDist;
        }
      }
    }
    // in case the optimized logic failed, use the dummy-chasing closest rat logic improve ticks by 4000 :|
//    if(closestRatIndex == -1 || leastDist > dimension/2){
//      closestRatIndex = 0;
//      leastDist = Double.MAX_VALUE;
//      for(int i = 0; i < nrats; i++){
//        double currentLocationDist = distance(currentLocation, rats[i]);
//        if(getSide(rats[i]) == 1 && currentLocationDist < leastDist && currentLocationDist > 10.0){
//          closestRatIndex = i;
//          leastDist = distance(currentLocation, rats[i]);
//        }
//      }
//    }
    return closestRatIndex == -1? 0: closestRatIndex;
  }




  int closestRatBelowinHorizontalRangeIndex(double left, double right){
    int closestRat = -1;
    double leastDist = Double.MAX_VALUE;
    for (int i = 0; i < rats.length; i++) {
      if (rats[i].x >= left && rats[i].x <= right && rats[i].y > currentLocation.y) {
        double currentLocationDist = distance(currentLocation, futureRats[i]);
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
