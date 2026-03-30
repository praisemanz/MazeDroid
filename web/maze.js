/**
 * Maze — generates a 2-D maze using the same randomised DFS carving
 * algorithm as the original Java implementation.
 *
 * Cell contents: 'BLOCK' | 'EMPTY' | 'END'
 *
 * Coordinate convention: cells[y][x], matching the Java grid layout.
 */
class Maze {
  /**
   * @param {number} dim  — grid size (dim × dim)
   * @param {number|null} seed — fixed seed for reproducible mazes, or null for random
   */
  constructor(dim, seed = null) {
    this.dim = dim;

    // Seeded LCG so the TEST mode works like in Java (seed 37)
    if (seed !== null) {
      let s = seed | 0;
      this._rng = () => {
        s = Math.imul(s, 1664525) + 1013904223;
        return (s >>> 0) / 0x100000000;
      };
    } else {
      this._rng = Math.random.bind(Math);
    }

    this._build();
  }

  // ── Public API ────────────────────────────────────────────────────────

  /** Content of cell (x, y). */
  getContent(x, y) { return this._cells[y][x].content; }

  /** True if the droid can stand on (x, y). */
  canOccupy(x, y) {
    if (x < 0 || x >= this.dim || y < 0 || y >= this.dim) return false;
    return this._cells[y][x].content !== 'BLOCK';
  }

  // ── Generation ────────────────────────────────────────────────────────

  _build() {
    const dim = this.dim;

    // Initialise every cell as a wall
    this._cells = Array.from({ length: dim }, (_, y) =>
      Array.from({ length: dim }, (_, x) => ({
        x, y,
        content:  'BLOCK',
        genVisit: false,
      }))
    );

    // Pick a random cell to be the END — this is the root of the DFS tree,
    // mirroring how the Java code starts carving from the portal/end cell.
    const ex = Math.floor(this._rng() * dim);
    const ey = Math.floor(this._rng() * dim);
    this._cells[ey][ex].content = 'END';
    this.end = { x: ex, y: ey };

    this._createPath(ex, ey);

    // Pick entrance from carved EMPTY cells, biased toward cells far from END
    const candidates = [];
    for (let y = 0; y < dim; y++)
      for (let x = 0; x < dim; x++)
        if (this._cells[y][x].content === 'EMPTY')
          candidates.push({ x, y, dist: Math.abs(x - ex) + Math.abs(y - ey) });

    candidates.sort((a, b) => b.dist - a.dist);
    const pool = Math.max(1, Math.floor(candidates.length * 0.3));
    this.start = candidates[Math.floor(this._rng() * pool)];
  }

  /**
   * Iterative DFS path carver — faithful port of Maze.java createPath().
   * Multiple neighbours can be pushed per iteration; genVisit is set on pop.
   */
  _createPath(startX, startY) {
    const stack = [this._cells[startY][startX]];

    while (stack.length > 0) {
      const cur = stack.pop();
      cur.genVisit = true;

      const adj = this._getAdj(cur);
      const ri  = Math.floor(this._rng() * adj.length);

      for (let i = 0; i < adj.length; i++) {
        const next = adj[(i + ri) % adj.length];
        if (this._isOkForPath(cur, next)) {
          next.content = 'EMPTY';
          stack.push(next);
        }
      }
    }
  }

  /** Return the (up to 4) grid-adjacent cells of `cell`. */
  _getAdj(cell) {
    const { x, y } = cell;
    const adj = [];
    if (x > 0)           adj.push(this._cells[y][x - 1]);
    if (x < this.dim-1)  adj.push(this._cells[y][x + 1]);
    if (y > 0)           adj.push(this._cells[y - 1][x]);
    if (y < this.dim-1)  adj.push(this._cells[y + 1][x]);
    return adj;
  }

  /**
   * Port of Maze.java isOkForPath().
   * A move from `from` to `to` is valid when:
   *   - `to` has not yet been visited during generation
   *   - The cells perpendicular to the direction of travel at `to` are both
   *     walls (or boundary), preventing 2×2 open rooms.
   */
  _isOkForPath(from, to) {
    if (to.genVisit) return false;

    const dim = this.dim;

    if (from.x === to.x) {
      // Vertical move — check cells to the left and right of `to`
      const lx = to.x - 1;
      const rx = to.x + 1;
      const leftWall  = lx < 0        || this._cells[to.y][lx].content === 'BLOCK';
      const rightWall = rx >= dim      || this._cells[to.y][rx].content === 'BLOCK';
      if (lx >= 0 && rx < dim) return leftWall && rightWall;
      if (lx < 0)    return rightWall;
      if (rx >= dim) return leftWall;
    } else {
      // Horizontal move — check cells above and below `to`
      const ay = to.y - 1;
      const by = to.y + 1;
      const aboveWall = ay < 0        || this._cells[ay][to.x].content === 'BLOCK';
      const belowWall = by >= dim      || this._cells[by][to.x].content === 'BLOCK';
      if (ay >= 0 && by < dim) return aboveWall && belowWall;
      if (ay < 0)   return belowWall;
      if (by >= dim) return aboveWall;
    }
    return false;
  }
}
