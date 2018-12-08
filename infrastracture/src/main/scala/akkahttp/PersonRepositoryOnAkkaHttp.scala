package akkahttp



import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.MediaTypes._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.stream.ActorMaterializer
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._
import ch.megard.akka.http.cors.scaladsl.settings.CorsSettings
import de.heikoseeberger.akkahttpcirce.ErrorAccumulatingCirceSupport._
import dynamodb.{PersonRepository, PersonRepositoryOnDynamoDB}
import graphql.GraphQLRequestUnmarshaller._
import graphql.SchemaDefinition
import io.circe._
import io.circe.optics.JsonPath._
import io.circe.parser._
import sangria.ast.Document
import sangria.execution.deferred.DeferredResolver
import sangria.execution.{ErrorWithResolver, Executor, QueryAnalysisError}
import sangria.marshalling.circe._
import sangria.parser.DeliveryScheme.Try
import sangria.parser.{QueryParser, SyntaxError}
import scala.util.control.NonFatal
import scala.util.{Failure, Success}

object PersonRepositoryOnAkkaHttp extends App {
  implicit val system = ActorSystem("sangria-server")
  implicit val materializer = ActorMaterializer()

  import system.dispatcher

  def executeGraphQL(query: Document, operationName: Option[String], variables: Json) =
    complete(Executor.execute(SchemaDefinition.PersonSchema, query, new PersonRepository with PersonRepositoryOnDynamoDB,
      variables = if (variables.isNull) Json.obj() else variables,
      operationName = operationName)
      .map(OK → _)
      .recover {
        case error: QueryAnalysisError ⇒ BadRequest → error.resolveError
        case error: ErrorWithResolver ⇒ InternalServerError → error.resolveError
      })

  def formatError(error: Throwable): Json = error match {
    case syntaxError: SyntaxError ⇒
      Json.obj("errors" → Json.arr(
        Json.obj(
          "message" → Json.fromString(syntaxError.getMessage),
          "locations" → Json.arr(Json.obj(
            "line" → Json.fromBigInt(syntaxError.originalError.position.line),
            "column" → Json.fromBigInt(syntaxError.originalError.position.column))))))
    case NonFatal(e) ⇒
      formatError(e.getMessage)
    case e ⇒
      throw e
  }

  def formatError(message: String): Json =
    Json.obj("errors" → Json.arr(Json.obj("message" → Json.fromString(message))))

  val corsSettings = CorsSettings.defaultSettings
  val route = cors(corsSettings) {
    path("graphql") {
      get {
        explicitlyAccepts(`text/html`) {
          getFromResource("./graphql.html")
        } ~
          parameters('query, 'operationName.?, 'variables.?) { (query, operationName, variables) ⇒
            QueryParser.parse(query) match {
              case Success(ast) ⇒
                variables.map(parse) match {
                  case Some(Left(error)) ⇒ complete(BadRequest, formatError(error))
                  case Some(Right(json)) ⇒ executeGraphQL(ast, operationName, json)
                  case None ⇒ executeGraphQL(ast, operationName, Json.obj())
                }
              case Failure(error) ⇒ complete(BadRequest, formatError(error))
            }
          }
      } ~
        post {
          parameters('query.?, 'operationName.?, 'variables.?) { (queryParam, operationNameParam, variablesParam) ⇒
            entity(as[Json]) { body ⇒
              val query = queryParam orElse root.query.string.getOption(body)
              val operationName = operationNameParam orElse root.operationName.string.getOption(body)
              val variablesStr = variablesParam orElse root.variables.string.getOption(body)
              query.map(QueryParser.parse(_)) match {
                case Some(Success(ast)) ⇒
                  variablesStr.map(parse) match {
                    case Some(Left(error)) ⇒ complete(BadRequest, formatError(error))
                    case Some(Right(json)) ⇒ executeGraphQL(ast, operationName, json)
                    case None ⇒ executeGraphQL(ast, operationName, root.variables.json.getOption(body) getOrElse Json.obj())
                  }
                case Some(Failure(error)) ⇒ complete(BadRequest, formatError(error))
                case None ⇒ complete(BadRequest, formatError("No query to execute"))
              }
            } ~
              entity(as[Document]) { document ⇒
                variablesParam.map(parse) match {
                  case Some(Left(error)) ⇒ complete(BadRequest, formatError(error))
                  case Some(Right(json)) ⇒ executeGraphQL(document, operationNameParam, json)
                  case None ⇒ executeGraphQL(document, operationNameParam, Json.obj())
                }
              }
          }
        }
    } ~ cors(corsSettings) {
      (get & pathEndOrSingleSlash) {
        redirect("/graphql", PermanentRedirect)
      }
    }
  }
  print("server start!!!")
  Http().bindAndHandle(route, "0.0.0.0", sys.props.get("http.port").fold(9999)(_.toInt))
}
