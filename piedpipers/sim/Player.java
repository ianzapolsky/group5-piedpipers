package piedpipers.sim;

public abstract class Player {
	public int id; // id of the piper, 1,2,3...npiper
	public int dimension;
	
	public boolean music;

	public Player() {
	}

	public abstract void init();

	// Return: the next position
	// my position: pipers[id-1]
	//public abstract Point move(Point[] pipers, // positions of pipers
		//	Point[] rats, boolean[] pipermusic); // positions of the rats
	
	public abstract Point move(Point[] pipers, // positions of pipers
			Point[] rats, boolean[] pipermusic, int[] thetas); 

}
