package mazePD;

import java.util.HashSet;
import java.util.function.IntSupplier;

import mazePD.Maze.Content;
import mazePD.Maze.Direction;
import mazeStack.LinkedStack;

public class Droid implements DroidInterface {

    /** Called after each committed move (not during probe/undo). */
    @FunctionalInterface
    public interface StepListener {
        void onStep(Coordinates pos, int stepCount, boolean backtrack);
    }

    /** Called once when exploration finishes. */
    @FunctionalInterface
    public interface CompleteListener {
        void onComplete(boolean found, int totalSteps);
    }

    // Direction order matching scanAdjLoc() indices: 0=D00, 1=D90, 2=D180, 3=D270
    private static final Direction[] SCAN_DIRS =
        { Direction.D00, Direction.D90, Direction.D180, Direction.D270 };

    private String name;
    private Coordinates currentLocation;
    private final LinkedStack<Coordinates> stack   = new LinkedStack<>();
    private final HashSet<Coordinates>     visited = new HashSet<>();

    private StepListener     stepListener;
    private CompleteListener completeListener;
    private IntSupplier      delaySupplier;

    public Droid(String name) { this.name = name; }

    @Override
    public String getName() { return this.name; }

    public void setStepListener(StepListener l)        { this.stepListener    = l; }
    public void setCompleteListener(CompleteListener l) { this.completeListener = l; }
    /** Supply a delay in milliseconds between each committed move. */
    public void setStepDelay(IntSupplier supplier)     { this.delaySupplier   = supplier; }

    public void exploreMaze(Maze maze) {
        int stepCount = 0;

        Coordinates start = maze.enterMaze(this);
        stack.push(start);
        visited.add(start);
        setCurrentLocation(start);
        fireStep(start, stepCount, false);
        sleep();

        while (!stack.isEmpty() && !maze.scanCurLoc(this).equals(Content.END)) {
            if (Thread.currentThread().isInterrupted()) return;

            Content[]   adj  = maze.scanAdjLoc(this);
            Coordinates next = getNextMove(adj, maze);
            stepCount++;

            if (next != null) {
                stack.push(next);
                visited.add(next);
                fireStep(next, stepCount, false);
                sleep();

                // Use portal if standing on one
                if (maze.scanCurLoc(this).equals(Content.PORTAL_DN)) {
                    Coordinates dest = maze.usePortal(this, Direction.DN);
                    stack.push(dest);
                    visited.add(dest);
                    stepCount++;
                    fireStep(dest, stepCount, false);
                    moveToTopStack(maze);
                    sleep();
                }
            } else {
                stack.pop();
                if (!stack.isEmpty()) {
                    moveToTopStack(maze);
                    fireStep(stack.peek(), stepCount, true);
                    sleep();
                }
            }
        }

        boolean found = !stack.isEmpty() && maze.scanCurLoc(this).equals(Content.END);
        if (completeListener != null) completeListener.onComplete(found, stepCount);
        if (!found) System.out.println("no path found");
    }

    /**
     * Scans adjacent cells and moves the droid to the first unvisited
     * traversable neighbour, returning its coordinates.
     * Probe moves that land on an already-visited cell are immediately reversed.
     * Returns null if every direction is blocked or already visited.
     */
    public Coordinates getNextMove(Content[] mazeContent, Maze maze) {
        for (int i = 0; i < mazeContent.length; i++) {
            Content c = mazeContent[i];
            if (c == Content.EMPTY || c == Content.PORTAL_DN
                    || c == Content.PORTAL_UP || c == Content.END) {

                Coordinates candidate = maze.move(this, SCAN_DIRS[i]);
                if (!isInVisited(candidate)) {
                    return candidate;       // droid is now at candidate
                }
                // Undo the probe: reverse direction index is (i+2)%4
                maze.move(this, SCAN_DIRS[(i + 2) % 4]);
            }
        }
        return null;
    }

    /** Moves the droid one step toward the current top of the stack. */
    public void moveToTopStack(Maze maze) {
        if (stack.isEmpty()) return;
        int x = maze.getCurrentCoordinates(this).x;
        int y = maze.getCurrentCoordinates(this).y;
        Coordinates top = stack.peek();
        if (top.x == x) {
            maze.move(this, top.y > y ? Direction.D180 : Direction.D00);
        } else if (top.y == y) {
            maze.move(this, top.x > x ? Direction.D90 : Direction.D270);
        }
    }

    /** O(1) visited check — relies on Coordinates.hashCode() and equals(). */
    public boolean isInVisited(Coordinates coord) {
        return visited.contains(coord);
    }

    public Coordinates getCurrentLocation() { return currentLocation; }
    public void setCurrentLocation(Coordinates c) { this.currentLocation = c; }

    @Override
    public String toString() {
        return "Path Taken: " + stack.toArrayFromLast();
    }

    // ── Private helpers ───────────────────────────────────────

    private void fireStep(Coordinates pos, int step, boolean backtrack) {
        if (stepListener != null) stepListener.onStep(pos, step, backtrack);
    }

    private void sleep() {
        int delay = (delaySupplier != null) ? delaySupplier.getAsInt() : 0;
        if (delay <= 0) return;
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
