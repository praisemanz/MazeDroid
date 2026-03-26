package mazeStack;

public interface Stack<E> {
	int size();
	boolean isEmpty();
	E peek();
	void push(E element);
	E pop();}
