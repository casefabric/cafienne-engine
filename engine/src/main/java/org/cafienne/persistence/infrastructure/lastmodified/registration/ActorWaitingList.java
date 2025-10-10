package org.cafienne.persistence.infrastructure.lastmodified.registration;

import org.apache.pekko.dispatch.Futures;
import org.cafienne.actormodel.debug.DebugInfoAppender;
import org.cafienne.actormodel.message.event.ActorModified;
import org.cafienne.actormodel.message.response.ActorLastModified;
import org.cafienne.persistence.infrastructure.lastmodified.LastModifiedRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Promise;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

public class ActorWaitingList {
    private final static Logger logger = LoggerFactory.getLogger(ActorWaitingList.class);

    private final LastModifiedRegistration lastModifiedRegistration;
    public final String actorId;
    private final ConcurrentLinkedQueue<Waiter> waiters = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<ActorUpdate> updates = new ConcurrentLinkedQueue<>();
    private ActorUpdate lastProcessedActorUpdate;

    public ActorWaitingList(LastModifiedRegistration lastModifiedRegistration, String actorId) {
        this.lastModifiedRegistration = lastModifiedRegistration;
        this.actorId = actorId;
    }

    public Promise<String> waitForCorrelationId(String correlationId) {
        Promise<String> p = Futures.promise();
        if (updates.stream().anyMatch(update -> update.correlationId.equals(correlationId))) {
            p.success("Your correlation id arrived already!");
        } else {
            addWaiter(new Waiter(this, correlationId, p));
        }
        return p;
    }

    public Promise<String> waitForLastModified(ActorLastModified notBefore) {
        log(() -> "Executing query after response for " + notBefore);
        Instant awaitingEventMoment = notBefore.lastModified;

        Promise<String> p = Futures.promise();
        if (awaitingEventMoment.isBefore(lastModifiedRegistration.previousCleaningRound)) {
            p.success("That's quite an old timestamp; we're not gonna wait for it; we started at " + lastModifiedRegistration.previousCleaningRound);
        } else if (lastProcessedActorUpdate != null && !awaitingEventMoment.isAfter(lastProcessedActorUpdate.lastModified)) {
            p.success("Your case last modified arrived already!");
        } else {
            addWaiter(new Waiter(this, awaitingEventMoment, p));
        }
        return p;
    }

    public void handle(ActorModified<?, ?> event) {
        ActorUpdate latest = new ActorUpdate(event);
        lastProcessedActorUpdate = latest;
        updates.add(latest);
        List<Waiter> matches = waiters.stream().filter(waiter -> waiter.matches(latest)).toList();
//            System.out.println("Found " + matches.size() +" waiters on event " + event.lastModified);
        waiters.removeAll(matches);
        matches.forEach(Waiter::stopWaiting);
    }

    public void cleanup(Instant cleanUntil) {
        checkListRemoval(cleanUntil);
    }

    private void stop() {
//        System.out.println("Stop listening to updates for " + actorId);
        lastModifiedRegistration.removeWaitingList(actorId);
    }

    private void checkListRemoval(Instant cleanUntil) {
//        System.out.println("Cleaning registration for actor[" + actorId +"] has " + waiters.size() +" waiters and " + updates.size() +" updates");
        List<Waiter> tooLate = waiters.stream().filter(waiter -> waiter.createdAt.isBefore(cleanUntil)).toList();
        waiters.removeAll(tooLate);
        tooLate.forEach(Waiter::tooLate);
        List<ActorUpdate> tooLongAgo = updates.stream().filter(update -> update.receivedAt.isBefore(cleanUntil)).toList();
        updates.removeAll(tooLongAgo);
        if (updates.isEmpty() && waiters.isEmpty()) {
            stop();
        }
    }

    private void addWaiter(Waiter waiter) {
        waiters.add(waiter);
    }

    void logTooLate(long waitedFor) {
        if (updates.isEmpty()) {
            log(() -> "Cleaning a waiter for updates on actor: " + actorId + " after " + waitedFor + " milliseconds. No updates received from actor.");
        } else {
            String updateList = updates.stream().map(u -> "- " + u.lastModified).collect(Collectors.joining("\n"));
            log(() -> "Cleaning a waiter that waited " + waitedFor + " milliseconds for updates on actor " + actorId + "; received " + updates.size() + " updates:\n" + updateList);
        }
    }

    void log(DebugInfoAppender messageProducer) {
        // TEMPORARY LOGGING CODE
        if (logger.isDebugEnabled()) {
            String msg = String.valueOf(messageProducer.info());
            msg = lastModifiedRegistration.name + " in thread: " + Thread.currentThread().getName() + ": " + msg;
            logger.debug(msg);
        }
    }
}
