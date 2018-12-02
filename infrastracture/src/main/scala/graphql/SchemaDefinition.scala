package graphql

import domain.RepositoryError
import dynamodb.{Person, PersonRepository}
import sangria.schema.{Argument, Field, ObjectType, OptionType, Schema, StringType, fields}

object SchemaDefinition {
  import sangria.macros.derive._
  val personObject = deriveObjectType[PersonRepository, Person](
    ObjectTypeDescription("person"),
    DocumentField("id", "name")
  )
//  val PersonType =
//    ObjectType(
//      "Person",
//      "人",
//      fields[PersonRepository, Either[RepositoryError, Option[Person]]](
////      fields[PersonRepository, Person](
//        Field("id", StringType,
//          Some("識別id"),
//          resolve = _.value.id),
//        Field("name", StringType,
//          Some("名前"),
//          resolve = _.value.name)
//      ))

  val idArgument = Argument("id", StringType, description = "id")
  val nameArgument = Argument("name", StringType, description = "name")

  val QueryType = ObjectType(
    "Query", fields[PersonRepository, Unit](
      Field("person", OptionType(personObject),
        arguments = idArgument :: Nil,
//        arguments = idArgument :: nameArgument :: Nil,
        // PersonRepositoryからどのように記事を取得するかを記述しています(ctx.ctxはPersonRepository型)
        //TODO
        resolve = ctx => ctx.ctx findBy(ctx.arg(idArgument), "HOGE") match {
//        resolve = ctx => ctx.ctx findBy(ctx.arg(idArgument), ctx.arg(nameArgument)) match {
          case Right(value)=> value.getOrElse(Person("", "xxx"))
          case Left(_) => Person("", "error")
        }),
//      Field("articles", ListType(PersonType),
//        resolve = ctx => ctx.ctx.findAllPersons)
    )
  )
  val PersonSchema = Schema(QueryType)
}
