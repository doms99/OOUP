package components;

import plugins.Plugin;
import model.Location;
import model.LocationRange;
import model.TextEditorModel;
import model.clipboard.ClipboardObserver;
import model.clipboard.ClipboardStack;
import model.manager.StackStatusListener;
import model.manager.UndoManager;
import observers.CursorObserver;
import observers.SelectionObserver;
import observers.TextObserver;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.List;

public class Frame extends JFrame {
  private static final Path pluginFolder = Path.of("./plugins/");
  private final TextEditorModel model;
  private Path savePath;
  private boolean ctrlPressed;
  private Location dragStartLocation;
  private int clickCount;
  private final ClipboardStack clipboard;

  private final Action ctrlOff = new AbstractAction() {
    @Override
    public void actionPerformed(ActionEvent e) {
      ctrlPressed = false;
    }
  };
  private final Action ctrlOn = new AbstractAction() {
    @Override
    public void actionPerformed(ActionEvent e) {
      ctrlPressed = true;
    }
  };
  private final Action open = new AbstractAction() {
    @Override
    public void actionPerformed(ActionEvent e) {
      JFileChooser fileChooser = new JFileChooser();
      int result = fileChooser.showOpenDialog(Frame.this);

      if(result != JFileChooser.APPROVE_OPTION) return;

      File file = fileChooser.getSelectedFile();
      Path filePath = file.toPath();
      if(!Files.isReadable(filePath)) {
        JOptionPane.showMessageDialog(Frame.this, "The file " + file.getAbsolutePath() + " doesn't exist.", "Error", JOptionPane.ERROR_MESSAGE);
        return;
      }
      loadDocument(filePath);
      savePath = filePath;
    }
  };
  private final Action save = new AbstractAction() {
    @Override
    public void actionPerformed(ActionEvent e) {
      if(savePath == null) {
        savePath = selectPath();
      }

      saveDocument(savePath);
    }
  };
  private final Action exit = new AbstractAction() {
    @Override
    public void actionPerformed(ActionEvent e) {
      dispose();
    }
  };
  private final Action moveToStart = new AbstractAction() {
    @Override
    public void actionPerformed(ActionEvent e) {
      model.moveCursor(new Location(0, 0));
      model.setSelectionRange(null);
    }
  };
  private final Action moveToEnd = new AbstractAction() {
    @Override
    public void actionPerformed(ActionEvent e) {
      List<String> lines = model.getLines();
      model.moveCursor(new Location(lines.size()-1, lines.get(lines.size()-1).length()));
      model.setSelectionRange(null);
    }
  };
  private final Action moveLeft = new AbstractAction() {
    @Override
    public void actionPerformed(ActionEvent e) {
      model.moveCursorLeft();
      model.setSelectionRange(null);
    }
  };
  private final Action moveRight = new AbstractAction() {
    @Override
    public void actionPerformed(ActionEvent e) {
      model.moveCursorRight();
      model.setSelectionRange(null);
    }
  };
  private final Action moveUp = new AbstractAction() {
    @Override
    public void actionPerformed(ActionEvent e) {
      model.moveCursorUp();
      model.setSelectionRange(null);
    }
  };
  private final Action moveDown = new AbstractAction() {
    @Override
    public void actionPerformed(ActionEvent e) {
      model.moveCursorDown();
      model.setSelectionRange(null);
    }
  };
  private final Action deleteBefore = new AbstractAction() {
    @Override
    public void actionPerformed(ActionEvent e) {
      if(model.getSelectionRange() != null) {
        model.deleteRange(model.getSelectionRange());
        model.setSelectionRange(null);
        return;
      }
      model.deleteBefore();
    }
  };
  private final Action deleteAfter = new AbstractAction() {
    @Override
    public void actionPerformed(ActionEvent e) {
      if(model.getSelectionRange() != null) {
        model.deleteRange(model.getSelectionRange());
        model.setSelectionRange(null);
        return;
      }
      model.deleteAfter();
    }
  };
  private final Action deleteSelection = new AbstractAction() {
    @Override
    public void actionPerformed(ActionEvent e) {
      if(model.getSelectionRange() == null) return;

      model.deleteRange(model.getSelectionRange());
      model.setSelectionRange(null);
    }
  };
  private final Action clear = new AbstractAction() {
    @Override
    public void actionPerformed(ActionEvent e) {
      List<String> lines = model.getLines();
      LocationRange range = new LocationRange(new Location(0,0), new Location(lines.size()-1, lines.get(lines.size()-1).length()));

      model.deleteRange(range);
      model.setSelectionRange(null);
    }
  };
  private final Action selectLeft = new AbstractAction() {
    @Override
    public void actionPerformed(ActionEvent e) {
      Location cursorStart = model.getCursorLocation();
      model.moveCursorLeft();
      Location cursorEnd = model.getCursorLocation();
      if(cursorStart.equals(cursorEnd)) return;
      LocationRange selection = model.getSelectionRange();
      if(selection == null) {
        model.setSelectionRange(new LocationRange(cursorEnd, cursorStart));
        return;
      }

      if(cursorEnd.compareTo(selection.getStart()) > 0) {
        model.setSelectionRange(new LocationRange(model.getSelectionRange().getStart(), cursorEnd));
      } else {
        model.setSelectionRange(new LocationRange(cursorEnd, model.getSelectionRange().getEnd()));
      }
    }
  };
  private final Action selectRight = new AbstractAction() {
    @Override
    public void actionPerformed(ActionEvent e) {
      Location cursorStart = model.getCursorLocation();
      model.moveCursorRight();
      Location cursorEnd = model.getCursorLocation();
      if(cursorStart.equals(cursorEnd)) return;
      LocationRange selection = model.getSelectionRange();
      if(selection == null) {
        model.setSelectionRange(new LocationRange(cursorStart, cursorEnd));
        return;
      }

      if(cursorEnd.compareTo(selection.getEnd()) < 0) {
        model.setSelectionRange(new LocationRange(cursorEnd, model.getSelectionRange().getEnd()));
      } else {
        model.setSelectionRange(new LocationRange(model.getSelectionRange().getStart(), cursorEnd));
      }
    }
  };
  private final Action selectUp = new AbstractAction() {
    @Override
    public void actionPerformed(ActionEvent e) {
      Location cursorStart = model.getCursorLocation();
      model.moveCursorUp();
      Location cursorEnd = model.getCursorLocation();
      if(cursorStart.equals(cursorEnd)) return;
      LocationRange selection = model.getSelectionRange();
      if(selection == null) {
        model.setSelectionRange(new LocationRange(cursorEnd, cursorStart));
        return;
      }

      if(cursorEnd.compareTo(selection.getStart()) > 0) {
        model.setSelectionRange(new LocationRange(selection.getStart(), cursorEnd));
      } else {
        if(selection.getStart().getRow() == cursorEnd.getRow()) {
          model.setSelectionRange(new LocationRange(cursorEnd, selection.getStart()));
        } else {
          model.setSelectionRange(new LocationRange(cursorEnd, selection.getEnd()));
        }
      }
    }
  };
  private final Action selectDown = new AbstractAction() {
    @Override
    public void actionPerformed(ActionEvent e) {
      Location cursorStart = model.getCursorLocation();
      model.moveCursorDown();
      Location cursorEnd = model.getCursorLocation();
      if(cursorStart.equals(cursorEnd)) return;
      LocationRange selection = model.getSelectionRange();
      if(selection == null) {
        model.setSelectionRange(new LocationRange(cursorStart, cursorEnd));
        return;
      }

      if(cursorEnd.compareTo(selection.getEnd()) > 0) {
        if(selection.getEnd().getRow() == cursorEnd.getRow()) {
          model.setSelectionRange(new LocationRange(selection.getEnd(), cursorEnd));
        } else {
          model.setSelectionRange(new LocationRange(selection.getStart(), cursorEnd));
        }
      } else {
        model.setSelectionRange(new LocationRange(cursorEnd, selection.getEnd()));
      }
    }
  };
  private final Action copy = new AbstractAction() {
    @Override
    public void actionPerformed(ActionEvent e) {
      if(model.getSelectionRange() == null) return;

      String selection = model.getTextRange(model.getSelectionRange());
      transferToClipboard(selection);
    }
  };
  private final Action cut = new AbstractAction() {
    @Override
    public void actionPerformed(ActionEvent e) {
      if(model.getSelectionRange() == null) return;

      String selection = model.getTextRange(model.getSelectionRange());
      transferToClipboard(selection);
      model.deleteRange(model.getSelectionRange());
      model.setSelectionRange(null);
    }
  };
  private final Action paste = new AbstractAction() {
    @Override
    public void actionPerformed(ActionEvent e) {
      String text = getFromClipboard(false);
      if(text == null) return;

      if(model.getSelectionRange() != null) {
        model.deleteRange(model.getSelectionRange());
        model.setSelectionRange(null);
      }
      model.insert(text);
    }
  };
  private final Action pasteRemove = new AbstractAction() {
    @Override
    public void actionPerformed(ActionEvent e) {
      String text = getFromClipboard(true);
      if(text == null) return;

      if(model.getSelectionRange() != null) {
        model.deleteRange(model.getSelectionRange());
        model.setSelectionRange(null);
      }
      model.insert(text);
    }
  };
  private final Action undo = new AbstractAction() {
    @Override
    public void actionPerformed(ActionEvent e) {
      UndoManager.instance().undo();
    }
  };
  private final Action redo = new AbstractAction() {
    @Override
    public void actionPerformed(ActionEvent e) {
      UndoManager.instance().redo();
    }
  };

