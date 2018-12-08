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

  val idArgument = Argument("id", StringType, description = "id")
  val nameArgument = Argument("name", StringType, description = "name")
  val QueryType = ObjectType(
    "Query", fields[PersonRepository, Unit](
      Field("person", OptionType(personObject),
        arguments = Nil,
        //        arguments = idArgument :: Nil,
        //        arguments = idArgument :: nameArgument :: Nil,
        resolve = ctx => ctx.ctx findBy("person_1", "HOGE") match {
          //        resolve = ctx => ctx.ctx findBy(ctx.arg(idArgument), "HOGE") match {
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
