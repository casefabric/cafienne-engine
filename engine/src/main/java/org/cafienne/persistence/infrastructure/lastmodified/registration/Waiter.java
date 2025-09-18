package org.cafienne.persistence.infrastructure.lastmodified.registration;

import scala.concurrent.Promise;

import java.time.Instant;
import java.util.function.Function;

class Waiter {
    private final ActorWaitingList list;
    final Promise<String> promise;
    final Instant createdAt = Instant.now();
    private final Function<ActorUpdate, Boolean> matcher;

    Waiter(ActorWaitingList list, Instant notBefore, Promise<String> promise) {
        // We have to do the matching with "not" statement, because lastModified and notBefore may equal as well.
        this(list, promise, actorUpdatedEvent -> !notBefore.isAfter(actorUpdatedEvent.lastModified));
    }

    Waiter(ActorWaitingList list, String correlationId, Promise<String> promise) {
        this(list, promise, update -> update.correlationId.equals(correlationId));
    }

    private Waiter(ActorWaitingList list, Promise<String> promise, Function<ActorUpdate, Boolean> matcher) {
        this.list = list;
        this.promise = promise;
        this.matcher = matcher;
    }

    void tooLate() {
        long waitedFor = Instant.now().toEpochMilli() - createdAt.toEpochMilli();
        list.logTooLate(waitedFor);
        promise.success("Cleaning Waiter for updates on actor: " + this.list.actorId + " after " + waitedFor + " milliseconds.");
    }

    void stopWaiting() {
        list.log(() -> "Waited " + (Instant.now().toEpochMilli() - createdAt.toEpochMilli()) + " milliseconds");
        if (!promise.isCompleted()) {
            // Only invoke the promise if no one has done it yet
            promise.success("Your case last modified arrived just now");
        } else {
            list.log(() -> "AFTER STOP WAITING, BUT ALREADY COMPLETED?!");
        }
    }

    boolean matches(ActorUpdate update) {
        return matcher.apply(update);
    }
}

