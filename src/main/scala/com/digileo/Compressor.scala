package com.digileo

case class Repeat[A](count: Int, element: A) {
  def expand: List[A] = List.fill(count)(element)

  def join(other: Repeat[A]) = {
    require(element == other.element)
    Repeat(count + other.count, element)
  }
}

case class Compressed[A](val elems: List[Repeat[A]]) {
  val count = elems.map(_.count).sum
}

trait Compressor[A] {
  def compress[A](values: List[A]): Compressed[A] = {
    val elems =
      values.foldRight(List.empty[Repeat[A]])(
        (value, acc) => acc match {
          case Nil => List(Repeat(1, value))
          case _ if acc.head.element == value => Repeat(acc.head.count + 1, value) :: acc.tail
          case _ => Repeat(1, value) :: acc
        })
    Compressed(elems)
  }

  def expand[A](compressed: Compressed[A]): List[A] =
    compressed.elems.foldLeft(List.empty[A])((acc, repeat) => acc ++ repeat.expand)

  def getItem[A](index: Int, compressed: Compressed[A]): Option[A] = {
    index match {
      case i if i >= compressed.count => None
      case i if i < compressed.elems.head.count => Some(compressed.elems.head.element)
      case i => getItem(i - compressed.elems.head.count, Compressed(compressed.elems.tail))
    }
  }

  def join[A](compressed0: Compressed[A], compressed1: Compressed[A]): Compressed[A] = {
    (compressed0.elems, compressed1.elems) match {
      case (Nil, Nil) => Compressed(Nil)
      case (Nil, elems) => Compressed(elems)
      case (elems, Nil) => Compressed(elems)
      case (elems0, elems1) if elems0.last.element == elems1.head.element => {
        val joined = elems0.last.join(elems1.head)
        Compressed(elems0.init ++ (joined :: elems1.tail))
      }
      case (elems0, elems1) => Compressed(elems0 ++ elems1)
    }
  }
}
