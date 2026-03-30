/**
 * Droid — step-by-step DFS solver, ported from Droid.java.
 *
 * Call step() once per animation frame to advance one move.
 * Reads from a Maze instance; never modifies it.
 */
class Droid {
  /** @param {Maze} maze */
  constructor(maze) {
    this.maze      = maze;
    this.pos       = { ...maze.start };          // current position
    this.stack     = [{ ...maze.start }];        // DFS path stack
    this.visited   = new Set([_key(maze.start)]);// all visited cells (O(1) lookup)
    this.pathSet   = new Set([_key(maze.start)]);// cells currently on the stack
    this.stepCount = 0;
    this.done      = false;
    this.found     = false;

    // Direction vectors matching Java scanAdjLoc() order: N, E, S, W
    this._dirs = [
      { dx:  0, dy: -1 },  // D00  — north (decrease y)
      { dx:  1, dy:  0 },  // D90  — east  (increase x)
      { dx:  0, dy:  1 },  // D180 — south (increase y)
      { dx: -1, dy:  0 },  // D270 — west  (decrease x)
    ];
  }

  /**
   * Advance one step of DFS exploration.
   * @returns {boolean} true while exploration is still running, false when done.
   */
  step() {
    if (this.done) return false;
    if (this.stack.length === 0) { this.done = true; return false; }

    const { x, y } = this.stack[this.stack.length - 1]; // peek

    // Reached the exit?
    if (this.maze.getContent(x, y) === 'END') {
      this.done  = true;
      this.found = true;
      this.pos   = { x, y };
      return false;
    }

    // Try each direction in order (matching Java's D00, D90, D180, D270)
    let moved = false;
    for (const { dx, dy } of this._dirs) {
      const nx  = x + dx;
      const ny  = y + dy;
      const key = _key({ x: nx, y: ny });

      if (this.maze.canOccupy(nx, ny) && !this.visited.has(key)) {
        this.stack.push({ x: nx, y: ny });
        this.visited.add(key);
        this.pathSet.add(key);
        this.pos = { x: nx, y: ny };
        this.stepCount++;
        moved = true;
        break;
      }
    }

    if (!moved) {
      // Dead end — backtrack: pop current cell off the path
      const popped = this.stack.pop();
      this.pathSet.delete(_key(popped));
      this.stepCount++;

      if (this.stack.length === 0) {
        this.done = true;
        return false;
      }
      // Move droid back to new top of stack
      this.pos = { ...this.stack[this.stack.length - 1] };
    }

    return !this.done;
  }
}

/** Canonical string key for a {x, y} coordinate. */
function _key({ x, y }) { return `${x},${y}`; }
