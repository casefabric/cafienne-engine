package org.cafienne.actormodel.communication

import org.cafienne.actormodel.message.UserMessage

trait CaseSystemCommunicationMessage extends UserMessage {

  override def toString: String = getDescription
}
