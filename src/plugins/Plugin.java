package plugins;

import model.TextEditorModel;
import model.clipboard.ClipboardStack;
import model.manager.UndoManager;

public interface Plugin {
  String getName();
  String getDescription();
  void execute(TextEditorModel model, UndoManager undoManager, ClipboardStack clipboardStack);
}
