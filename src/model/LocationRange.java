package model;

import java.util.Objects;

public class LocationRange {
  private Location start;
  private Location end;

  public LocationRange(Location start, Location end) {
    if(start == null || end == null) throw new IllegalArgumentException("Start or end location can't be null.");

    if(start.compareTo(end) > 0) {
      this.end = start;
      this.start = end;
      return;
    }

    this.start = start;
    this.end = end;
  }

  public Location getStart() {
    return start;
  }

  public void setStart(Location start) {
    if(start == null) throw new IllegalArgumentException("Start location can't be null.");
    this.start = start;
  }

  public Location getEnd() {
    return end;
  }

  public void setEnd(Location end) {
    if(end == null) throw new IllegalArgumentException("End location can't be null.");
    this.end = end;
  }

  @Override
  public boolean equals(Object o) {
    if(this == o) return true;
    if(o == null || getClass() != o.getClass()) return false;
    LocationRange that = (LocationRange) o;
    return Objects.equals(start, that.start) &&
        Objects.equals(end, that.end);
  }

  @Override
  public int hashCode() {
    return Objects.hash(start, end);
  }
}
