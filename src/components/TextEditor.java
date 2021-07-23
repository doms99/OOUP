package components;

import model.Location;
import model.LocationRange;
import model.TextEditorModel;
import model.manager.StackStatusListener;
import model.manager.UndoManager;
import observers.SelectionObserver;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class TextEditor extends JComponent {
  public static final int padding = 4;
  public static final int lineSpacing = 3;
  private final TextEditorModel model;

  public TextEditor(TextEditorModel model) {
    if(model == null) throw new IllegalArgumentException("Model can't be null.");

    this.model = model;

    initGUI();
  }

  private void initGUI() {
    setFocusable(true);
  }

  public Location getPointerDocumentLocation(int x, int y) {
    Graphics2D g2d = (Graphics2D) getGraphics();
    List<String> lines = model.getLines();
    int row = (int) Math.floor((double) (y-padding) / (g2d.getFont().getSize()+lineSpacing));
    row = Math.max(Math.min(row, lines.size()-1), 0);

    String line = lines.get(Math.min(row, lines.size() - 1));
    int col = line.length();
    int lineWidth = g2d.getFontMetrics().stringWidth(line);
    if(x <= 0) col = 0;
    else if(lineWidth > x)  {
      int charApprox =  (int) Math.floor((double) line.length() * x / lineWidth);
      for(int i = charApprox > 1 ? charApprox -2 : charApprox ; i <= line.length(); i++) {
        if(g2d.getFontMetrics().stringWidth(line.substring(0, i)) > x) {
          col = i-1;
          break;
        }
      }
    }
    col = Math.max(Math.min(col, lines.get(row).length()), 0);
    return new Location(row, col);
  }

  @Override
  public void paintComponent(Graphics g) {
    drawSelection(g);
    drawText(g);
    drawCursor(g);
  }

  private void drawSelection(Graphics g) {
    LocationRange selection = model.getSelectionRange();
    if(selection == null) return;

    Color originalColor = g.getColor();

    Graphics2D g2d = (Graphics2D) g;
    List<String> lines = model.getLines();
    Location start = selection.getStart();
    Location end = selection.getEnd();

    g2d.setColor(Color.PINK);
    String startLine = lines.get(start.getRow());

    int x = g2d.getFontMetrics().stringWidth(startLine.substring(0, start.getColumn()));
    int y = padding+start.getRow()*(lineSpacing+g2d.getFont().getSize());
    int width = g2d.getFontMetrics().stringWidth(start.getRow() == end.getRow() ?
        startLine.substring(start.getColumn(), end.getColumn()) :
        startLine.substring(start.getColumn()));
    int height = g2d.getFont().getSize();
    Shape rect = new Rectangle(x, y, width, height);
    g2d.fill(rect);
    g2d.draw(rect);

    if(start.getRow() != end.getRow()) {
      for(int i = start.getRow()+1; i < end.getRow(); i++) {
        rect = new Rectangle(0, padding+i*(lineSpacing+g2d.getFont().getSize()), g2d.getFontMetrics().stringWidth(lines.get(i)), height);
        g2d.fill(rect);
        g2d.draw(rect);
      }

      rect = new Rectangle(0,
          padding+end.getRow()*(lineSpacing+g2d.getFont().getSize()),
          g2d.getFontMetrics().stringWidth(lines.get(end.getRow()).substring(0, end.getColumn())),
          height);
      g2d.fill(rect);
      g2d.draw(rect);
    }

    g.setColor(originalColor);
  }

  private void drawText(Graphics g) {
    Color originalColor = g.getColor();
    Graphics2D g2d = (Graphics2D) g;
    g2d.setColor(Color.BLACK);

    int lineNum = 0;
    g2d.setColor(Color.BLACK);
    Iterator<String> iter = model.allLines();
    while(iter.hasNext()) {
      String line = iter.next();
      g2d.drawString(iter.hasNext() ? line.concat("\n") : line, 0, padding+g2d.getFont().getSize()+(lineNum++)*(lineSpacing+g2d.getFont().getSize()));
    }

    g.setColor(originalColor);
  }

  private void drawCursor(Graphics g) {
    Color originalColor = g.getColor();
    Graphics2D g2d = (Graphics2D) g;

    List<String> lines = model.getLines();
    Location cursor = model.getCursorLocation();
    g2d.setColor(Color.RED);
    int x = g2d.getFontMetrics().stringWidth(lines.get(cursor.getRow()).substring(0, cursor.getColumn()));
    int y = padding + cursor.getRow()*(lineSpacing+g2d.getFont().getSize());
    g2d.drawLine(x, y, x, y+g2d.getFont().getSize());
    g.setColor(originalColor);
  }
}
