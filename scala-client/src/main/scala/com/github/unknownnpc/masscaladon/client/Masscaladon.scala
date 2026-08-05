package com.github.unknownnpc.masscaladon.client

import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration.*
import scala.util.control.NonFatal

import com.github.unknownnpc.masscaladon.exception.MasscaladonClientException
import com.github.unknownnpc.masscaladon.exception.MasscaladonException
import com.github.unknownnpc.masscaladon.exception.MasscaladonTimeoutException
import com.github.unknownnpc.masscaladon.logging.Logging

import sttp.client3.HttpClientFutureBackend
import sttp.client3.Request
import sttp.client3.ResponseException
import sttp.client3.SttpBackend
import sttp.client3.SttpBackendOptions
import sttp.model.Uri

/** Runs requests built by any generated `*Api` object against a Mastodon
  * instance. Every generated method builds a request with a relative URI — this
  * client is what attaches `baseUrl` to it, at execution time.
  *
  * {{{
  * val mastodon = Masscaladon("https://example.social", token = "...")
  *
  * // async
  * val request = StatusesApi.createStatus(CreateStatusRequest(status = "hello"))
  * mastodon.execute(request)
  *
  * // blocking, e.g. from a script or a synchronous framework
  * mastodon.executeSync(StatusesApi.getStatus(id)) match
  *   case Right(status) => println(status)
  *   case Left(error)   => println(s"failed: ${error.getMessage}")
  * }}}
  */
final class Masscaladon private (
    baseUrl: Uri,
    token: String,
    timeout: FiniteDuration,
    backend: SttpBackend[Future, Any],
  )(using ExecutionContext)
    extends Logging
    with AutoCloseable:

  def execute[T](
      request: Request[Either[ResponseException[String, Exception], T], Any],
    ): Future[T] =
    request.copy(uri = baseUrl.resolve(request.uri))
      .auth.bearer(token)
      .send(backend)
      .flatMap(response =>
        response.body match
          case Right(value) => Future.successful(value)
          case Left(err) => Future
              .failed(MasscaladonException.fromStatusCode(
                response.code,
                err.getMessage,
              )),
      )
      .recoverWith {
        case e: MasscaladonException => Future.failed(e)
        case t: Throwable =>
          Future.failed(MasscaladonClientException("Unexpected failure", t))
      }

  def executeSync[T](
      request: Request[Either[ResponseException[String, Exception], T], Any],
    ): Either[MasscaladonException, T] =
    try Right(Await.result(execute(request), timeout))
    catch
      case e: MasscaladonException => Left(e)
      case e: java.util.concurrent.TimeoutException =>
        Left(MasscaladonTimeoutException(timeout, e))
      case NonFatal(t) =>
        Left(MasscaladonClientException("Unexpected failure", t))

  private val shutdownHook: Thread = sys.addShutdownHook(
    try backend.close()
    catch
      case e: Throwable =>
        logger.error(s"Failed to auto-close backend: ${e.getMessage}", e),
  )

  override def close(): Unit =
    try Runtime.getRuntime.removeShutdownHook(shutdownHook)
    catch case _: IllegalStateException => ()
    backend.close()

object Masscaladon:

  def apply(
      baseUrl: String,
      token: String,
      timeout: FiniteDuration = 60.seconds,
    )(using ExecutionContext,
    ): Masscaladon =
    val backend = HttpClientFutureBackend(options =
      SttpBackendOptions.connectionTimeout(timeout),
    )
    new Masscaladon(Uri.unsafeParse(baseUrl), token, timeout, backend)

  /** Test-only escape hatch for injecting a stub backend instead of a real one.
    */
  private[client] def withBackend(
      backend: SttpBackend[Future, Any],
      baseUrl: String,
      token: String,
      timeout: FiniteDuration = 60.seconds,
    )(using ExecutionContext,
    ): Masscaladon =
    new Masscaladon(Uri.unsafeParse(baseUrl), token, timeout, backend)
