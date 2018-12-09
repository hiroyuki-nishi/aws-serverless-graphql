package person

import domain.{Page, RepositoryError}

trait PersonRepository {
  def findBy(id: String, name: String): Either[RepositoryError, Option[Person]]
  def findAllBy(id: String,
                pageNo: Int,
                pageSize: Int): Either[RepositoryError, Page[Person]]
}
