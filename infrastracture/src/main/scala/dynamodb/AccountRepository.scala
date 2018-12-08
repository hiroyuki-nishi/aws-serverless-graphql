package dynamodb

import com.amazonaws.services.dynamodbv2.document.Item
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec
import com.amazonaws.services.dynamodbv2.model.{AttributeValue, QueryRequest, Select}
import domain.RepositoryError

import scala.collection.JavaConverters._
import scala.util.Try

trait AccountRepositoryOnDynamoDB extends DynamoDBWrapper {
  lazy override val tableName = "accounts"
  lazy override val regionName = "ap-northeast-1"

  val AttrPersonId = "person_id"
  val AttrEmail = "email"
  val IndexIdWithName = s"${AttrPersonId}-${AttrEmail}"

  private def item2Account(itemOpt: Option[Item]): Option[Account] =
    itemOpt.map(item => Account(item.getString(AttrPersonId), item.getString(AttrEmail)))

  private def record2Entity(item: Item): Try[Option[Account]] = Try(item2Account(Option(item)))

  def findBy(personId: String, email: Email): Either[RepositoryError, Option[Account]] =
    getItem(AttrPersonId, personId, AttrEmail, email.value)(record2Entity)
      .fold(
        e => Left(RepositoryError()),
        Right(_)
      )

  def findAllBy(id: String,
                pageNo: Int,
                pageSize: Int): Either[RepositoryError, domain.Page[Account] with Object {
    val data: Seq[Account]
    val pageSize: Int
    val totalSize: Int
    val lastPageNo: Int
    val pageNo: Int
  }] =
    Try {
      // TODO QueryRequestの理解
      val countQueryRequest: QueryRequest = new QueryRequest(tableName)
        .withSelect(Select.COUNT)
        .withIndexName(IndexIdWithName) // TODO
        .withKeyConditionExpression(s"$AttrPersonId = :id") //TODO
        .withExpressionAttributeValues(Map(":id" -> new AttributeValue().withS(id)).asJava) //TODO

      lazy val totalSize = dynamoDBClient.query(countQueryRequest).getCount
      // TODO リファクタリング
      val querySpec = new QuerySpec()
        .withHashKey(AttrPersonId, id)
        .withScanIndexForward(false)
        .withMaxPageSize(pageSize)

      val table         = getTable(tableName).get
      val index         = table.getIndex(IndexIdWithName) //TODO
      val queryOutcomes = index.query(querySpec)

      lazy val pages = queryOutcomes.pages().asScala
      lazy val lastPageNo = (BigDecimal(totalSize) / BigDecimal(pageSize))
        .setScale(0, scala.math.BigDecimal.RoundingMode.CEILING)
        .toInt

      if (pageNo <= lastPageNo) {
        val page = pages.take(pageNo).last
        val data = page.asScala.flatMap(item => item2Account(Option(item))).toSeq
        PageAccountView.create(
          totalSize = totalSize,
          pageNo = pageNo,
          pageSize = pageSize,
          lastPageNo = lastPageNo,
          data = data
        )
      } else if (pageNo == 1) {
        PageAccountView.create(
          totalSize = totalSize,
          pageNo = pageNo,
          pageSize = pageSize,
          lastPageNo = lastPageNo,
          data = Seq.empty
        )
      } else {
        throw new IndexOutOfBoundsException(s"$pageNo")
      }
    }.fold({
      case _: IndexOutOfBoundsException => Left(RepositoryError())
    },
      Right(_)
    )
}

