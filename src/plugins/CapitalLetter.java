package plugins;

import model.Location;
import model.TextEditorModel;
import model.clipboard.ClipboardStack;
import model.manager.UndoManager;

import java.util.Iterator;

public class CapitalLetter implements Plugin {
  @Override
  public String getName() {
    return "Capital letter";
  }

  @Override
  public String getDescription() {
    return "Transforms every words first letter to be uppercase letter";
  }

  @Override
  public void execute(TextEditorModel model, UndoManager undoManager, ClipboardStack clipboardStack) {
    Iterator<String> iter = model.allLines();

    int row = 0;
    while(iter.hasNext()) {
      String line = iter.next();
      int col = 0;
      boolean spaceFound = true;
      for(char c : line.toCharArray()) {
        if(Character.isLetter(c) && spaceFound) {
          spaceFound = false;
          model.moveCursor(new Location(row, col));
          model.deleteAfter();
          model.insert(Character.toUpperCase(c));
        } else if(Character.isWhitespace(c)) spaceFound = true;

        col++;
      }
      row++;
    }
  }
}
