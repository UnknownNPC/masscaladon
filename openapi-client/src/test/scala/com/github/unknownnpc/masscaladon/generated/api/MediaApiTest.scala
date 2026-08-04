package com.github.unknownnpc.masscaladon.generated.api

import java.nio.file.Files

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import sttp.client3.MultipartBody

/** Regression test for the paramMultipartCreation.mustache override (see
  * CLAUDE.md): the stock template built multipart bodies as
  * `Seq(requiredPart, optionalPart.map(...), ...).flatten`, mixing a bare
  * element with Option-wrapped ones. That doesn't just fail to compile under
  * Scala 3 — if it *did* compile (e.g. under Scala 2), it's still worth pinning
  * down that wrapping the required part in `Some(...)` doesn't silently drop it
  * or change its name/position at runtime.
  */
class MediaApiTest extends AnyFlatSpec with Matchers {

  private val file = Files.createTempFile("masscaladon-test", ".png").toFile

  private def partNames(request: sttp.client3.Request[?, ?]): Seq[String] =
    request.body match
      case MultipartBody(parts) => parts.map(_.name)
      case other => fail(s"expected a MultipartBody, got $other")

  "createMedia" should "include only the required `file` part when optional params are omitted" in {
    partNames(MediaApi.createMedia(file)) shouldBe Seq("file")
  }

  it should "include every part, in order, when all optional params are provided" in {
    val thumbnail =
      Files.createTempFile("masscaladon-test-thumb", ".png").toFile

    partNames(
      MediaApi.createMedia(
        file,
        description = Some("a description"),
        focus = Some("0,0"),
        thumbnail = Some(thumbnail),
      ),
    ) shouldBe Seq("file", "description", "focus", "thumbnail")
  }

  it should "include only the provided optional parts, skipping the omitted ones" in {
    partNames(
      MediaApi.createMedia(file, description = Some("a description")),
    ) shouldBe Seq("file", "description")
  }

}
