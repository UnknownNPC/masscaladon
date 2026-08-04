package com.github.unknownnpc.masscaladon.client

import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration.*

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import com.github.unknownnpc.masscaladon.exception.MasscaladonClientException
import com.github.unknownnpc.masscaladon.exception.MasscaladonTimeoutException
import com.github.unknownnpc.masscaladon.exception.NotFoundException

import io.circe.Decoder
import io.circe.Encoder
import sttp.client3.*
import sttp.client3.circe.*
import sttp.client3.testing.SttpBackendStub
import sttp.model.StatusCode

// Plain (non-Async) spec on purpose: `executeSync` blocks the calling thread,
// and doing that from an AsyncFlatSpec test body starves the very thread
// pool the underlying Future needs to complete on, timing out every time.
class MasscaladonTest extends AnyFlatSpec with Matchers {

  private given ExecutionContext = ExecutionContext.global

  private case class Ping(ok: Boolean) derives Decoder, Encoder

  private val token = "test-token"
  private val baseUrl = "https://example.social"

  // The explicit return type is load-bearing: it's what lets `asJson[Ping]`
  // (naturally `ResponseException[String, io.circe.Error]`) widen into the
  // `ResponseException[String, Exception]` that `execute`/`executeSync`
  // (and every generated `*Api` method) are defined over.
  private def request(
      path: String = "/ping",
    ): Request[Either[ResponseException[String, Exception], Ping], Any] =
    basicRequest.get(uri"$path").response(asJson[Ping])

  private def clientWith(
      backend: SttpBackend[Future, Any],
      timeout: FiniteDuration = 1.second,
    ): Masscaladon =
    Masscaladon.withBackend(backend, baseUrl, token, timeout)

  "execute" should "resolve the decoded body on a successful response" in {
    val mastodon = clientWith(
      SttpBackendStub.asynchronousFuture.whenAnyRequest.thenRespond(
        """{"ok":true}""",
      ),
    )

    Await.result(mastodon.execute(request()), 1.second) shouldBe Ping(true)
  }

  it should "resolve the request's relative URI against the client's baseUrl" in {
    val backend = SttpBackendStub.asynchronousFuture
      .whenRequestMatches(_.uri.toString == s"$baseUrl/ping")
      .thenRespond("""{"ok":true}""")
    val mastodon = clientWith(backend)

    Await.result(mastodon.execute(request()), 1.second) shouldBe Ping(true)
  }

  it should "send the token as a bearer Authorization header" in {
    val backend = SttpBackendStub.asynchronousFuture
      .whenRequestMatches(req =>
        req.headers.exists(h =>
          h.name == "Authorization" && h.value == s"Bearer $token",
        ),
      )
      .thenRespond("""{"ok":true}""")
    val mastodon = clientWith(backend)

    Await.result(mastodon.execute(request()), 1.second) shouldBe Ping(true)
  }

  it should "fail with a typed MasscaladonException on an HTTP error" in {
    val mastodon = clientWith(
      SttpBackendStub.asynchronousFuture.whenAnyRequest.thenRespond(
        Response("""{"error":"not found"}""", StatusCode.NotFound),
      ),
    )

    a[NotFoundException] should be thrownBy Await.result(
      mastodon.execute(request()),
      1.second,
    )
  }

  it should "fail with MasscaladonClientException when the backend itself fails" in {
    // Distinct from an HTTP error response: the send() call never produces a
    // Response at all (e.g. connection refused, DNS failure). Covers the
    // recoverWith fallback in Masscaladon.execute.
    val mastodon = clientWith(
      SttpBackendStub.asynchronousFuture.whenAnyRequest.thenRespondF(
        Future.failed(new RuntimeException("connection refused")),
      ),
    )

    a[MasscaladonClientException] should be thrownBy Await.result(
      mastodon.execute(request()),
      1.second,
    )
  }

  "executeSync" should "return Right with the decoded body on success" in {
    val mastodon = clientWith(
      SttpBackendStub.asynchronousFuture.whenAnyRequest.thenRespond(
        """{"ok":true}""",
      ),
    )

    mastodon.executeSync(request()) shouldBe Right(Ping(true))
  }

  it should "return Left with a typed MasscaladonException on an HTTP error" in {
    val mastodon = clientWith(
      SttpBackendStub.asynchronousFuture.whenAnyRequest.thenRespond(
        Response("""{"error":"not found"}""", StatusCode.NotFound),
      ),
    )

    mastodon.executeSync(request()) match
      case Left(_: NotFoundException) => succeed
      case other => fail(s"expected Left(NotFoundException), got $other")
  }

  it should "return Left with MasscaladonClientException when the backend itself fails" in {
    val mastodon = clientWith(
      SttpBackendStub.asynchronousFuture.whenAnyRequest.thenRespondF(
        Future.failed(new RuntimeException("connection refused")),
      ),
    )

    mastodon.executeSync(request()) match
      case Left(_: MasscaladonClientException) => succeed
      case other =>
        fail(s"expected Left(MasscaladonClientException), got $other")
  }

  it should "return Left with MasscaladonTimeoutException when the backend is too slow" in {
    val slowBackend =
      SttpBackendStub.asynchronousFuture.whenAnyRequest.thenRespondF(
        Future {
          Thread.sleep(500)
          Response("""{"ok":true}""", StatusCode.Ok)
        },
      )
    val mastodon = clientWith(slowBackend, timeout = 50.millis)

    mastodon.executeSync(request()) match
      case Left(_: MasscaladonTimeoutException) => succeed
      case other =>
        fail(s"expected Left(MasscaladonTimeoutException), got $other")
  }

  it should "allow close() to be called more than once without throwing" in {
    val mastodon = clientWith(SttpBackendStub.asynchronousFuture)
    mastodon.close()
    noException should be thrownBy mastodon.close()
  }

}
