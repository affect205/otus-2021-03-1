package me.chuwy.otusfp.homework

import cats.effect._
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import me.chuwy.otusfp.Restful.addHeader
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.circe.{jsonEncoderOf, jsonOf}
import org.http4s.dsl.impl.IntVar
import org.http4s.dsl.io._
import org.http4s.implicits.http4sKleisliResponseSyntaxOptionT
import org.http4s.server.Router
import org.http4s.{EntityDecoder, EntityEncoder, HttpApp, HttpRoutes}

import java.util.concurrent.atomic.AtomicInteger
import scala.concurrent.ExecutionContext.global
import scala.concurrent.duration.DurationInt
import scala.io.Source

object HomeworkApp extends IOApp {

// https://github.com/http4s/http4s/blob/series/0.23/examples/src/main/scala/com/example/http4s/ExampleService.scala
// This is a mock data source, but could be a Process representing results from a database
//  def dataStream(n: Int)(implicit clock: Clock[F]): Stream[F, String] = {
//    val interval = 100.millis
//    val stream = Stream
//      .awakeEvery[F](interval)
//      .evalMap(_ => clock.realTime)
//      .map(time => s"Current system time: $time ms\n")
//      .take(n.toLong)
//
//    Stream.emit(s"Starting $interval stream intervals, taking $n results\n\n") ++ stream
//  }

  val counter: AtomicInteger = new AtomicInteger(0)

  case class Counter(counter: Int)

  implicit val counterEncoder: Encoder[Counter] = deriveEncoder[Counter]
  implicit val counterDecoder: Decoder[Counter] = deriveDecoder[Counter]
  implicit def counterEntityDecoder[F[_]: Concurrent]: EntityDecoder[F, Counter] = jsonOf[F, Counter]
  implicit def counterEntityEncoder[F[_]: Concurrent]: EntityEncoder[F, Counter] = jsonEncoderOf[F, Counter]

  val serviceOne: HttpRoutes[IO] =
    HttpRoutes.of {
      case GET -> Root / "counter"  =>
        /**
         * 1. HTTP эндпоинт /counter Возвращающий JSON в виде {"counter": 1}, где число увеличивается с каждым запросом,
         * поступающим на этот эндпоинт
         **/
        Ok(Counter(counter.incrementAndGet()))

      case GET -> Root / "slow" / IntVar(chunkSize) / IntVar(totalSize) / IntVar(timeout) =>
        /**
         * 2. HTTP эндпоинт /slow/:chunk/:total/:time выдающий искусственно медленный ответ, имитируя сервер под нагрузкой
         * :chunk, :total и :time - переменные, которые пользователь эндпоинта может заменить числами, например запрос
         * по адресу /show/10/1024/5 будет выдавать body кусками по 10 байт, каждые 5 секунд пока не не достигнет 1024 байт.
         * Содержимое потока на усмотрение учащегося - может быть повторяющийся символ, может быть локальный файл.
         * Неверные значения переменных (строки или отрицательные числа в переменных) должны...
         **/

        if (chunkSize <= 0) {
          BadRequest(s"chunkSize($chunkSize) cannot be negative")
        } else if (totalSize <= 0 || totalSize < chunkSize) {
          BadRequest(s"totalSize($totalSize) cannot be negative or less than chunkSize")
        } else if (timeout <= 0 || timeout > 60) {
          BadRequest(s"timeout($timeout) cannot be negative or greater than 60 seconds")
        } else {
          def dataStream(n: Int): fs2.Stream[IO, String] = {
            // How can I read it with fs2.Stream ???
            val streamData = Source.fromFile("src/main/scala/me/chuwy/otusfp/homework/HomeworkApp.scala")
            val rawData = streamData.toArray
            streamData.close()
            fs2.Stream
              .emits(0 to n)
              .map(n => {
                fs2.Stream.emits(rawData
                  .slice(n * chunkSize, n * chunkSize + chunkSize))
              })
              .map(_.toList)
              .map(_.toArray.map(_.toChar))
              .map(String.valueOf)
              .metered[IO](timeout.seconds)
              .covary[IO]
          }
          Ok(dataStream(totalSize/chunkSize))
        }
    }

  val httpApp: HttpApp[IO] = Router(
    "/" -> addHeader(serviceOne)
  ).orNotFound

  def run(args: List[String]): IO[ExitCode] = {
    BlazeServerBuilder[IO](global)
      .bindHttp(8080, "localhost")
      .withHttpApp(httpApp)
      .serve
      .compile
      .drain
      .as(ExitCode.Success)
  }
}
