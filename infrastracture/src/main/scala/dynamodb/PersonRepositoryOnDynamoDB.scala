package dynamodb

import com.amazonaws.services.dynamodbv2.document._
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec
import com.amazonaws.services.dynamodbv2.model.{AttributeValue, QueryRequest, Select}
import domain.RepositoryError

import scala.collection.JavaConverters._
import scala.util.Try


trait PersonRepositoryOnDynamoDB extends DynamoDBWrapper {
  lazy override val tableName  = "persons"
  lazy override val regionName = "ap-northeast-1"

  val AttrId         = "id"
  val AttrName       = "name"
  val IndexIdWithName = s"${AttrId}-${AttrName}"

  private def item2Person(itemOpt: Option[Item]): Option[Person] =
    itemOpt.map(item => Person(item.getString(AttrId), item.getString(AttrName)))

  private def record2Entity(item: Item): Try[Option[Person]] = Try(item2Person(Option(item)))

  def findBy(id: String, name: String): Either[RepositoryError, Option[Person]] =
    getItem(AttrId, id, AttrName, name)(record2Entity)
      .fold(
        e => Left(RepositoryError()),
        Right(_)
      )
}

