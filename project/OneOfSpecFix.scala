import io.circe.{Json, JsonObject}
import io.circe.parser.parse

// Fixes oneOf.required in the spec: openapi-generator unions it across variants,
// we want the intersection. Runs before openapi-generator-cli sees the spec.
object OneOfSpecFix {

  def fix(rawSpecJson: String): String = {
    val json = parse(rawSpecJson).fold(
      err => sys.error(s"OneOfSpecFix: failed to parse spec JSON: ${err.getMessage}"),
      identity,
    )
    val schemas = json.hcursor.downField("components").downField("schemas").focus.getOrElse(Json.obj())
    walk(json, schemas).spaces2
  }

  // `oneOf: [X, {type: null}]` nullable idiom, not a real variant.
  private def isNullSchema(j: Json): Boolean =
    j.asObject.exists { o =>
      o.size == 1 && o("type").flatMap(_.asString).contains("null")
    }

  private def resolveRef(ref: String, schemas: Json): Json =
    schemas.hcursor
      .downField(ref.stripPrefix("#/components/schemas/"))
      .focus
      .getOrElse(sys.error(s"OneOfSpecFix: unresolved $$ref: $ref"))

  // oneOf members are often a bare $ref, so resolve before inspecting.
  private def resolve(schema: Json, schemas: Json): Json =
    schema.asObject.flatMap(_("$ref")).flatMap(_.asString) match {
      case Some(ref) => resolveRef(ref, schemas)
      case None      => schema
    }

  // required from the schema itself plus any allOf branch.
  private def effectiveRequired(schema: Json, schemas: Json): Set[String] = {
    val resolved = resolve(schema, schemas)
    val ownRequired = resolved.hcursor
      .downField("required")
      .focus
      .flatMap(_.asArray)
      .getOrElse(Vector.empty)
      .flatMap(_.asString)
      .toSet
    val allOfRequired = resolved.hcursor
      .downField("allOf")
      .focus
      .flatMap(_.asArray)
      .getOrElse(Vector.empty)
      .flatMap(member => effectiveRequired(member, schemas))
      .toSet
    ownRequired ++ allOfRequired
  }

  // properties from the schema itself plus any allOf branch.
  private def effectiveProperties(schema: Json, schemas: Json): Json = {
    val resolved = resolve(schema, schemas)
    val ownProps = resolved.hcursor.downField("properties").focus.getOrElse(Json.obj())
    val allOfProps = resolved.hcursor
      .downField("allOf")
      .focus
      .flatMap(_.asArray)
      .getOrElse(Vector.empty)
      .foldLeft(Json.obj())((acc, member) => acc.deepMerge(effectiveProperties(member, schemas)))
    allOfProps.deepMerge(ownProps)
  }

  // Replaces oneOf with a single object schema: properties unioned, required intersected.
  private def flattenOneOf(members: Vector[Json], siblings: JsonObject, schemas: Json): Json = {
    val nonNullMembers = members.filterNot(isNullSchema)
    val requiredSets = nonNullMembers.map(effectiveRequired(_, schemas))
    val mergedRequired = requiredSets.reduce(_ intersect _).toList.sorted
    val mergedProperties = nonNullMembers.foldLeft(Json.obj())((acc, m) => acc.deepMerge(effectiveProperties(m, schemas)))

    val synthesized = Json
      .obj(
        "type" -> Json.fromString("object"),
        "properties" -> mergedProperties,
      )
      .deepMerge(
        if (mergedRequired.isEmpty) Json.obj()
        else Json.obj("required" -> Json.fromValues(mergedRequired.map(Json.fromString))),
      )

    Json.fromJsonObject(siblings).deepMerge(synthesized)
  }

  // Recurses over the whole spec, flattening any oneOf with 2+ non-null variants.
  private def walk(j: Json, schemas: Json): Json =
    j.asObject match {
      case Some(obj) =>
        obj("oneOf").flatMap(_.asArray) match {
          case Some(members) if members.count(m => !isNullSchema(m)) >= 2 =>
            val siblings = JsonObject.fromIterable(obj.toList.filterNot(_._1 == "oneOf"))
            walk(flattenOneOf(members, siblings, schemas), schemas)
          case _ =>
            Json.fromJsonObject(JsonObject.fromIterable(obj.toList.map { case (k, v) => k -> walk(v, schemas) }))
        }
      case None =>
        j.asArray match {
          case Some(items) => Json.fromValues(items.map(walk(_, schemas)))
          case None         => j
        }
    }
}
