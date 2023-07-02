import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.stream.alpakka.slick.scaladsl.SlickSession
import sttp.tapir.server.akkahttp.{AkkaHttpServerInterpreter, AkkaHttpServerOptions}
import slick.jdbc.JdbcBackend.Database
import slick.jdbc.JdbcProfile
import sttp.tapir.server.model.ValuedEndpointOutput
import sttp.tapir.stringBody
import sttp.tapir.swagger.bundle.SwaggerInterpreter

import scala.language.postfixOps
import scala.concurrent.ExecutionContextExecutor
import scala.io.StdIn

import routing.CommonRouter
import podcasts.PodcastsRouter
import channels.ChannelsRouter
import items.ItemsRouter

object Start extends App {

  val app = "ps1"
  val ver = "0.1"

  Class.forName("org.postgresql.Driver")

  implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, app)
  implicit val executionContext: ExecutionContextExecutor = system.executionContext
  val db = Database.forConfig("db")
  //val db = Database.forURL("jdbc:postgresql://127.0.0.1/pm1?user=user")
  implicit val profile: JdbcProfile = slick.jdbc.PostgresProfile
  implicit val session: SlickSession = SlickSession.forDbAndProfile(db, profile)

  system.whenTerminated.foreach(_ => session.close())

  val endpoints = CommonRouter() ++ PodcastsRouter() ++ ChannelsRouter() ++ ItemsRouter()
  val swagger = SwaggerInterpreter().fromServerEndpoints(endpoints, app, ver)

  def failureResponse(msg: String): ValuedEndpointOutput[_] =
    ValuedEndpointOutput(stringBody, msg)

  val customServerOptions: AkkaHttpServerOptions =
    AkkaHttpServerOptions
      .customiseInterceptors
      .defaultHandlers(failureResponse)
      .options

  val router = AkkaHttpServerInterpreter(customServerOptions).toRoute(endpoints ++ swagger)

  val bindingFuture = Http().newServerAt("0.0.0.0", 8088).bindFlow(router)
  bindingFuture.foreach { binding =>
    println(s"Server is started: " + binding.localAddress)
    binding.whenTerminated.foreach { terminated =>
      println(s"Server is terminated: " + terminated)
    }
  }

  StdIn.readLine() // let it run until user presses return
  bindingFuture
    .flatMap(_.unbind()) // trigger unbinding from the port
    .onComplete(_ => system.terminate()) // and shutdown when done
}
