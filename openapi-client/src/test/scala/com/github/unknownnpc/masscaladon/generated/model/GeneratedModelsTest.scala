package com.github.unknownnpc.masscaladon.generated.model

import java.net.URI
import java.time.OffsetDateTime

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import io.circe.Json
import io.circe.parser.decode
import io.circe.syntax.*

/** These aren't tests of hand-written code — they're regression tests for the
  * codegen customization in openapi-client/templates/ (see CLAUDE.md). Each one
  * pins down a specific way the stock scala-sttp + circe `AutoDerivation` combo
  * used to fail under Scala 3, now fixed by giving every model its own
  * `derives Decoder, Encoder`.
  */
class GeneratedModelsTest extends AnyFlatSpec with Matchers {

  private val someUri = URI.create("https://example.social/x")
  private val someInstant = OffsetDateTime.parse("2024-01-01T00:00:00Z")

  private def account(id: String, moved: Option[Account] = None) =
    Account(
      acct = s"user$id",
      avatar = someUri,
      avatarStatic = someUri,
      bot = false,
      createdAt = someInstant,
      displayName = s"User $id",
      emojis = Seq.empty,
      fields = Seq.empty,
      followersCount = 0,
      followingCount = 0,
      group = false,
      header = someUri,
      headerStatic = someUri,
      id = id,
      locked = false,
      note = "",
      statusesCount = 0,
      uri = someUri,
      username = s"user$id",
      moved = moved,
    )

  "Account" should "round-trip through JSON, including a self-referential `moved` account" in {
    // Account.moved: Option[Account] is why the generator's default
    // AutoDerivation setup failed to compile at all: automatic, inline
    // derivation can't tie the knot on a recursive case class. An explicit
    // `derives Decoder, Encoder` (a named, not inline, instance) can.
    val movedTo = account("2")
    val original = account("1", moved = Some(movedTo))

    val roundTripped = decode[Account](original.asJson.noSpaces)

    roundTripped shouldBe Right(original)
    roundTripped.map(_.moved) shouldBe Right(Some(movedTo))
  }

  "Status" should "round-trip through JSON" in {
    // Status has ~30 fields; circe's derives-based derivation expands one
    // inline per field, which exceeded the compiler's default inline budget
    // (32) until -Xmax-inlines:64 was added (see project/Settings.scala).
    val status = Status(
      account = account("1"),
      content = "hello world",
      createdAt = someInstant,
      emojis = Seq.empty,
      favouritesCount = 0,
      id = "1",
      mediaAttachments = Seq.empty,
      mentions = Seq.empty,
      reblogsCount = 0,
      repliesCount = 0,
      sensitive = false,
      spoilerText = "",
      tags = Seq.empty,
      uri = "https://example.social/statuses/1",
      visibility = StatusVisibilityEnum.Public,
    )

    decode[Status](status.asJson.noSpaces) shouldBe Right(status)
  }

  "PostStatusReblogRequest" should "round-trip a top-level enum field" in {
    val request =
      PostStatusReblogRequest(visibility = Some(StatusVisibilityEnum.Unlisted))

    decode[PostStatusReblogRequest](request.asJson.noSpaces) shouldBe Right(
      request,
    )
  }

  it should "serialize the enum as its wire value, not the Scala identifier" in {
    val request =
      PostStatusReblogRequest(visibility = Some(StatusVisibilityEnum.Unlisted))

    request.asJson.hcursor.downField("visibility").as[String] shouldBe Right(
      "unlisted",
    )
  }

  "Suggestion" should "round-trip a per-field nested enum (SuggestionEnums.Sources)" in {
    // The nested-enum given was, for a while, mistakenly typed against
    // itself (Sources.Sources instead of the enclosing Sources alias) and
    // failed to compile at all. This exercises that the fixed version both
    // compiles and decodes/encodes correctly.
    val suggestion = Suggestion(
      account = account("1"),
      sources = Some(Seq(
        SuggestionEnums.Sources.Featured,
        SuggestionEnums.Sources.MostFollowed,
      )),
    )

    decode[Suggestion](suggestion.asJson.noSpaces) shouldBe Right(suggestion)
  }

  "Rule" should "round-trip a free-form JSON field" in {
    // Rule.translations has no fixed shape in the spec (`type: object` with
    // no properties). It maps to io.circe.Json (via the `object` entry in
    // openapi-client/openapi-config.yaml's typeMappings) rather than `Any`,
    // which circe has no way to derive a codec for.
    val translations = Json.obj(
      "fr" -> Json.obj("text" -> Json.fromString("Pas de racisme.")),
    )
    val rule =
      Rule(id = "1", text = "Be nice", translations = Some(translations))

    val roundTripped = decode[Rule](rule.asJson.noSpaces)

    roundTripped shouldBe Right(rule)
    roundTripped.map(_.translations) shouldBe Right(Some(translations))
  }

}
