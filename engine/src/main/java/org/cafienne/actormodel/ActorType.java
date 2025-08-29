package org.cafienne.actormodel;

import org.cafienne.actormodel.event.ModelEvent;
import org.cafienne.cmmn.actorapi.event.CaseEvent;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.consentgroup.ConsentGroupActor;
import org.cafienne.consentgroup.actorapi.event.ConsentGroupEvent;
import org.cafienne.processtask.actorapi.event.ProcessEvent;
import org.cafienne.processtask.instance.ProcessTaskActor;
import org.cafienne.tenant.TenantActor;
import org.cafienne.tenant.actorapi.event.TenantEvent;

public enum ActorType {
    ModelActor(ModelActor.class, ModelEvent.class),
    Case(Case.class, CaseEvent.class),
    Process(ProcessTaskActor.class, ProcessEvent.class),
    Group(ConsentGroupActor.class, ConsentGroupEvent.class),
    Tenant(TenantActor.class, TenantEvent.class);

    public final String value;
    public final Class<? extends ModelActor> actorClass;
    public final Class<? extends ModelEvent> actorEventClass;
    public final boolean isCase;
    public final boolean isProcess;
    public final boolean isGroup;
    public final boolean isTenant;
    public final boolean isGeneric;
    public final boolean isModel;

    ActorType(Class<? extends ModelActor> actorClass, Class<? extends ModelEvent> actorEventClass) {
        this.actorClass = actorClass;
        this.actorEventClass = actorEventClass;
        this.value = actorClass.getSimpleName();
        this.isCase = actorClass == Case.class;
        this.isProcess = actorClass == ProcessTaskActor.class;
        this.isGroup = actorClass == ConsentGroupActor.class;
        this.isTenant = actorClass == TenantActor.class;
        this.isGeneric = actorClass == ModelActor.class;
        this.isModel = this.isCase || this.isProcess;
    }

    public static ActorType getEnum(String value) {
        if (value == null) return null;
        for (ActorType type : values()) {
            if (type.value.equalsIgnoreCase(value)) return type;
        }
        return null;
    }

    @Override
    public String toString() {
        return value;
    }
}
