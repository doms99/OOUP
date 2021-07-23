package model;

import java.util.Objects;

public class Location implements Comparable<Location> {
  private final int row;
  private final int column;

  public Location(int row, int column) {
    if(row < 0 || column < 0) throw new IllegalArgumentException("Row or column can't be smaller then 0. ("+ row +", "+ column +")");

    this.row = row;
    this.column = column;
  }

  public int getRow() {
    return row;
  }

  public int getColumn() {
    return column;
  }

  @Override
  public boolean equals(Object o) {
    if(this == o) return true;
    if(o == null || getClass() != o.getClass()) return false;
    Location location = (Location) o;
    return row == location.row &&
        column == location.column;
  }

  @Override
  public int hashCode() {
    return Objects.hash(row, column);
  }

  @Override
  public int compareTo(Location o) {
    if(this.row == o.row) return Integer.compare(this.column, o.column);

    return Integer.compare(this.row, o.row);
  }
}
