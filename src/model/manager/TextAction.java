package model.manager;

import model.Location;
import model.LocationRange;
import model.TextEditorModel;

import java.util.List;

public class TextAction implements EditAction {
  private final TextEditorModel model;
  private final LocationRange oldRange;
  private final LocationRange newRange;
  private final String newLines;
  private final String oldLines;

  public TextAction(LocationRange oldRange, LocationRange newRange, String newLines, String oldLines, TextEditorModel model) {
    if(newRange == null || oldRange == null) throw new NullPointerException("Ranges can't be null.");
    if(newLines == null || oldLines == null) throw new NullPointerException("Lines can't be null.");
    if(model == null) throw new NullPointerException("Model can't be null.");

    this.oldRange = oldRange;
    this.newRange = newRange;
    this.newLines = newLines;
    this.oldLines = oldLines;
    this.model = model;
  }

  @Override
  public void execute_do() {
    model.moveCursor(oldRange.getStart());
    model.deleteRange(oldRange);
    model.insert(newLines);
  }

  @Override
  public void execute_undo() {
    model.moveCursor(newRange.getStart());
    model.deleteRange(newRange);
    model.insert(oldLines);
  }
}
