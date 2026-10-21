import io.circe.{Json, JsonObject}
import io.circe.parser.parse

// Types every header parameter in the spec as a string. Runs before openapi-generator-cli sees the spec.
//
// A header value is text on the wire, but the upstream spec types some header parameters as a free-form
// `object` (e.g. `Idempotency-Key` on `POST /api/v1/statuses`), which typeMappings turns into `io.circe.Json`.
// Such a parameter can't be sent faithfully: a JSON string renders with its quotes. Scalar schemas (string,
// integer, number, boolean) are kept as they are; anything else becomes `{type: string}`, other schema
// keywords such as `description` preserved.
object HeaderParamSpecFix {

  private val ScalarTypes = Set("string", "integer", "number", "boolean")

  def fix(rawSpecJson: String): String = {
    val json = parse(rawSpecJson).fold(
      err => sys.error(s"HeaderParamSpecFix: failed to parse spec JSON: ${err.getMessage}"),
      identity,
    )
    walk(json).spaces2
  }

  // A Parameter Object, wherever it sits: operation, path item or components.parameters.
  private def isHeaderParam(obj: JsonObject): Boolean =
    obj("in").flatMap(_.asString).contains("header") && obj("name").exists(_.isString)

  private def isScalar(schema: JsonObject): Boolean =
    schema("type").flatMap(_.asString).exists(ScalarTypes.contains)

  private def asStringSchema(param: JsonObject): JsonObject = {
    val schema = param("schema").flatMap(_.asObject).getOrElse(JsonObject.empty)
    if (isScalar(schema)) param
    else {
      // Drop what only made sense for the old type, keep the documentation.
      val kept = schema.filterKeys(key => key == "description" || key == "example")
      param.add("schema", Json.fromJsonObject(kept.add("type", Json.fromString("string"))))
    }
  }

  private def walk(j: Json): Json =
    j.fold(
      jsonNull = j,
      jsonBoolean = _ => j,
      jsonNumber = _ => j,
      jsonString = _ => j,
      jsonArray = items => Json.fromValues(items.map(walk)),
      jsonObject = obj => {
        val fixed = if (isHeaderParam(obj)) asStringSchema(obj) else obj
        Json.fromJsonObject(fixed.mapValues(walk))
      },
    )
}
