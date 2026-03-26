package mazePD;

import java.util.ArrayList;

import mazePD.Maze.Content;
import mazePD.Maze.Direction;
import mazeStack.LinkedStack;

public class Droid implements DroidInterface {
	private String name;
	private Coordinates currentLocation;
	private LinkedStack <Coordinates> stack = new LinkedStack<>();
	private ArrayList <Coordinates> visited = new ArrayList<>();
	
	public Droid(String name){
		this.name=name;
	}

	public String getName() {
		// TODO Auto-generated method stub
		return this.name;
	}
	
	public void exploreMaze(Maze maze) {
		
		stack.push(maze.enterMaze(this));
		visited.add(stack.peek());
		this.setCurrentLocation(stack.peek());

		while(!stack.isEmpty() && ! maze.scanCurLoc(this).equals(Content.END))
		{
			Content[] arraymaze = maze.scanAdjLoc(this);
			Coordinates newmove = getNextMove(arraymaze, maze);
			if( newmove != null) {
				System.out.println("maze location: " + stack.peek());
					for(int i=0; i<arraymaze.length; i++)
						System.out.print(arraymaze[i]+" ");
					System.out.println("\n");
					stack.push(newmove);
					
					visited.add(newmove);
					
					if(maze.scanCurLoc(this).equals(Content.PORTAL_DN))
					{
						stack.push(maze.usePortal(this, Direction.DN));
						this.moveToTopStack(maze);
					}
			}
			else {
				stack.pop();
				this.moveToTopStack(maze);
				
			}
		}
		
		if(stack.isEmpty()) System.out.println("no path found");
	}
	
	public Coordinates getNextMove(Content[] mazeContent, Maze maze) {
		
		Coordinates newone = new Coordinates(0,0,0);

		for(int i=0; i<mazeContent.length; i++)
		{
			
			if((mazeContent[i].equals(Content.EMPTY)) || (mazeContent[i].equals(Content.PORTAL_DN)) || (mazeContent[i].equals(Content.PORTAL_UP)) || (mazeContent[i].equals(Content.END)))
				{
				 newone = maze.move(this, moveDir(i));
				if(!this.isInVisited(newone)) {
					return newone;
				}
				
				// this condition sets the droid's position back to the initial position
				// before they tried to see which one was next on line 64;
				// it basically undo the line 64
				else {
					if(i==0) maze.move(this, moveDir(2));
					else if (i==1) maze.move(this, moveDir(3));
					else if (i==2) maze.move(this, moveDir(0));
					else if (i==3) maze.move(this, moveDir(1));
				}
		}
		}
		return null;
	}
	
	/*
	 * This function brings the maze to the position at the top of the stack
	 * */
	
	public void moveToTopStack(Maze maze) {
		int x = maze.getCurrentCoordinates(this).x;
		int y = maze.getCurrentCoordinates(this).y;
		if(stack.peek().x == x)
			if(stack.peek().y > y)
				maze.move(this, Direction.D180);
			else
				maze.move(this, Direction.D00);
		else if (stack.peek().y == y)
			if(stack.peek().x > x)
				maze.move(this, Direction.D90);
			else
				maze.move(this, Direction.D270);
	}
	
	
	public Coordinates getCurrentLocation() {
		return currentLocation;
	}

	public void setCurrentLocation(Coordinates currentLocation) {
		this.currentLocation = currentLocation;
	}
	
	/*
	 * this fuction receives a number and determine which position is appropriate
	 * the index is from the table containing the contents of adjacent cells
	 * */
	public Direction moveDir(int index) {
		if(index==0)return Direction.D00;
		else if (index==1)return Direction.D90;
		else if (index==2)return Direction.D180;
		else if (index==3)return Direction.D270;
		return null;
	}
	
	/*
	 * This function checks to see if a cell was already visited by the droid
	 * */
	public Boolean isInVisited(Coordinates given) {
		
		for(Coordinates c: visited)
			if(c.equals(given)) return true;
		
		return false;		
	}
	
	
	public String toString() {
		return " Path Taken: "+stack.toArrayFromLast();
	}
	
	
	

}
