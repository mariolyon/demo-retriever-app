package com.digileo

case class Repeat[A](count: Int, element: A) {
  def expand: List[A] = List.fill(count)(element)

  def getItem(index: Int): Option[A] =
    if (index < count)
      Some(element)
    else
      None

  def join(other: Repeat[A]): Repeat[A] = {
    require(element == other.element)
    Repeat(count + other.count, element)
  }
}

trait Compressor[A] {
  type Compressed[A] = List[Repeat[A]]

  def getItem(index: Int, repeat: Repeat[A]):Option[A] = repeat.getItem(index)

  def getItem(index: Int, compressed: Compressed[A]):Option[A] ={
    index match {
      case i if compressed.size == 1 && i >= compressed.head.count => None
      case i if compressed.isEmpty => None
      case i if i < compressed.head.count => Some(compressed.head.element)
      case i => getItem(i - compressed.head.count, compressed.tail)
    }
  }

  implicit class CompressedOps[A](elems: Compressed[A]) {
    def join(otherElems: List[Repeat[A]]): List[Repeat[A]] = {
      (elems, otherElems) match {
        case (Nil, Nil) => Nil
        case (Nil, elems) => elems
        case (elems, Nil) => elems
        case (elems0, elems1) if elems0.last.element == elems1.head.element => {
          val joined = elems0.last.join(elems1.head)
          elems0.init ++ (joined :: elems1.tail)
        }
        case (elems0, elems1) => elems0 ++ elems1
      }
    }

    def expand: List[A] = elems.foldLeft(List.empty[A])((acc, repeat) => acc ++ repeat.expand)

    def count() = elems.map(_.count).sum
  }

  def compress[A](values: List[A]): Compressed[A] =
    values.foldRight(List.empty[Repeat[A]])(
      (value, acc) => acc match {
        case Nil => List(Repeat(1, value))
        case _ if acc.head.element == value => Repeat(acc.head.count + 1, value) :: acc.tail
        case _ => Repeat(1, value) :: acc
      })
}
