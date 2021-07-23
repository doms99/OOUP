package model;

import model.manager.EditAction;
import model.manager.UndoManager;
import observers.CursorObserver;
import observers.SelectionObserver;
import observers.TextObserver;

import java.util.*;
import java.util.stream.IntStream;

public class TextEditorModel {
  private final LinkedList<String> lines;
  private LocationRange selectionRange ;
  private Location cursorLocation;
  private final List<CursorObserver> cursorObservers;
  private final List<TextObserver> textObservers;
  private final List<SelectionObserver> selectionObservers;

  public TextEditorModel(String initialText) {
    this.lines = new LinkedList<>(Arrays.asList(initialText.replace("\t", "    ").split("\n", -1)));
    this.selectionRange  = null;
    this.cursorLocation = new Location(lines.size()-1, lines.getLast().length());
    this.cursorObservers = new ArrayList<>();
    this.textObservers = new ArrayList<>();
    this.selectionObservers = new ArrayList<>();
  }

  public List<String> getLines() {
    return lines;
  }

  public LocationRange getSelectionRange() {
    return selectionRange;
  }

  public String getTextRange(LocationRange range) {
    if(range == null) throw new NullPointerException("Trying to get text while no selection is made.");

    Location start = range.getStart();
    Location end = range.getEnd();

    if(start.getRow() == end.getRow()) {
      return lines.get(start.getRow()).substring(start.getColumn(), end.getColumn());
    }

    StringBuilder builder = new StringBuilder();
    builder.append(lines.get(start.getRow()).substring(start.getColumn())).append("\n");
    for(int i = start.getRow()+1; i < end.getRow(); i++) {
      builder.append(lines.get(i)).append("\n");
    }
    builder.append(lines.get(end.getRow()).substring(0, end.getColumn()));

    return builder.toString();
  }

  public void setSelectionRange(LocationRange selectionRange) {
    if(this.selectionRange == null && selectionRange == null) return;
    if(selectionRange == null || selectionRange.equals(this.selectionRange)) {
      this.selectionRange = null;
    } else {
      this.selectionRange = selectionRange;
    }
    notifySelectionObservers();
  }

  public Location getCursorLocation() {
    return cursorLocation;
  }

  public void addCursorObserver(CursorObserver observer) {
    if(observer == null) return;

    this.cursorObservers.add(observer);
  }

  public void removeCursorObserver(CursorObserver observer) {
    this.cursorObservers.remove(observer);
  }

  public void addTextObserver(TextObserver observer) {
    if(observer == null) return;

    this.textObservers.add(observer);
  }

  public void removeTextObserver(TextObserver observer) {
    this.textObservers.remove(observer);
  }

  public void addSelectionObserver(SelectionObserver observer) {
    if(observer == null) return;

    this.selectionObservers.add(observer);
  }

  public void removeSelectionObserver(SelectionObserver observer) {
    this.selectionObservers.remove(observer);
  }

  public void notifyTextObservers() {
    for(TextObserver observer : textObservers) {
      observer.updateText();
    }
  }

  public void notifyCursorObservers() {
    for(CursorObserver observer : cursorObservers) {
      observer.updateCursorLocation(cursorLocation);
    }
  }

  public void notifySelectionObservers() {
    for(SelectionObserver observer : selectionObservers) {
      observer.updateSelectionRange(selectionRange);
    }
  }

  private void insert(String text, boolean pushAction) {
    if(text == null) return;

    String line = lines.remove(cursorLocation.getRow());
    String newLine = line.substring(0, cursorLocation.getColumn())+text.replace("\t", "    ")+line.substring(cursorLocation.getColumn());
    LinkedList<String> split = new LinkedList<>(Arrays.asList(newLine.split("\n", -1)));

    lines.addAll(cursorLocation.getRow(), split);
    Location newCursorLocation = new Location(cursorLocation.getRow()+split.size()-1, split.size() == 1 ?
        cursorLocation.getColumn()+text.length() :
        split.getLast().length()-line.substring(cursorLocation.getColumn()).length()
    );

    if(pushAction) {
      UndoManager.instance().push(new EditAction() {
        private final Location cursorStart = cursorLocation;
        private final String newText = text;
        private final LocationRange newTextRange = new LocationRange(cursorLocation, newCursorLocation);

        @Override
        public void execute_do() {
          moveCursor(cursorStart);
          insert(newText, false);
        }

        @Override
        public void execute_undo() {
          delete(newTextRange, false);
          moveCursor(cursorStart);
        }
      });
    }

    moveCursor(newCursorLocation);
    notifyTextObservers();
  }

