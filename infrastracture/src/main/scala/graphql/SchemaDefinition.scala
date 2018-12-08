package graphql

import domain.{Page, RepositoryError}
import dynamodb._
import sangria.schema.{Argument, Field, ListType, ObjectType, OptionType, Schema, StringType, fields}

object SchemaDefinition {
  import sangria.macros.derive._
  val personObject = deriveObjectType[PersonRepository, Person](
    ObjectTypeDescription("person"),
    DocumentField("id", "name")
  )
  // TODO Page[E]で返す方法
  val personsObject = deriveObjectType[PersonRepository, Person](
    ObjectTypeDescription("persons"),
    DocumentField("id", "name")
  )
  // TODO case class
  val accountObject = deriveObjectType[AccountRepository, Account](
    ObjectTypeDescription("account"),
    DocumentField("personId", "email")
  )

  val accountRepository = new AccountRepository with AccountRepositoryOnDynamoDB
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
        }),
      Field("accounts", OptionType(accountObject),
        resolve = ctx => accountRepository.findBy("person_1", Email("administrator@hogeo.com")) match {
          case Right(r)=> r.getOrElse(Account("xxx", "xxx"))
        }),
    )
  )
  val PersonSchema = Schema(QueryType)
}
