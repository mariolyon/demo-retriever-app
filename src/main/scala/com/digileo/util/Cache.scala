package com.digileo.util

import com.digileo.Value

class Cache extends Compressor[Value] {
  private var compressed: Compressed[Value] = List.empty[Repeat[Value]]

  def add(newValues: List[Value]): Unit =
    compressed = compressed.join( compress(newValues))

  def contains(index: Int): Boolean =
    index < compressed.count()

  def get(index: Int): Option[Value] = getItem(index, compressed)
}
