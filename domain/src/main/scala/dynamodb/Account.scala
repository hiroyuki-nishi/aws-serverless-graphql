package dynamodb

import domain.Page

case class Email(value: String)
case class Account(personId: String, email: String)

object PageAccountView {
  def create(totalSize: Int,
             pageNo: Int,
             pageSize: Int,
             lastPageNo: Int,
             data: Seq[Account]) = {
    val (a, b, c, d, f) = (
      totalSize,
      pageNo,
      pageSize,
      lastPageNo,
      data
    )
    new Page[Account] {
      override val totalSize  = a
      override val pageNo     = b
      override val pageSize   = c
      override val lastPageNo = d
      //      override val order      = e
      override val data       = f
    }
  }
}


