package observers;

import model.LocationRange;

public interface SelectionObserver {
  void updateSelectionRange(LocationRange range);
}
