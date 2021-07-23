package model.manager;

import model.manager.EditAction;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class UndoManager {
  private static final UndoManager singleton = new UndoManager();
  private final Stack<EditAction> undoStack;
  private final Stack<EditAction> redoStack;
  private final List<StackStatusListener> undoStackListeners;
  private final List<StackStatusListener> redoStackListeners;

  private UndoManager() {
    this.undoStack = new Stack<>();
    this.redoStack = new Stack<>();
    this.undoStackListeners = new ArrayList<>();
    this.redoStackListeners = new ArrayList<>();
  }

  public static UndoManager instance() {
    return singleton;
  }

  private void notifyUndoListeners() {
    for(StackStatusListener listener : undoStackListeners) {
      listener.statusChanged(undoStack.empty());
    }
  }

  private void notifyRedoListeners() {
    for(StackStatusListener listener : redoStackListeners) {
      listener.statusChanged(redoStack.empty());
    }
  }

  public void addRedoListener(StackStatusListener listener) {
    if(listener == null) return;

    redoStackListeners.add(listener);
  }

  public void removeRedoListener(StackStatusListener listener) {
    redoStackListeners.remove(listener);
  }

  public void addUndoListener(StackStatusListener listener) {
    if(listener == null) return;

    undoStackListeners.add(listener);
  }

  public void removeUndoListener(StackStatusListener listener) {
    undoStackListeners.remove(listener);
  }

  public void undo() {
    if(undoStack.empty()) return;

    EditAction action = undoStack.pop();
    if(undoStack.empty()) notifyUndoListeners();

    action.execute_undo();

    boolean wasEmpty = redoStack.empty();
    redoStack.push(action);
    if(wasEmpty) notifyRedoListeners();
  }

  public void redo() {
    if(redoStack.empty()) return;

    EditAction action = redoStack.pop();
    if(redoStack.empty()) notifyRedoListeners();

    action.execute_do();

    boolean wasEmpty = undoStack.empty();
    undoStack.push(action);
    if(wasEmpty) notifyUndoListeners();
  }

  public void push(EditAction action) {
    if(!redoStack.empty()) {
      redoStack.clear();
      notifyRedoListeners();
    }

    boolean wasEmpty = undoStack.empty();
    undoStack.push(action);
    if(wasEmpty) notifyUndoListeners();
  }
}
