/**
* Coordinates is a public class that represents the location of a cell in a maze.
* It contains the x,y, and z location in the maze. Several of the methods in maze
* return an instance of Coordinates.
*
* @package mazePD
* @author(David North)
*/

package mazePD;

import java.util.Objects;

public class Coordinates {
	int x;		// x coordinate for a cell location (0..maze dim-1)
	int y;		// y coordinate for a cell location (0..maze dim-1)
	int z;		// z coordinate (level) for a cell location (0..depth-1)

	/**
	* Coordinate constructor creates an instance setting the values of x,y,z
	*
	* @param  x  int x coordinate for a cell location (0..maze dim-1)
	* @param  y  int y coordinate for a cell location (0..maze dim-1)
	* @param  z  int z coordinate (level) for a cell location (0..maze dim-1)
	*/
	public Coordinates (int x, int y, int z)
	{
		setX(x);
		setY(y);
		setZ(z);
	}

	/**
	* setX() set x attribute
	*
	* @param  x  int x coordinate for a cell location (0..maze dim-1)
	*/
	public void setX(int x) { this.x = x; }

	/**
	* getX() get x attribute
	*
	* @return x  int x coordinate for a cell location (0..maze dim-1)
	*/
	public int getX() { return this.x; }

	/**
	* setY() set y attribute
	*
	* @param  y  int y coordinate for a cell location (0..maze dim-1)
	*/
	public void setY(int y) { this.y = y; }

	/**
	* getY() get y attribute
	*
	* @return y  int y coordinate for a cell location (0..maze dim-1)
	*/
	public int getY() { return this.y; }

	/**
	* setZ() set z attribute
	*
	* @param  z  int z coordinate for a cell location (0..maze dim-1)
	*/
	public void setZ(int z) { this.z = z; }

	/**
	* getZ() get z attribute
	*
	* @return z  int z coordinate for a cell location (0..maze dim-1)
	*/
	public int getZ() { return this.z; }

	/**
	* equals() compares this coordinate to another Object.
	* Overrides Object.equals() so HashSet/HashMap work correctly.
	*
	* @param obj Object - object to compare
	* @return boolean true if all coordinates are equal, false otherwise
	*/
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (!(obj instanceof Coordinates)) return false;
		Coordinates c = (Coordinates) obj;
		return x == c.x && y == c.y && z == c.z;
	}

	/**
	* hashCode() consistent with equals() so Coordinates works correctly
	* as a key in HashSet and HashMap.
	*
	* @return int hash code derived from x, y, z
	*/
	@Override
	public int hashCode()
	{
		return Objects.hash(x, y, z);
	}

	/**
	* toString() create string that represents coordinates
	*
	* @return String - of x,y, and z values for coordinate
	*/
	@Override
	public String toString()
	{
		return "[" + x + "," + y + "," + z + "]";
	}
}
