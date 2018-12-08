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

  def findAllBy(id: String,
                pageNo: Int,
                pageSize: Int): Either[RepositoryError, domain.Page[Person] with Object {
    val data: Seq[Person]

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
        .withKeyConditionExpression(s"$AttrId = :id") // TODO
        .withExpressionAttributeValues(
        Map(":id" -> new AttributeValue().withS(id)).asJava)

      lazy val totalSize = dynamoDBClient.query(countQueryRequest).getCount
      // TODO リファクタリング
      val querySpec = new QuerySpec()
        .withHashKey(AttrId, id)
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
        val data = page.asScala.flatMap(item => item2Person(Option(item))).toSeq
        PagePersonView.create(
          totalSize = totalSize,
          pageNo = pageNo,
          pageSize = pageSize,
          lastPageNo = lastPageNo,
          data = data
        )
      } else if (pageNo == 1) {
        PagePersonView.create(
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

