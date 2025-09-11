/*
 * Copyright (C) 2014  Batav B.V.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.cafienne.persistence.infrastructure.lastmodified;

import org.cafienne.actormodel.message.event.ActorModified;
import org.cafienne.actormodel.message.response.ActorLastModified;
import org.cafienne.persistence.infrastructure.lastmodified.registration.ActorWaitingList;
import scala.concurrent.Promise;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Registration of the last modified timestamp per case instance. Can be used by writers to and query actors to get notified about CaseLastModified.
 */
public class LastModifiedRegistration {
    /**
     * Global startup moment of the whole JVM for last modified requests trying to be jumpy.
     */
    public final static Instant startupMoment = Instant.now();
    public final String name;
    private final Map<String, ActorWaitingList> actorLists = new HashMap<>();
    public final static long MONITOR_PERIOD = 10 * 60 * 1000; // Every 10 minutes
    public final static Duration WAIT_TIMEOUT = Duration.ofMinutes(2);

    public LastModifiedRegistration(String name) {
        this.name = name;

        Timer timer = new Timer(this.name + "-monitor", true);
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                List<ActorWaitingList> lists = actorLists.values().stream().toList();
                if (lists.isEmpty()) {
                    return;
                }
                Instant now = Instant.now();
                lists.forEach(list -> list.cleanup(now));
//                System.out.println("Cleaning " + name +" remained with " + actorLists.size() +" elements");
            }
        };
        timer.schedule(task, MONITOR_PERIOD, MONITOR_PERIOD);  // Start only after 10 minutes
    }

    private ActorWaitingList getWaitingList(String actorId) {
        synchronized (actorLists) {
            ActorWaitingList list = actorLists.get(actorId);
            if (list == null) {
                list = new ActorWaitingList(this, actorId);
                actorLists.put(actorId, list);
            }
            return list;
        }
    }

    public void removeWaitingList(String actorId) {
        synchronized (actorLists) {
            actorLists.remove(actorId);
        }
    }

    public Promise<String> waitFor(ActorLastModified notBefore) {
        return getWaitingList(notBefore.actorId).waitForLastModified(notBefore);
    }

    public Promise<String> waitFor(String actorId, String correlationId) {
        return getWaitingList(actorId).waitForCorrelationId(correlationId);
    }

    public void handle(ActorModified<?> event) {
        getWaitingList(event.actorId()).handle(event);
    }
}