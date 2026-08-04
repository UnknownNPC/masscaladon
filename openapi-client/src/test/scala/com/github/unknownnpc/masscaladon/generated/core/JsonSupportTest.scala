package com.github.unknownnpc.masscaladon.generated.core

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import com.github.unknownnpc.masscaladon.generated.model.CreateStatusRequest
import com.github.unknownnpc.masscaladon.generated.model.UpdateStatusRequestPoll

import io.circe.syntax.*

class JsonSupportTest extends AnyFlatSpec with Matchers {

  "The derived Encoder" should "successfully serialize CreateStatusRequest" in {
    val testStatus = "Test message for Mastodon"
    val request = CreateStatusRequest(
      status = testStatus,
      mediaIds = Seq.empty,
      poll = UpdateStatusRequestPoll(),
    )

    val json = request.asJson.noSpaces

    json should not be empty
    json should include(testStatus)
  }

}
