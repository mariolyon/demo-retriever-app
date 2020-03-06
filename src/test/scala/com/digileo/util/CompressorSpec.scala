package com.digileo.util

import com.digileo.Value
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class CompressorSpec extends AnyWordSpecLike with Matchers with Compressor[Value] {
  "Compressor" should {
    "compress a list of values" in {
      val input = List('A', 'A', 'A', 'B', 'C', 'C', 'D', 'A')
      val expected = List(Repeat(3, 'A'), Repeat(1, 'B'), Repeat(2, 'C'), Repeat(1, 'D'),  Repeat(1, 'A'))

      val result = compress(input)
      result should equal(expected)
    }

    "expand a list of values" in {
      val input = List(Repeat(3, 'A'), Repeat(1, 'B'), Repeat(2, 'C'), Repeat(1, 'D'),  Repeat(1, 'A'))
      val expected = List('A', 'A', 'A', 'B', 'C', 'C', 'D', 'A')

      val result = input.expand
      result should equal(expected)
    }

    "get element at index" in {
      val expected =
        List('A', 'A', 'A', 'B', 'C', 'C', 'D', 'A').map(Some(_))

      val compressed = List(Repeat(3, 'A'), Repeat(1, 'B'), Repeat(2, 'C'), Repeat(1, 'D'),  Repeat(1, 'A'))

      val result = (0 until expected.size).map(getItem(_, compressed))
      expected should equal(result)
    }

    "join two compressed sequences" in {
      val compressed0 = List(Repeat(3, 'A'), Repeat(1, 'B'), Repeat(1, 'C'))
      val compressed1 = List(Repeat(1, 'C'), Repeat(1, 'D'),  Repeat(1, 'A'))

      val expected  = List(Repeat(3, 'A'), Repeat(1, 'B'), Repeat(2, 'C'), Repeat(1, 'D'),  Repeat(1, 'A'))

      val result = compressed0.join(compressed1)
      expected should equal(result)
    }
  }
}
