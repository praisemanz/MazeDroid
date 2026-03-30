# R2D2 Maze Explorer

A Java application that generates random 3D mazes and animates a Star Wars-inspired droid (R2D2) solving them in real time using a depth-first search algorithm with backtracking.

---

## Features

- **Random maze generation** — every run produces a unique maze using a randomized DFS carving algorithm
- **3D multi-level mazes** — levels are connected by portals (↓ down, ↑ up)
- **Animated solver** — watch R2D2 explore the maze step by step, with a live trail showing the current path and backtracked cells
- **Modern dark UI** — built with Java Swing; dark-theme grid with color-coded cells and a rendered R2D2 face
- **Adjustable speed** — slider controls animation delay from slow (step-by-step) to fast
- **Configurable size** — choose from 6×6, 8×8, 10×10, or 12×12 grids

---

## How It Works

### Maze Generation
Each level is initialized as a grid of walls. A randomized DFS algorithm carves passages starting from a random cell, ensuring every open cell is reachable. Levels are linked by portal pairs — a `PORTAL_DN` on one level corresponds to a `PORTAL_UP` directly below it. The exit (`★`) is placed on the deepest level.

### Pathfinding (R2D2)
The droid uses **iterative depth-first search with backtracking**:
1. Start at the entrance, push position onto a stack, mark as visited.
2. Scan the four adjacent cells (N, E, S, W).
3. Move to the first unvisited traversable neighbour and push it.
4. If no unvisited neighbours exist, pop the stack and backtrack one step.
5. Repeat until the exit (`END`) is reached or the stack is empty (no solution).

Visited cells are stored in a `HashSet<Coordinates>` for O(1) lookup.

### Cell Types

| Symbol | Color  | Meaning             |
|--------|--------|---------------------|
| (dark) | Gray   | Wall — impassable   |
| (dim)  | Black  | Open — traversable  |
| Trail  | Green  | Visited but off path|
| Path   | Bright | Current DFS path    |
| R2D2   | Cyan   | Droid position      |
| ↓      | Purple | Portal down         |
| ↑      | Blue   | Portal up           |
| ★      | Amber  | Exit                |

---

## Project Structure

```
Maze/src/
├── mazePD/
│   ├── Maze.java           # 3D maze grid, generation, movement API
│   ├── Droid.java          # DFS solver with step/complete listener hooks
│   ├── DroidInterface.java # Interface maze uses to interact with droids
│   └── Coordinates.java    # (x, y, z) value type with hashCode/equals
├── mazeStack/
│   ├── Stack.java          # Generic stack interface
│   ├── LinkedStack.java    # Stack backed by doubly linked list
│   └── DoublyLinkedList.java
└── mazeUI/
    ├── MazeApp.java        # Swing GUI — maze panel, controls, animation
    └── Test.java           # Entry point
```

---

## Running the Project

### In Eclipse
1. Import the project (`File → Import → Existing Projects into Workspace`, point at `Maze-master/Maze`).
2. Run `mazeUI.Test` as a Java Application.

### From the terminal
```bash
cd Maze-master/Maze
mkdir -p bin
javac -d bin $(find src -name "*.java")
java -cp bin mazeUI.Test
```

Requires **Java 11** or later.

---

## Controls

| Control       | Action                                      |
|---------------|---------------------------------------------|
| ▶ Start       | Begin solving the current maze              |
| ⏹ Stop        | Halt the solver (keeps visual state)        |
| ↺ Reset       | Clear the solver trail, keep the same maze  |
| ⟳ New Maze   | Generate a fresh random maze                |
| Size selector | Switch between 6×6, 8×8, 10×10, 12×12 grids|
| Speed slider  | Control animation delay (slow ↔ fast)       |

---

## Author

David North (original maze engine) — extended with a modern Swing UI, HashSet-based visited tracking, and an event-driven animation system.
