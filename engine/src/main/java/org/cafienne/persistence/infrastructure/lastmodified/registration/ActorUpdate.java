package org.cafienne.persistence.infrastructure.lastmodified.registration;

import org.cafienne.actormodel.message.event.ActorModified;

import java.time.Instant;

class ActorUpdate {
    final Instant receivedAt = Instant.now();
    final Instant lastModified;
    final String correlationId;

    ActorUpdate(ActorModified<?, ?> event) {
        this.lastModified = event.lastModified;
        this.correlationId = event.correlationId();
    }
}
