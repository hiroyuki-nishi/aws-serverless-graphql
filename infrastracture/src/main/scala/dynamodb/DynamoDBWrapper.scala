package dynamodb

import com.amazonaws.regions.{Region, Regions}
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.amazonaws.services.dynamodbv2.document._
import com.amazonaws.services.dynamodbv2.document.internal.IteratorSupport
import com.amazonaws.services.dynamodbv2.document.spec.{QuerySpec, UpdateItemSpec}
import com.amazonaws.services.dynamodbv2.document.utils.{NameMap, ValueMap}
import com.amazonaws.services.dynamodbv2.model.{AttributeValue, ConditionalCheckFailedException, QueryRequest}

import scala.annotation.tailrec
import scala.collection.JavaConverters._
import scala.util.Try

trait DynamoDBWrapper {
  protected val tableName: String
  protected val regionName: String

  lazy val dynamoDBClient = {
    val client = new AmazonDynamoDBClient()
    client.setRegion(Region.getRegion(Regions.fromName(regionName)))
    client
  }

  lazy private val dynamoDB = new DynamoDB(dynamoDBClient)

  protected def getTable(name: String = tableName) = Try {
    dynamoDB.getTable(name)
  }

  protected def put(item: Item): Try[PutItemOutcome] =
    for {
      t <- getTable()
      r <- Try(t.putItem(item))
    } yield r

  protected def put(item: Item, conditions: String, nameMap: NameMap, valueMap: ValueMap) = Try {
    (for {
      t <- getTable()
      r <- Try(t.putItem(item, conditions, nameMap, valueMap))
    } yield r).fold(
      {
        case o: ConditionalCheckFailedException => throw o
        //        case o: ConditionalCheckFailedException => throw OptimisticLockingException(o)
        case t                                  => throw t
      },
      l => l
    )
  }

  protected def getItem[E](hashKeyName: String, hashKeyValue: String)(
    record2Entity: Item => Try[E]) =
    for {
      t <- getTable()
      i <- Try(t.getItem(hashKeyName, hashKeyValue))
      e <- record2Entity(i)
    } yield e

  protected def getItem[E](hashKeyName: String,
                           hashKeyValue: String,
                           rangeKeyName: String,
                           rangeKeyValue: String)(record2Entity: Item => Try[E]) =
    for {
      t <- getTable()
      i <- Try(t.getItem(hashKeyName, hashKeyValue, rangeKeyName, rangeKeyValue))
      e <- record2Entity(i)
    } yield e

  protected def getWithIndex[E](indexName: String, hashKeyName: String, hashKeyValue: String)(
    iterator2Entity: IteratorSupport[Item, QueryOutcome] => Try[E]) =
    for {
      t     <- getTable()
      index <- Try(t.getIndex(indexName))
      it    <- Try(index.query(hashKeyName, hashKeyValue).iterator())
      r     <- iterator2Entity(it)
    } yield r

  protected def getWithIndex[E](
                                 indexName: String,
                                 hashKeyName: String,
                                 hashKeyValue: String,
                                 rangeKeyName: String,
                                 rangeKeyValue: String)(iterator2Entity: IteratorSupport[Item, QueryOutcome] => Try[E]) = {
    val rangeKeyCondition: RangeKeyCondition =
      new RangeKeyCondition(rangeKeyName).eq(rangeKeyValue)
    for {
      t     <- getTable()
      index <- Try(t.getIndex(indexName))
      it    <- Try(index.query(hashKeyName, hashKeyValue, rangeKeyCondition).iterator())
      r     <- iterator2Entity(it)
    } yield r
  }

  protected def query(hashKeyName: String, hashKeyValue: String): Try[Iterator[Item]] =
    for {
      t  <- getTable()
      it <- Try(t.query(hashKeyName, hashKeyValue).iterator())
    } yield it.asScala

  protected def queryWithIndex(indexName: String,
                               hashKeyName: String,
                               hashKeyValue: String): Try[Iterator[Item]] =
    for {
      t   <- getTable()
      idx <- Try(t.getIndex(indexName))
      it  <- Try(idx.query(hashKeyName, hashKeyValue).iterator())
    } yield it.asScala

  protected def queryWithIndexFilters(indexName: String,
                                      hashKeyName: String,
                                      hashKeyValue: String,
                                      filter: QuerySpec => QuerySpec): Try[Iterator[Item]] = {
    val spec = filter(
      new QuerySpec()
        .withHashKey(hashKeyName, hashKeyValue)
    )
    for {
      t   <- getTable()
      idx <- Try(t.getIndex(indexName))
      it  <- Try(idx.query(spec).iterator())
    } yield it.asScala
  }

  @tailrec
  private def queryAllInternal[E](request: QueryRequest, stream: Stream[E] = Stream.empty)(
    handler: Map[String, AttributeValue] => E): Stream[E] = {
    val result = dynamoDBClient.query(request)
    val s      = stream ++ result.getItems.asScala.map(item => handler(item.asScala.toMap)).toStream
    val last   = result.getLastEvaluatedKey
    if (last == null) return s
    request.withExclusiveStartKey(last)
    queryAllInternal(request, s)(handler)
  }

  protected def queryAll[E](request: QueryRequest)(handler: Map[String, AttributeValue] => E) =
    queryAllInternal(request)(handler)

  protected def updateItem(item: UpdateItemSpec): Try[UpdateItemOutcome] = Try {
    (for {
      t ← getTable()
      o ← Try(t.updateItem(item))
    } yield o).fold(
      {
        case o: ConditionalCheckFailedException => throw o
        //        case o: ConditionalCheckFailedException => throw OptimisticLockingException(o)
        case t                                  => throw t
      },
      l => l
    )
  }

  protected def iterator2Entity[E](f: Option[Item] => Option[E])(
    iterator: IteratorSupport[Item, QueryOutcome]) = Try {
    for {
      it <- Option(iterator)
      if it.hasNext
      e <- f(Option(it.next))
    } yield e
  }

  protected def deleteItem(hashKeyName: String, hashKeyValue: String) =
    for {
      t ← getTable()
      _ ← Try(t.deleteItem(hashKeyName, hashKeyValue))
    } yield ()

  protected def deleteItem(hashKeyName: String,
                           hashKeyValue: String,
                           rangeKeyName: String,
                           rangeKeyValue: String) =
    for {
      t ← getTable()
      _ ← Try(t.deleteItem(hashKeyName, hashKeyValue, rangeKeyName, rangeKeyValue))
    } yield ()
}

