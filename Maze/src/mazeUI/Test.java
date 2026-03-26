package mazeUI;

import mazePD.Droid;
import mazePD.Maze;
import mazePD.Maze.MazeMode;

public class Test {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		int levels = 1;
		int dim = 6;
		System.out.println("Maze Test");
		
		Maze maze = new Maze(dim,levels,MazeMode.NORMAL);
		
		System.out.println("Maze - "+maze.toString());
		
		for (int z=0;z<levels;z++)
		{
		  System.out.println("Level - "+ z );
		  System.out.print("   ");
		  for(int i=0; i<dim; i++)
			  System.out.print(i+"  ");

		  System.out.println("");

		  String[] mazeArray = maze.toStringLevel(z);
		  for (int y=0;y<dim;y++) 						     
			  System.out.println(y+" "+ mazeArray[y]);
		}
		Droid droid = new Droid("R2D2");
		
		new Thread(new Runnable() {
			public void run() {
				droid.exploreMaze(maze);
				//System.out.println(droid.toString());
			 }
			}).start();
		
	}
}
