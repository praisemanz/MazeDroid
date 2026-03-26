package mazeStack;
import java.util.ArrayList;

public class DoublyLinkedList<E> {
	private static class Node<E>{
		private E element;
		private Node<E> next;
		private Node<E> previous;
		
		public Node(E e, Node<E> n, Node<E> p) {
			element = e;
			next = n;
			previous = p;
		}
		public E getElement() {
			return element;
		}
		public Node<E> getNext() {
			return next;
		}
		public void setNext(Node<E> next) {
			this.next = next;
		}
		public Node<E> getPrevious() {
			return previous;
		}
		public void setPrevious(Node<E> previous) {
			this.previous = previous;
		}
		
	}
	
	private Node<E> head;
	
	private Node <E> tail;
	
	private int size = 0;
	
	public DoublyLinkedList() {
		head = new Node<>(null,null,null);
		tail = new Node<>(null,null,head);
		head.setNext(tail);
		
	}
	
	public int size() {return size;	}
	
	public boolean isEmpty() {return size==0;}
	
	public E first() {
		if(isEmpty())return null;
		return head.getNext().getElement();
	}
	
	public E last() {
		if(isEmpty())return null;
		return tail.getPrevious().getElement();
	}
	
	public void addFirst(E e)
	{
		Node<E> newest = new Node<>(e,null, null);
		newest.setNext(head.getNext());
		newest.setPrevious(head);
		
		head.getNext().setPrevious(newest);
		
		head.setNext(newest);
		
		size++;
	}
	
	public void addLast(E e) {
		Node<E> newest = new Node<>(e, null, null);
		newest.setNext(tail);
		newest.setPrevious(tail.getPrevious());
		
		
		
		tail.getPrevious().setNext(newest);
		
		tail.setPrevious(newest);
		
		size++;
	}
	
	public E removeFirst() {
		if(isEmpty())return null;
		Node<E> rem = head.getNext();
		E answer = rem.getElement();
		rem.getNext().setPrevious(head);
		head.setNext(rem.getNext());
		size--;
		if(size == 0)tail.setPrevious(head);
		return answer;
	}
	
	public E removeLast() {
		if(isEmpty())return null;
		Node<E> rem = tail.getPrevious();
		E answer = rem.getElement();
		rem.getPrevious().setNext(tail);
		tail.setPrevious(rem.getPrevious());
		size--;
		if(size == 0)head.setNext(tail);
		return answer;
	}
	
	public DoublyLinkedList<E> clone(){
		DoublyLinkedList<E> listCopy = new DoublyLinkedList<E>();
		Node<E> listNode = head.getNext();
		while(listNode.getNext() != null)
		{
			listCopy.addLast(listNode.getElement());
			listNode = listNode.getNext();
		}
		return listCopy;
	}
	public ArrayList<E> toArrayFromFirst()
	{
		ArrayList<E> array = new ArrayList();
		Node<E> node = head.getNext();
		while(node.getNext() != null) {
			array.add(node.getElement());
			node = node.getNext();
		}
		return array;
	}
	
	public ArrayList<E> toArrayFromLast()
	{
		ArrayList<E> array = new ArrayList();
		Node<E> node = tail.getPrevious();
		while(node.getPrevious() != null) {
			array.add(node.getElement());
			node = node.getPrevious();
		}
		return array;
	}
	
	
}
