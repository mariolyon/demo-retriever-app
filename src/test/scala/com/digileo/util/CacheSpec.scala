package com.digileo.util

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class CacheSpec extends AnyWordSpecLike with Matchers {
  "Cache" should {
    "start with an empty cache" in {
      val cache = new Cache()
      cache.get(0) should equal(None)
    }

    "return None if an item is not in the cache" in {
      val cache = new Cache()
      cache.add(List('A', 'B'))
      cache.get(2) should equal(None)
    }

    "return the wrapped value if an item is in the cache" in {
      val cache = new Cache()
      cache.add(List('A', 'B'))
      cache.get(1) should equal(Some('B'))
    }

    "mutate it's internal state with successive calls to its add method" in {
      val cache = new Cache()
      cache.add(List('A', 'B'))
      cache.add(List('C', 'D'))
      cache.get(3) should equal(Some('D'))
    }

    "the internal state should be compressed" in {
      val cache = new Cache()
      cache.add(List('A', 'A'))
      cache.add(List('A', 'D'))
      cache.get(2) should equal(Some('A'))
      cache.get(3) should equal(Some('D'))
    }
  }
}
