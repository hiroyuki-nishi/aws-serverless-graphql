package dynamodb

import domain.{Page, RepositoryError}

trait AccountRepository {
  def findBy(personId: String, email: Email): Either[RepositoryError, Option[Account]]
  def findAllBy(personId: String,
                pageNo: Int,
                pageSize: Int): Either[RepositoryError, Page[Account]]
}

