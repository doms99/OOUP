package plugins;

import components.Frame;
import model.TextEditorModel;
import model.clipboard.ClipboardStack;
import model.manager.UndoManager;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class StatPlugin implements Plugin {
  @Override
  public String getName() {
    return "Stats";
  }

  @Override
  public String getDescription() {
    return "Computes line count, word count and letter count";
  }

  @Override
  public void execute(TextEditorModel model, UndoManager undoManager, ClipboardStack clipboardStack) {
    List<String> lines = model.getLines();
    int lineCount = lines.size(), wordCount = 0, letterCount = 0;
    for(String line : lines) {
      letterCount += line.length() + 1;
      String[] split = line.split("\s+");
      if(split.length != 1 || !split[0].equals("")) {
        wordCount += split.length;
      }
    }
    letterCount--;

    JOptionPane.showMessageDialog(null,
        String.format("Line count: %d%nWord count: %d%nLetter count: %d", lineCount, wordCount, letterCount),
        "Stats",
        JOptionPane.INFORMATION_MESSAGE);
  }
}
