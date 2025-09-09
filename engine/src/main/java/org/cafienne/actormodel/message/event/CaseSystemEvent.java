package org.cafienne.actormodel.message.event;

import org.cafienne.actormodel.ModelActor;
import org.cafienne.actormodel.identity.UserIdentity;
import org.cafienne.json.ValueMap;

public abstract class CaseSystemEvent extends BaseModelEvent<ModelActor, UserIdentity> {
    protected CaseSystemEvent(ModelActor actor) {
        super(actor);
    }

    protected CaseSystemEvent(ValueMap json) {
        super(json);
    }

    @Override
    protected UserIdentity readUser(ValueMap json) {
        return UserIdentity.deserialize(json);
    }
}
