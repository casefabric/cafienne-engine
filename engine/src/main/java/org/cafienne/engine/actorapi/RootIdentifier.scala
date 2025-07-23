package org.cafienne.engine.actorapi

import org.cafienne.infrastructure.serialization.Fields
import org.cafienne.json.{CafienneJson, Value, ValueMap}

case class RootIdentifier(identifier: String) extends CafienneJson {
  override def toValue: Value[_] = new ValueMap(Fields.identifier, identifier)

  override def toString: String = identifier
}

object RootIdentifier {
  def deserialize(json: ValueMap): RootIdentifier = RootIdentifier(json.readString(Fields.identifier))
}
