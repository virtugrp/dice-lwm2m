/*******************************************************************************
 * Copyright (c) 2015 Sierra Wireless and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 * 
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 * 
 * Contributors:
 *     Sierra Wireless - initial API and implementation
 *     Achim Kraus (Bosch Software Innovations GmbH) - add defaultMinPeriod/defaultMaxPeriod
 *                                                     and reset() for REPLACE/UPDATE implementation
 *******************************************************************************/
package org.eclipse.leshan.client.object;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.eclipse.leshan.client.resource.BaseInstanceEnabler;
import org.eclipse.leshan.client.resource.LwM2mInstanceEnabler;
import org.eclipse.leshan.client.servers.ServerIdentity;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.model.ResourceModel.Type;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.request.BindingMode;
import org.eclipse.leshan.core.response.ExecuteResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.WriteResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple {@link LwM2mInstanceEnabler} for the Server (1) object.
 */
public class Server extends BaseInstanceEnabler {

    private static final Logger LOG = LoggerFactory.getLogger(Server.class);

    private final static List<Integer> supportedResources = Arrays.asList(0, 1, 2, 3, 6, 7, 8);

    private int shortServerId;
    private long lifetime;
    private Long defaultMinPeriod;
    private Long defaultMaxPeriod;
    private BindingMode binding;
    private boolean notifyWhenDisable;

    public Server() {
        // should only be used at bootstrap time
    }

    public Server(int shortServerId, long lifetime, BindingMode binding, boolean notifyWhenDisable) {
        this.shortServerId = shortServerId;
        this.lifetime = lifetime;
        this.binding = binding;
        this.notifyWhenDisable = notifyWhenDisable;
    }

    @Override
    public ReadResponse read(ServerIdentity identity, int resourceid) {
        if (!identity.isSystem())
            LOG.debug("Read on Server resource /{}/{}/{}", getModel().id, getId(), resourceid);

        switch (resourceid) {
        case 0: // short server ID
            return ReadResponse.success(resourceid, shortServerId);

        case 1: // lifetime
            return ReadResponse.success(resourceid, lifetime);

        case 2: // default min period
            if (null == defaultMinPeriod)
                return ReadResponse.notFound();
            return ReadResponse.success(resourceid, defaultMinPeriod);

        case 3: // default max period
            if (null == defaultMaxPeriod)
                return ReadResponse.notFound();
            return ReadResponse.success(resourceid, defaultMaxPeriod);

        case 6: // notification storing when disable or offline
            return ReadResponse.success(resourceid, notifyWhenDisable);

        case 7: // binding
            return ReadResponse.success(resourceid, binding.toString());

        default:
            return super.read(identity, resourceid);
        }
    }

    @Override
    public WriteResponse write(ServerIdentity identity, int resourceid, LwM2mResource value) {
        if (!identity.isSystem())
            LOG.debug("Write on Server resource /{}/{}/{}", getModel().id, getId(), resourceid);

        switch (resourceid) {
        case 0:
            if (value.getType() != Type.INTEGER) {
                return WriteResponse.badRequest("invalid type");
            }
            int previousShortServerId = shortServerId;
            shortServerId = ((Long) value.getValue()).intValue();
            if (previousShortServerId != shortServerId)
                fireResourcesChange(resourceid);
            return WriteResponse.success();

        case 1:
            if (value.getType() != Type.INTEGER) {
                return WriteResponse.badRequest("invalid type");
            }
            long previousLifetime = lifetime;
            lifetime = (Long) value.getValue();
            if (previousLifetime != lifetime)
                fireResourcesChange(resourceid);
            return WriteResponse.success();

        case 2:
            if (value.getType() != Type.INTEGER) {
                return WriteResponse.badRequest("invalid type");
            }
            Long previousDefaultMinPeriod = defaultMinPeriod;
            defaultMinPeriod = (Long) value.getValue();
            if (!Objects.equals(previousDefaultMinPeriod, defaultMinPeriod))
                fireResourcesChange(resourceid);
            return WriteResponse.success();

        case 3:
            if (value.getType() != Type.INTEGER) {
                return WriteResponse.badRequest("invalid type");
            }
            Long previousDefaultMaxPeriod = defaultMaxPeriod;
            defaultMaxPeriod = (Long) value.getValue();
            if (!Objects.equals(previousDefaultMaxPeriod, defaultMaxPeriod))
                fireResourcesChange(resourceid);
            return WriteResponse.success();

        case 6: // notification storing when disable or offline
            if (value.getType() != Type.BOOLEAN) {
                return WriteResponse.badRequest("invalid type");
            }
            boolean previousNotifyWhenDisable = notifyWhenDisable;
            notifyWhenDisable = (boolean) value.getValue();
            if (previousNotifyWhenDisable != notifyWhenDisable)
                fireResourcesChange(resourceid);
            return WriteResponse.success();

        case 7: // binding
            if (value.getType() != Type.STRING) {
                return WriteResponse.badRequest("invalid type");
            }
            try {
                BindingMode previousBinding = binding;
                binding = BindingMode.valueOf((String) value.getValue());
                if (!Objects.equals(previousBinding, binding))
                    fireResourcesChange(resourceid);
                return WriteResponse.success();
            } catch (IllegalArgumentException e) {
                return WriteResponse.badRequest("invalid value");
            }

        default:
            return super.write(identity, resourceid, value);
        }
    }

    @Override
    public ExecuteResponse execute(ServerIdentity identity, int resourceid, String params) {
        LOG.debug("Execute on Server resource /{}/{}/{}", getModel().id, getId(), resourceid);
        if (resourceid == 8) {
            getLwM2mClient().triggerRegistrationUpdate(identity);
            return ExecuteResponse.success();
        } else {
            return super.execute(identity, resourceid, params);
        }
    }

    @Override
    public void reset(int resourceid) {
        switch (resourceid) {
        case 2:
            defaultMinPeriod = null;
            break;
        case 3:
            defaultMaxPeriod = null;
            break;
        default:
            super.reset(resourceid);
        }
    }

    @Override
    public List<Integer> getAvailableResourceIds(ObjectModel model) {
        return supportedResources;
    }
}
