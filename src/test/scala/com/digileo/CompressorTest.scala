package com.digileo

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class CompressorTest extends AnyWordSpecLike with Matchers {
  val compressor = new Compressor[Value] {}


  "Compressor" should {
    "compress a list of values" in {
      val input = List('A', 'A', 'A', 'B', 'C', 'C', 'D', 'A')
      val expected = List(Repeat(3, 'A'), Repeat(1, 'B'), Repeat(2, 'C'), Repeat(1, 'D'),  Repeat(1, 'A'))

      val result = compressor.compress(input)
      result.elems should equal(expected)
    }

    "expand a list of values" in {
      val input = List(Repeat(3, 'A'), Repeat(1, 'B'), Repeat(2, 'C'), Repeat(1, 'D'),  Repeat(1, 'A'))
      val expected = List('A', 'A', 'A', 'B', 'C', 'C', 'D', 'A')

      val result = compressor.expand(Compressed(input))
      result should equal(expected)
    }

    "get element at index" in {
      val expected =
        List('A', 'A', 'A', 'B', 'C', 'C', 'D', 'A').map(Some(_))

      val compressed = Compressed(List(Repeat(3, 'A'), Repeat(1, 'B'), Repeat(2, 'C'), Repeat(1, 'D'),  Repeat(1, 'A')))

      val result = (0 until expected.size).map(compressor.getItem(_, compressed))
      expected should equal(result)
    }

    "join two compressed sequences" in {
      val compressed0 = Compressed(List(Repeat(3, 'A'), Repeat(1, 'B'), Repeat(1, 'C')))
      val compressed1 = Compressed(List(Repeat(1, 'C'), Repeat(1, 'D'),  Repeat(1, 'A')))

      val expected  = Compressed(List(Repeat(3, 'A'), Repeat(1, 'B'), Repeat(2, 'C'), Repeat(1, 'D'),  Repeat(1, 'A')))

      val result = compressor.join(compressed0, compressed1)
      expected should equal(result)
    }
  }
}
