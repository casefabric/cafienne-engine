package org.cafienne.persistence.infrastructure.lastmodified.registration;

import org.apache.pekko.dispatch.Futures;
import org.cafienne.actormodel.message.event.ActorModified;
import org.cafienne.actormodel.message.response.ActorLastModified;
import org.cafienne.persistence.infrastructure.lastmodified.LastModifiedRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Promise;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.cafienne.persistence.infrastructure.lastmodified.LastModifiedRegistration.WAIT_TIMEOUT;

public class ActorWaitingList {
    private final static Logger logger = LoggerFactory.getLogger(ActorWaitingList.class);

    private final LastModifiedRegistration lastModifiedRegistration;
    public final String actorId;
    private static final Instant startupMoment = LastModifiedRegistration.startupMoment;
    private final List<Waiter> waiters = new ArrayList<>();
    private final List<ActorUpdate> updates = new ArrayList<>();
    private ActorUpdate lastUpdate;

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
        log("Executing query after response for " + notBefore);
        Instant eventMoment = notBefore.lastModified;

        Promise<String> p = Futures.promise();
        if (eventMoment.isBefore(startupMoment)) {
            p.success("That's quite an old timestamp; we're not gonna wait for it; we started at " + startupMoment);
        } else if (lastUpdate != null && !eventMoment.isAfter(lastUpdate.lastModified)) {
            p.success("Your case last modified arrived already!");
        } else {
            addWaiter(new Waiter(this, eventMoment, p));
        }
        return p;
    }

    public void handle(ActorModified<?> event) {
        ActorUpdate latest = new ActorUpdate(event);
        synchronized (updates) {
            lastUpdate = latest;
            updates.add(latest);
        }
        synchronized (waiters) {
            List<Waiter> matches = waiters.stream().filter(waiter -> waiter.matches(latest)).toList();
//            System.out.println("Found " + matches.size() +" waiters on event " + event.lastModified);
            waiters.removeAll(matches);
            matches.forEach(Waiter::stopWaiting);
        }
    }

    public void cleanup(Instant now) {
        checkListRemoval(now);
    }

    private void stop() {
//        System.out.println("Stop listening to updates for " + actorId);
        lastModifiedRegistration.removeWaitingList(actorId);
    }

    private void checkListRemoval(Instant now) {
//        System.out.println("Cleaning registration for actor[" + actorId +"] has " + waiters.size() +" waiters and " + updates.size() +" updates");
        List<Waiter> tooLate = waiters.stream().filter(waiter -> waiter.createdAt.plus(WAIT_TIMEOUT).isBefore(now)).toList();
        synchronized (waiters) {
            waiters.removeAll(tooLate);
        }
        tooLate.forEach(Waiter::tooLate);
        List<ActorUpdate> tooLongAgo = updates.stream().filter(update -> update.receivedAt.plus(WAIT_TIMEOUT).isBefore(now)).toList();
        synchronized (updates) {
            updates.removeAll(tooLongAgo);
        }
        if (updates.isEmpty() && waiters.isEmpty()) {
            stop();
        }
    }

    private void addWaiter(Waiter waiter) {
        synchronized (waiters) {
            waiters.add(waiter);
        }
    }

    void log(String msg) {
        // TEMPORARY LOGGING CODE
        if (logger.isDebugEnabled()) {
            msg = lastModifiedRegistration.name + " in thread: " + Thread.currentThread().getName() + ": " + msg;
            logger.debug(msg);
        }
    }
}
