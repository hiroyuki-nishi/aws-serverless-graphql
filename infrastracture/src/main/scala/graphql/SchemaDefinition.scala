package graphql


object SchemaDefinition {
  import domain.{Page, RepositoryError}
  import dynamodb._
  import sangria.macros.derive._
  import sangria.schema._

  val idArgument = Argument("id", StringType, description = "id")
  val nameArgument = Argument("name", StringType, description = "name")
  // TODO Page[Person]で返す方法
  val pageNoArgument = Argument("page_no", IntType, description = "page_no")
  val pageSizeArgument = Argument("page_size", IntType, description = "page_size")
  val personObject = deriveObjectType[PersonRepository, Person](
    ObjectTypeDescription("person"),
    DocumentField("id", "name")
  )

  val accountRepository = new AccountRepository with AccountRepositoryOnDynamoDB
  val personIdArgument = Argument("person_id", StringType, description = "person_id")
  val emailArgument = Argument("email", StringType, description = "email")
  // TODO case class
  val accountObject = deriveObjectType[AccountRepository, Account](
    ObjectTypeDescription("account"),
    DocumentField("personId", "email")
  )

  val QueryType = ObjectType(
    "Query", fields[PersonRepository, Unit](
      Field("person", OptionType(personObject),
        arguments = idArgument :: nameArgument :: Nil,
        resolve = ctx => ctx.ctx findBy(ctx.arg(idArgument), ctx.arg(nameArgument)) match {
          case Right(r)=> r.getOrElse(Person("", "xxx"))
          case Left(_) => Person("", "error")
        }),
      Field("persons", ListType(personObject),
        arguments = idArgument :: pageNoArgument :: pageSizeArgument :: Nil,
        resolve = ctx => ctx.ctx.findAllBy(ctx.arg(idArgument), ctx.arg(pageNoArgument), ctx.arg(pageSizeArgument)) match {
          case Right(value)=> value.data
        }),
      Field("account", OptionType(accountObject),
        arguments = personIdArgument :: emailArgument :: Nil,
        resolve = ctx => accountRepository.findBy(ctx.arg(personIdArgument), ctx.arg(emailArgument)) match {
          case Right(r)=> r.getOrElse(Account("xxx", "xxx"))
        }),
      Field("accounts", ListType(accountObject),
        arguments = personIdArgument :: pageNoArgument :: pageSizeArgument :: Nil,
        resolve = ctx => accountRepository.findAllBy(ctx.arg(personIdArgument), ctx.arg(pageNoArgument), ctx.arg(pageSizeArgument)) match {
          case Right(r)=> r.data
        }),
    )
  )
  val PersonSchema = Schema(QueryType)
}
