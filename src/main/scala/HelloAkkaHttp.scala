import java.io.IOException

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.DebuggingDirectives
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.{ActorFlowMaterializer, FlowMaterializer}
import com.typesafe.config.{Config, ConfigFactory}

import scala.concurrent.{ExecutionContextExecutor, Future}

/**
 * Akka Http as per: https://github.com/theiterators/akka-http-microservice/blob/master/src/main/scala/AkkaHttpMicroservice.scala
 *
 * Created by clelio on 27/05/15.
 */
trait HelloAkkaHttpService {

  implicit val system: ActorSystem
  implicit def executor: ExecutionContextExecutor
  implicit val materializer: FlowMaterializer

  def config: Config

  lazy val telizeConnectionFlow: Flow[HttpRequest, HttpResponse, Any] = Http().outgoingConnection(config.getString("services.telizeHost"), config.getInt("services.telizePort"))

  def telizeRequest(request: HttpRequest): Future[HttpResponse] = Source.single(request).via(telizeConnectionFlow).runWith(Sink.head)

  def sayHello(hello: String): Future[Either[String, String]] = {
    telizeRequest(RequestBuilding.Get(s"/say/$hello")).flatMap { response =>
      response.status match {
        case OK => Unmarshal(response.entity).to[String].map(Right(_))
        case BadRequest => Future.successful(Left(s"$hello: incorrect parameter"))
        case _ => Unmarshal(response.entity).to[String].flatMap { entity =>
          val error = s"Telize request failed with status code ${response.status} and entity $entity"
          Future.failed(new IOException(error))
        }
      }

    }
  }

  val routes = {
    DebuggingDirectives.logRequestResult("akka-http-microservice") {
      pathPrefix("say") {
        (get & path(Segment)) { hello =>
          complete {
            sayHello(hello).map[ToResponseMarshallable] {
              case Right(successMsg) => successMsg
              case Left(errorMsg) => BadRequest -> errorMsg
            }
          }
        }
      } ~
      path("order") {
        get {
          parameter('q) { query =>
            complete(s"Received GET with query string $query\n")
          }
        }
      }
    }
  }

}

object HelloAkkaHttp extends App with HelloAkkaHttpService {
  override implicit val system: ActorSystem = ActorSystem()
  override implicit val executor: ExecutionContextExecutor = system.dispatcher
  override implicit val materializer = ActorFlowMaterializer()

  override val config = ConfigFactory.load()

  Http().bindAndHandle(routes, config.getString("http.interface"), config.getInt("http.port"))


}
