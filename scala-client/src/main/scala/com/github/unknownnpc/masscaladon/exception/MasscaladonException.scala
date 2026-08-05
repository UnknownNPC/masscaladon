package com.github.unknownnpc.masscaladon.exception

import scala.concurrent.duration.FiniteDuration

import sttp.model.StatusCode

sealed abstract class MasscaladonException(
    message: String,
    cause: Throwable = null)
    extends RuntimeException(message, cause)

class MasscaladonClientException(message: String, cause: Throwable = null)
    extends MasscaladonException(message, cause)

class MasscaladonTimeoutException(timeout: FiniteDuration, cause: Throwable)
    extends MasscaladonClientException(
      s"Request did not complete within $timeout",
      cause,
    )

sealed abstract class MasscaladonApiException(
    val code: Int,
    val body: String,
    message: String)
    extends MasscaladonException(message)

class BadRequestException(body: String)
    extends MasscaladonApiException(400, body, s"Bad Request (400): $body")

class UnauthorizedException(body: String)
    extends MasscaladonApiException(401, body, s"Unauthorized (401): $body")

class ForbiddenException(body: String)
    extends MasscaladonApiException(403, body, s"Forbidden (403): $body")

class NotFoundException(body: String)
    extends MasscaladonApiException(404, body, s"Not Found (404): $body")

class RateLimitExceededException(body: String)
    extends MasscaladonApiException(
      429,
      body,
      s"Rate Limit Exceeded (429): $body",
    )

class ServerErrorException(code: Int, body: String)
    extends MasscaladonApiException(code, body, s"Server Error ($code): $body")

class UnknownApiException(code: Int, body: String)
    extends MasscaladonApiException(
      code,
      body,
      s"Unknown API Error ($code): $body",
    )

object MasscaladonException:

  def fromStatusCode(statusCode: StatusCode, body: String)
      : MasscaladonApiException =
    statusCode.code match
      case 400 => BadRequestException(body)
      case 401 => UnauthorizedException(body)
      case 403 => ForbiddenException(body)
      case 404 => NotFoundException(body)
      case 429 => RateLimitExceededException(body)
      case c if c >= 500 => ServerErrorException(c, body)
      case c => UnknownApiException(c, body)