  public Frame(TextEditorModel model) {
    if(model == null) throw new IllegalArgumentException("Model can't be null.");

    this.model = model;
    this.savePath = null;
    this.ctrlPressed = false;
    this.dragStartLocation = null;
    this.clickCount = 0;
    this.clipboard = new ClipboardStack();

    setTitle("TextEditor");
    setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    setPreferredSize(new Dimension(1000, 600));

    initGUI();
    
    pack();
    setLocationRelativeTo(null);
  }

  private void initGUI() {
    JMenuBar menuBar = new JMenuBar();
    this.setJMenuBar(menuBar);

    JMenu fileMenu = new JMenu("File");
    fileMenu.add(new JMenuItem(open));
    fileMenu.add(new JMenuItem(save));
    fileMenu.addSeparator();
    fileMenu.add(new JMenuItem(exit));
    menuBar.add(fileMenu);

    JMenu editMenu = new JMenu("Edit");
    editMenu.add(new JMenuItem(undo));
    editMenu.add(new JMenuItem(redo));
    editMenu.addSeparator();
    editMenu.add(new JMenuItem(cut));
    editMenu.add(new JMenuItem(copy));
    editMenu.add(new JMenuItem(paste));
    editMenu.add(new JMenuItem(pasteRemove));
    editMenu.add(new JMenuItem(deleteSelection));
    editMenu.add(new JMenuItem(clear));
    menuBar.add(editMenu);

    JMenu moveMenu = new JMenu("Move");
    moveMenu.add(new JMenuItem(moveToStart));
    moveMenu.add(new JMenuItem(moveToEnd));
    menuBar.add(moveMenu);

    JMenu plugins = new JMenu("Plugins");
    menuBar.add(plugins);
    initPlugins(plugins);

    Container cp = this.getContentPane();
    cp.setLayout(new BorderLayout());
    TextEditor textEditor = new TextEditor(model);
    model.addCursorObserver(location -> textEditor.repaint());
    model.addTextObserver(textEditor::repaint);
    model.addSelectionObserver(range -> textEditor.repaint());

    cp.add(textEditor, BorderLayout.CENTER);

    JPanel statusBar = new JPanel();
    statusBar.setLayout(new GridLayout(1, 2));
    cp.add(statusBar, BorderLayout.PAGE_END);

    JLabel left = new JLabel(String.format("Row: %d, Col: %d", model.getCursorLocation().getRow()+1, model.getCursorLocation().getColumn()+1));
    JLabel right = new JLabel(String.format("Lines: %d", model.getLines().size()));
    statusBar.add(left);
    statusBar.add(right);
    statusBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.BLACK));

    model.addCursorObserver(new CursorObserver() {
      @Override
      public void updateCursorLocation(Location loc) {
        left.setText(String.format("Row: %d, Col: %d", loc.getRow()+1, loc.getColumn()+1));
      }
    });
    model.addTextObserver(new TextObserver() {
      @Override
      public void updateText() {
        right.setText(String.format("Lines: %d", model.getLines().size()));
      }
    });

    initActions();
    initKeyRegistration(textEditor);
    initMouseRegistration(textEditor);
  }

  private void initActions() {
    open.putValue(
        Action.NAME,
        "Open"
    );
    open.putValue(
      Action.ACCELERATOR_KEY,
      KeyStroke.getKeyStroke("control O"));
    open.putValue(
      Action.MNEMONIC_KEY,
      KeyEvent.VK_O);
    open.setEnabled(true);

    save.putValue(
        Action.NAME,
        "Save"
    );
    save.putValue(
      Action.ACCELERATOR_KEY,
      KeyStroke.getKeyStroke("control S"));
    save.putValue(
      Action.MNEMONIC_KEY,
      KeyEvent.VK_S);
    save.setEnabled(true);

    exit.putValue(
        Action.NAME,
        "Exit"
    );
    exit.setEnabled(true);

    undo.putValue(
        Action.NAME,
        "Undo");
    undo.putValue(
        Action.ACCELERATOR_KEY,
        KeyStroke.getKeyStroke("control Z"));
    undo.putValue(
        Action.MNEMONIC_KEY,
        KeyEvent.VK_Z);
    undo.setEnabled(false);
    UndoManager.instance().addUndoListener(new StackStatusListener() {
      @Override
      public void statusChanged(boolean empty) {
        undo.setEnabled(!empty);
      }
    });

    redo.putValue(
        Action.NAME,
        "Redo");
    redo.putValue(
        Action.ACCELERATOR_KEY,
        KeyStroke.getKeyStroke("control Y"));
    redo.putValue(
        Action.MNEMONIC_KEY,
        KeyEvent.VK_Y);
    redo.setEnabled(false);
    UndoManager.instance().addRedoListener(new StackStatusListener() {
      @Override
      public void statusChanged(boolean empty) {
        redo.setEnabled(!empty);
      }
    });

    copy.putValue(
        Action.NAME,
        "Copy");
    copy.putValue(
        Action.ACCELERATOR_KEY,
        KeyStroke.getKeyStroke("control C"));
    copy.putValue(
        Action.MNEMONIC_KEY,
        KeyEvent.VK_C);
    copy.setEnabled(false);

    cut.putValue(
        Action.NAME,
        "Cut");
    cut.putValue(
        Action.ACCELERATOR_KEY,
        KeyStroke.getKeyStroke("control X"));
    cut.putValue(
        Action.MNEMONIC_KEY,
        KeyEvent.VK_X);
    cut.setEnabled(false);

    deleteBefore.putValue(
        Action.ACCELERATOR_KEY,
        KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0));
    deleteBefore.setEnabled(true);

    deleteAfter.putValue(
        Action.ACCELERATOR_KEY,
        KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
    deleteAfter.setEnabled(true);

    deleteSelection.putValue(
        Action.NAME,
        "Delete selection");
    deleteSelection.setEnabled(false);

    model.addSelectionObserver(new SelectionObserver() {
      @Override
      public void updateSelectionRange(LocationRange range) {
        boolean enabled = range != null;
        copy.setEnabled(enabled);
        cut.setEnabled(enabled);
        deleteSelection.setEnabled(enabled);
      }
    });

    paste.putValue(
        Action.NAME,
        "Paste");
    paste.putValue(
        Action.ACCELERATOR_KEY,
        KeyStroke.getKeyStroke("control V"));
    paste.putValue(
        Action.MNEMONIC_KEY,
        KeyEvent.VK_V);
    paste.setEnabled(false);

    pasteRemove.putValue(
        Action.NAME,
        "Paste and Take");
    pasteRemove.putValue(
        Action.ACCELERATOR_KEY,
        KeyStroke.getKeyStroke("control shift V"));
    pasteRemove.setEnabled(false);

    clipboard.addObserver(new ClipboardObserver() {
      @Override
      public void updateClipboard() {
        boolean enabled = !clipboard.isEmpty();
        paste.setEnabled(enabled);
        pasteRemove.setEnabled(enabled);
      }
    });

    clear.putValue(
        Action.NAME,
        "Clear document");
    clear.setEnabled(true);

    moveToStart.putValue(
        Action.NAME,
        "Cursor to document start");
    moveToStart.setEnabled(true);

    moveToEnd.putValue(
        Action.NAME,
        "Cursor to document end");
    moveToEnd.setEnabled(true);

    moveLeft.putValue(
        Action.ACCELERATOR_KEY,
        KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0));
    moveLeft.setEnabled(true);
    moveRight.putValue(
        Action.ACCELERATOR_KEY,
        KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0));
    moveRight.setEnabled(true);
    moveUp.putValue(
        Action.ACCELERATOR_KEY,
        KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0));
    moveUp.setEnabled(true);
    moveDown.putValue(
        Action.ACCELERATOR_KEY,
        KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0));
    moveDown.setEnabled(true);

    selectLeft.putValue(
        Action.ACCELERATOR_KEY,
        KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, InputEvent.SHIFT_DOWN_MASK));
    selectLeft.setEnabled(true);
    selectRight.putValue(
        Action.ACCELERATOR_KEY,
        KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, InputEvent.SHIFT_DOWN_MASK));
    selectRight.setEnabled(true);
    selectUp.putValue(
        Action.ACCELERATOR_KEY,
        KeyStroke.getKeyStroke(KeyEvent.VK_UP, InputEvent.SHIFT_DOWN_MASK));
    selectUp.setEnabled(true);
    selectDown.putValue(
        Action.ACCELERATOR_KEY,
        KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, InputEvent.SHIFT_DOWN_MASK));
    selectDown.setEnabled(true);
  }

  private void initKeyRegistration(TextEditor editor) {
    editor.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_CONTROL, InputEvent.CTRL_DOWN_MASK, false), "ctrlOn");
    editor.getActionMap().put("ctrlOn", ctrlOn);
    editor.getInputMap().put(KeyStroke.getKeyStroke("released CONTROL"), "ctrlOff");
    editor.getActionMap().put("ctrlOff", ctrlOff);
    editor.getInputMap().put((KeyStroke) deleteBefore.getValue(Action.ACCELERATOR_KEY), "deleteBefore");
    editor.getActionMap().put("deleteBefore", deleteBefore);
    editor.getInputMap().put((KeyStroke) deleteAfter.getValue(Action.ACCELERATOR_KEY), "deleteAfter");
    editor.getActionMap().put("deleteAfter", deleteAfter);
    editor.getInputMap().put((KeyStroke) moveLeft.getValue(Action.ACCELERATOR_KEY), "moveLeft");
    editor.getActionMap().put("moveLeft", moveLeft);
    editor.getInputMap().put((KeyStroke) moveRight.getValue(Action.ACCELERATOR_KEY), "moveRight");
    editor.getActionMap().put("moveRight", moveRight);
    editor.getInputMap().put((KeyStroke) moveUp.getValue(Action.ACCELERATOR_KEY), "moveUp");
    editor.getActionMap().put("moveUp", moveUp);
    editor.getInputMap().put((KeyStroke) moveDown.getValue(Action.ACCELERATOR_KEY), "moveDown");
    editor.getActionMap().put("moveDown", moveDown);
    editor.getInputMap().put((KeyStroke) selectLeft.getValue(Action.ACCELERATOR_KEY), "selectLeft");
    editor.getActionMap().put("selectLeft", selectLeft);
    editor.getInputMap().put((KeyStroke) selectRight.getValue(Action.ACCELERATOR_KEY), "selectRight");
    editor.getActionMap().put("selectRight", selectRight);
    editor.getInputMap().put((KeyStroke) selectUp.getValue(Action.ACCELERATOR_KEY), "selectUp");
    editor.getActionMap().put("selectUp", selectUp);
    editor.getInputMap().put((KeyStroke) selectDown.getValue(Action.ACCELERATOR_KEY), "selectDown");
    editor.getActionMap().put("selectDown", selectDown);


    editor.addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        if(!ctrlPressed
            && e.getKeyChar() != KeyEvent.CHAR_UNDEFINED
            && !Arrays.asList(editor.getInputMap().keys()).contains(KeyStroke.getKeyStroke(e.getKeyCode(), 0))
        ) {
          if(model.getSelectionRange() != null) {
            model.deleteRange(model.getSelectionRange());
            model.setSelectionRange(null);
          }
          model.insert(e.getKeyChar());
        }
      }
    });
  }

  private void initMouseRegistration(TextEditor editor) {
    editor.addMouseMotionListener(new MouseAdapter() {
      @Override
      public void mouseDragged(MouseEvent e) {
        Location newCursorLocation = editor.getPointerDocumentLocation(e.getX(), e.getY());
        if(dragStartLocation == null) dragStartLocation = editor.getPointerDocumentLocation(e.getX(), e.getY());

        if(newCursorLocation.equals(model.getCursorLocation())) return;

        model.setSelectionRange(new LocationRange(dragStartLocation, newCursorLocation));
        model.moveCursor(newCursorLocation);
      }

      @Override
      public void mouseMoved(MouseEvent e) {
        clickCount = 0;
      }
    });

    editor.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseEntered(MouseEvent e) {
        setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
      }

      @Override
      public void mouseExited(MouseEvent e) {
        setCursor(Cursor.getDefaultCursor());
      }

      @Override
      public void mousePressed(MouseEvent e) {
        clickCount++;
        model.setSelectionRange(null);
        dragStartLocation = null;
        model.moveCursor(editor.getPointerDocumentLocation(e.getX(), e.getY()));

        if(clickCount == 2) {
          Location mouseLocation = editor.getPointerDocumentLocation(e.getX(), e.getY());
          String line = model.getLines().get(mouseLocation.getRow());
          if(line.length() == 0) return;
          int endX, startX;
          startX = endX = Math.max(Math.min(mouseLocation.getColumn(), line.length()-1), 0);
          if(Character.isLetterOrDigit(line.charAt(startX))){
            while(startX > 0 && Character.isLetterOrDigit(line.charAt(startX-1))) {
              startX--;
            }
            while(endX < line.length() && Character.isLetterOrDigit(line.charAt(endX))) {
              endX++;
            }
          } else if(Character.isWhitespace(line.charAt(startX))) {
            while(startX > 0 && Character.isWhitespace(line.charAt(startX-1))) {
              startX--;
            }
            while(endX < line.length() && Character.isWhitespace(line.charAt(endX))) {
              endX++;
            }
          } else {
            endX++;
          }
          model.setSelectionRange(new LocationRange(new Location(mouseLocation.getRow(), startX), new Location(mouseLocation.getRow(), endX)));
          model.moveCursor(new Location(mouseLocation.getRow(), endX));
          return;
        }

        if(clickCount == 3) {
          clickCount = 0;
          Location mouseLocation = editor.getPointerDocumentLocation(e.getX(), e.getY());
          String line = model.getLines().get(mouseLocation.getRow());
          model.setSelectionRange(new LocationRange(
              new Location(mouseLocation.getRow(), 0),
              mouseLocation.getRow() < model.getLines().size()-1 ? new Location(mouseLocation.getRow()+1, 0) :  new Location(mouseLocation.getRow(), line.length()))
          );
          model.moveCursor(new Location(mouseLocation.getRow(), line.length()));
        }
      }
    });
  }

  private void initPlugins(JMenu menu) {
    List<Plugin> plugins = loadPlugins();

    if(plugins.isEmpty()) menu.setEnabled(false);

    for(Plugin plugin : plugins) {
      Action a = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
          plugin.execute(model, UndoManager.instance(), clipboard);
        }
      };
      a.putValue(Action.NAME, plugin.getName());
      a.putValue(Action.SHORT_DESCRIPTION, plugin.getDescription());
      menu.add(new JMenuItem(a));
    }
  }

  private void transferToClipboard(String content) {
    if(content == null) return;

//    StringSelection stringSelection = new StringSelection(content);
//    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
//    clipboard.setContents(stringSelection, null);
    clipboard.push(content);
  }

  private String getFromClipboard(boolean remove) {
//    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
//    String text;
//    try {
//      text = (String) clipboard.getData(DataFlavor.stringFlavor);
//    } catch(UnsupportedFlavorException | IOException ex) {
//      ex.printStackTrace();
//      return null;
//    }
//
//    return text;
    if(!clipboard.isEmpty()) return remove ? clipboard.pop() : clipboard.peek();

    return null;
  }

  private void loadDocument(Path path) {
    try(Scanner scanner = new Scanner(path, StandardCharsets.UTF_8)) {
      StringBuilder builder = new StringBuilder();
      while(scanner.hasNextLine()) {
        builder.append(scanner.nextLine());
        if(scanner.hasNextLine()) builder.append("\n");
      }

      List<String> lines = model.getLines();
      model.deleteRange(new LocationRange(new Location(0, 0), new Location(lines.size()-1, lines.get(lines.size()-1).length())));
      model.insert(builder.toString());
    } catch(IOException e) {
      JOptionPane.showMessageDialog(this, "Couldn't read selected file.", "Reading error", JOptionPane.ERROR_MESSAGE);
    }
  }

  private void saveDocument(Path path) {
    try(FileOutputStream output = new FileOutputStream(path.toFile(), false)) {
      Iterator<String> iter = model.allLines();
      while(iter.hasNext()) {
        String line = iter.next();
        output.write((iter.hasNext() ? line.concat("\n") : line).getBytes(StandardCharsets.UTF_8));
      }
    } catch(IOException e) {
      JOptionPane.showMessageDialog(this, "Couldn't save to selected file.", "Saving error", JOptionPane.ERROR_MESSAGE);
    }
  }

  private Path selectPath() {
    JFileChooser fileChooser = new JFileChooser();
    fileChooser.setSelectedFile(savePath == null ? null : savePath.toFile());
    int result = fileChooser.showSaveDialog(this);

    if(result != JFileChooser.APPROVE_OPTION) return null;

    File file = fileChooser.getSelectedFile();

    if(file.exists()) {
      String[] options = new String[] {"Yes", "No"};
      result = JOptionPane.showOptionDialog(
          this,
          "File already exists. Do you want to overwrite it?",
          "Overwrite file",
          JOptionPane.YES_NO_OPTION,
          JOptionPane.WARNING_MESSAGE,
          null,
          options,
          options[0]);
      if(result != JOptionPane.OK_OPTION)
        return null;
    }
    return file.toPath();
  }

  private List<Plugin> loadPlugins() {
    List<Plugin> plugins = new ArrayList<>();
    ClassLoader parent = this.getClass().getClassLoader();
    File[] pluginFiles = pluginFolder.toFile().listFiles();
    if(pluginFiles != null) {
      for(File plugin : pluginFiles) {
        try {
          URLClassLoader newClassLoader = new URLClassLoader(
              new URL[] {
                  // Dodaj jedan direktorij (završava s /)
                  pluginFolder.toFile().toURI().toURL(),
                  // Dodaj jedan konkretan JAR (ne završava s /)
                  plugin.toURI().toURL()
              }, parent);
          String name = plugin.getName().split("\\.")[0];
          Class<Plugin> clazz = (Class<Plugin>) newClassLoader.loadClass("plugins."+name);
          plugins.add(clazz.getConstructor().newInstance());
        } catch(Exception e) {
          e.printStackTrace();
        }
      }
    }
    return plugins;
  }
}
