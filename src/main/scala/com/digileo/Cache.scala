package com.digileo

class Cache {
  private var values = List.empty[Value]

  def add(newValues: List[Value]): Unit = values ++= newValues

  def contains(index: Int): Boolean = index < values.size

  def get(index: Int): Option[Value] =
    if (contains(index))
      Some(values(index))
    else
      None

}