  public void insert(String text) {
    insert(text, true);
  }

  public void insert(char c) {
    insert(Character.toString(c), true);
  }

  public void moveCursor(Location location) {
    if(location == null) throw new IllegalArgumentException("Cursor location can't be null.");

    cursorLocation = location;
    notifyCursorObservers();
  }

  public void moveCursorUp() {
    if(cursorLocation.getRow() == 0) return;

    int newY = Math.min(cursorLocation.getColumn(), lines.get(cursorLocation.getRow()-1).length());
    moveCursor(new Location(cursorLocation.getRow()-1, newY));
  }

  public void moveCursorDown() {
    if(cursorLocation.getRow() == lines.size()-1) return;

    int newY = Math.min(cursorLocation.getColumn(), lines.get(cursorLocation.getRow()+1).length());
    moveCursor(new Location(cursorLocation.getRow()+1, newY));
  }

  public void moveCursorLeft() {
    if(cursorLocation.getColumn() == 0) {
      if(cursorLocation.getRow() == 0) return;

      moveCursor(new Location(cursorLocation.getRow()-1, lines.get(cursorLocation.getRow()-1).length()));
      return;
    }

    moveCursor(new Location(cursorLocation.getRow(), cursorLocation.getColumn()-1));
  }

  public void moveCursorRight() {
    if(cursorLocation.getColumn() == lines.get(cursorLocation.getRow()).length()) {
      if(cursorLocation.getRow() == lines.size()-1) return;

      moveCursor(new Location(cursorLocation.getRow()+1, 0));
      return;
    }

    moveCursor(new Location(cursorLocation.getRow(), cursorLocation.getColumn()+1));
  }

  private void delete(LocationRange range, boolean pushAction) {
    if(range == null) return;
    Location start = range.getStart();
    Location end = range.getEnd();

    String newStartLine = lines.get(start.getRow()).substring(0, start.getColumn());
    String newEndLine = lines.get(end.getRow()).substring(end.getColumn());

    if(pushAction) {
      UndoManager.instance().push(new EditAction() {
        private final Location cursorStart = range.getStart();
        private final String removedText = getTextRange(range);

        @Override
        public void execute_do() {
          delete(range, false);
        }

        @Override
        public void execute_undo() {
          moveCursor(cursorStart);
          insert(removedText, false);
        }
      });
    }

    IntStream.rangeClosed(start.getRow(), end.getRow()).forEach(i -> lines.remove(start.getRow()));
    lines.add(start.getRow(), newStartLine+newEndLine);
    moveCursor(new Location(start.getRow(), start.getColumn()));
    notifyTextObservers();
  }

  public void deleteRange(LocationRange range) {
    delete(range, true);
  }

  public void deleteBefore() {
    if(cursorLocation.getColumn() == 0) {
      if(cursorLocation.getRow() == 0) return;

      delete(new LocationRange(new Location(cursorLocation.getRow()-1, lines.get(cursorLocation.getRow()-1).length()), cursorLocation), true);
      return;
    }

    delete(new LocationRange(new Location(cursorLocation.getRow(), cursorLocation.getColumn()-1), cursorLocation), true);
  }

  public void deleteAfter() {
    if(cursorLocation.getColumn() == lines.get(cursorLocation.getRow()).length()) {
      if(cursorLocation.getRow() == lines.size()-1) return;

      delete(new LocationRange(cursorLocation, new Location(cursorLocation.getRow()+1, 0)), true);
      return;
    }

    delete(new LocationRange(cursorLocation, new Location(cursorLocation.getRow(), cursorLocation.getColumn() + 1)), true);
  }

  public Iterator<String> allLines() {
    return new ModelIterator(0, lines.size());
  }

  public Iterator<String> linesRange(int start, int end) {
    if(start < 0) throw new IllegalArgumentException("Start index can't be smaller then 0.");
    if(end < start) throw new IllegalArgumentException("End must be equal or larger then start.");

    return new ModelIterator(start, end);
  }

  private class ModelIterator implements Iterator<String> {
    private int current;
    private final int end;

    public ModelIterator(int start, int end) {
      if(start < 0) throw new IllegalArgumentException("Start index can't be smaller then 0.");
      if(end < start) throw new IllegalArgumentException("End must be equal or larger then start.");

      this.current = start;
      this.end = end;
    }

    @Override
    public boolean hasNext() {
      return current < end;
    }

    @Override
    public String next() {
      if(!hasNext()) throw new NoSuchElementException("No more elements to iterate over.");

      return lines.get(current++);
    }
  }
}
