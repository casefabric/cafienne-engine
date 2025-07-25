package org.cafienne.system.router

import org.apache.pekko.actor.ActorRef
import org.cafienne.actormodel.command.ModelCommand

trait RouterProvider {
  def getRouter(message: ModelCommand): ActorRef
}
