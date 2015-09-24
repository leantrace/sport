package models

import java.util.UUID

import play.api.libs.json.Json


case class Address(
                    id: Option[Long],
                    extId: UUID,
                    street: String,
                    city: String,
                    zip: String,
                    state: String,
                    country: String)

/**
 * The companion object.
 */
object Address {

  /**
   * Converts the [User] object to Json and vice versa.
   */
  implicit val jsonFormat = Json.format[Address]
}