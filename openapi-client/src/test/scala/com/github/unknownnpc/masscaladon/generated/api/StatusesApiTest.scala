package com.github.unknownnpc.masscaladon.generated.api

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import com.github.unknownnpc.masscaladon.generated.model.CreateStatusRequest

/** Regression test for header parameters (HeaderParamSpecFix and the header
  * part of api.mustache, see CLAUDE.md). `Idempotency-Key` used to be typed as
  * free-form JSON and rendered with `Option#toString`, so every status was sent
  * with `Idempotency-Key: None` - and Mastodon answers a repeated key, for an
  * hour, with the status it created first instead of posting a new one.
  */
class StatusesApiTest extends AnyFlatSpec with Matchers {

  private val status = CreateStatusRequest(status = Some("hello"))

  "createStatus" should "send the idempotency key exactly as given" in {
    val request =
      StatusesApi.createStatus(status, idempotencyKey = Some("post-42"))

    request.headers.filter(
      _.name == "Idempotency-Key",
    ).map(_.value) shouldBe Seq("post-42")
  }

  it should "send no Idempotency-Key header at all when no key is given" in {
    StatusesApi.createStatus(status).header("Idempotency-Key") shouldBe None
  }

}
