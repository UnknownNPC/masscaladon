package com.github.unknownnpc.masscaladon.exception

import scala.concurrent.duration.FiniteDuration

import org.slf4j.LoggerFactory

import sttp.model.StatusCode

sealed class MasscaladonException(message: String, cause: Throwable = null)
    extends RuntimeException(message, cause)

class MasscaladonClientException(message: String, cause: Throwable = null)
    extends MasscaladonException(message, cause)

class MasscaladonTimeoutException(timeout: FiniteDuration, cause: Throwable)
    extends MasscaladonClientException(
      s"Request did not complete within $timeout",
      cause,
    )

sealed class MasscaladonApiException(
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
  private val logger = LoggerFactory.getLogger(this.getClass)

  def fromStatusCode(statusCode: StatusCode, body: String)
      : MasscaladonApiException =
    val code = statusCode.code
    val bodyPreview = if body.length > 50 then body.take(50) + "..." else body

    logger.info(
      s"Starting exception mapping for status code: $code. Body length: ${body.length}. Body preview: $bodyPreview",
    )

    val exception = code match
      case 400 =>
        logger.info(
          s"Identified 400 Bad Request. Creating BadRequestException.",
        )
        new BadRequestException(body)
      case 401 =>
        logger.info(
          s"Identified 401 Unauthorized. Creating UnauthorizedException.",
        )
        new UnauthorizedException(body)
      case 403 =>
        logger.info(s"Identified 403 Forbidden. Creating ForbiddenException.")
        new ForbiddenException(body)
      case 404 =>
        logger.info(s"Identified 404 Not Found. Creating NotFoundException.")
        new NotFoundException(body)
      case 429 =>
        logger.info(
          s"Identified 429 Rate Limit Exceeded. Creating RateLimitExceededException.",
        )
        new RateLimitExceededException(body)
      case c if c >= 500 =>
        logger.info(
          s"Identified Server Error ($c). Creating ServerErrorException.",
        )
        new ServerErrorException(c, body)
      case c =>
        logger.info(
          s"Identified Unknown API Error ($c). Creating UnknownApiException.",
        )
        new UnknownApiException(c, body)

    logger.info(
      s"Exception mapping completed. Returning ${exception.getClass.getSimpleName} with message: ${exception.getMessage}",
    )
    exception
