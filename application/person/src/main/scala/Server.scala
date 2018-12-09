import akka.http.scaladsl.Http

object Server extends App with Route {
  print("server start!!!")
  Http().bindAndHandle(route, "0.0.0.0", sys.props.get("http.port").fold(9999)(_.toInt))
}
