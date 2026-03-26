package mazeStack;

import java.util.ArrayList;

public class LinkedStack <E> implements Stack <E>
{
private DoublyLinkedList<E> list = new DoublyLinkedList<>();

public LinkedStack() {
}

public int size() {return list.size();}

public boolean isEmpty() {return list.isEmpty();}

public void push(E element) {list.addFirst (element);}

public E peek() {return list.first();}

public E pop() {return list.removeFirst();}

public ArrayList<E> toArrayFromLast(){return list.toArrayFromLast();}
}
