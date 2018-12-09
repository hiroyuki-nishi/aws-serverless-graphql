package person

import domain.Page

case class Person(id: String, name: String)

object PagePersonView {
  def create(totalSize: Int,
             pageNo: Int,
             pageSize: Int,
             lastPageNo: Int,
             data: Seq[Person]) = {
    val (a, b, c, d, f) = (
      totalSize,
      pageNo,
      pageSize,
      lastPageNo,
      data
    )
    new Page[Person] {
      override val totalSize  = a
      override val pageNo     = b
      override val pageSize   = c
      override val lastPageNo = d
      override val data       = f
    }
  }
}
