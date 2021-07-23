package model.clipboard;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class ClipboardStack {
  private final Stack<String> texts;
  private final List<ClipboardObserver> observers;

  public ClipboardStack() {
    this.texts = new Stack<>();
    this.observers = new ArrayList<>();
  }

  public void addObserver(ClipboardObserver observer) {
    if(observer == null) return;

    observers.add(observer);
  }

  public void removeObserver(ClipboardObserver observer) {
    observers.remove(observer);
  }

  private void notifyObservers() {
    for(ClipboardObserver observer : observers) {
      observer.updateClipboard();
    }
  }

  public void push(String text) {
    if(text == null) return;

    texts.push(text);
    notifyObservers();
  }

  public String peek() {
    return texts.peek();
  }

  public String pop() {
    String result = texts.pop();
    notifyObservers();

    return result;
  }

  public boolean isEmpty() {
    return texts.empty();
  }

  public void clear() {
    texts.clear();
    notifyObservers();
  }
}
