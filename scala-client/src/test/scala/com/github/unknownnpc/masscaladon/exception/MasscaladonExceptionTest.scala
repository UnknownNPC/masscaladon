package com.github.unknownnpc.masscaladon.exception

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import sttp.model.StatusCode

class MasscaladonExceptionTest extends AnyFlatSpec with Matchers {

  "fromStatusCode" should "map 400 to BadRequestException" in {
    MasscaladonException.fromStatusCode(
      StatusCode.BadRequest,
      "bad",
    ) shouldBe a[
      BadRequestException,
    ]
  }

  it should "map 401 to UnauthorizedException" in {
    MasscaladonException.fromStatusCode(
      StatusCode.Unauthorized,
      "nope",
    ) shouldBe a[
      UnauthorizedException,
    ]
  }

  it should "map 403 to ForbiddenException" in {
    MasscaladonException.fromStatusCode(
      StatusCode.Forbidden,
      "nope",
    ) shouldBe a[
      ForbiddenException,
    ]
  }

  it should "map 404 to NotFoundException" in {
    MasscaladonException.fromStatusCode(StatusCode.NotFound, "gone") shouldBe a[
      NotFoundException,
    ]
  }

  it should "map 429 to RateLimitExceededException" in {
    MasscaladonException.fromStatusCode(
      StatusCode.TooManyRequests,
      "slow down",
    ) shouldBe a[
      RateLimitExceededException,
    ]
  }

  it should "map any 5xx to ServerErrorException, preserving the code" in {
    val exception =
      MasscaladonException.fromStatusCode(StatusCode.BadGateway, "oops")

    exception shouldBe a[ServerErrorException]
    exception.code shouldBe 502
  }

  it should "map any other code to UnknownApiException, preserving the code" in {
    val exception =
      MasscaladonException.fromStatusCode(StatusCode.unsafeApply(418), "teapot")

    exception shouldBe a[UnknownApiException]
    exception.code shouldBe 418
  }

  it should "preserve the response body on the resulting exception" in {
    MasscaladonException.fromStatusCode(
      StatusCode.NotFound,
      "no such status",
    ).body shouldBe "no such status"
  }

}
