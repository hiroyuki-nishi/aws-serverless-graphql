package graphql

import domain.{Page, RepositoryError}
import dynamodb.{Person, PersonRepository}
import sangria.schema.{Argument, Field, ListType, ObjectType, OptionType, Schema, StringType, fields}

object SchemaDefinition {
  import sangria.macros.derive._
  val personObject = deriveObjectType[PersonRepository, Person](
    ObjectTypeDescription("person"),
    DocumentField("id", "name")
  )
  val personsObject = deriveObjectType[PersonRepository, Person](
    ObjectTypeDescription("persons"),
    DocumentField("id", "name")
  )

  val idArgument = Argument("id", StringType, description = "id")
  val nameArgument = Argument("name", StringType, description = "name")
  val QueryType = ObjectType(
    "Query", fields[PersonRepository, Unit](
      Field("person", OptionType(personObject),
        arguments = Nil,
        //        arguments = idArgument :: nameArgument :: Nil,
        resolve = ctx => ctx.ctx findBy("person_1", "HOGE") match {
          //        resolve = ctx => ctx.ctx findBy(ctx.arg(idArgument), "HOGE") match {
          //        resolve = ctx => ctx.ctx findBy(ctx.arg(idArgument), ctx.arg(nameArgument)) match {
          case Right(value)=> value.getOrElse(Person("", "xxx"))
          case Left(_) => Person("", "error")
        }),
      Field("persons", ListType(personsObject),
        resolve = ctx => ctx.ctx.findAllBy("person_1", 1, 1) match {
          case Right(value)=> value.data
        })
    )
  )
  val PersonSchema = Schema(QueryType)
}
