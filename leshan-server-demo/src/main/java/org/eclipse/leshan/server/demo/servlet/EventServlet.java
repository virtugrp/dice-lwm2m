/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
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
 *******************************************************************************/
package org.eclipse.leshan.server.demo.servlet;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.californium.core.network.Endpoint;
import org.eclipse.jetty.servlets.EventSource;
import org.eclipse.jetty.servlets.EventSourceServlet;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.response.ObserveResponse;
import org.eclipse.leshan.server.californium.LeshanServer;
import org.eclipse.leshan.server.demo.servlet.json.LwM2mNodeSerializer;
import org.eclipse.leshan.server.demo.servlet.json.RegistrationSerializer;
import org.eclipse.leshan.server.demo.servlet.log.CoapMessage;
import org.eclipse.leshan.server.demo.servlet.log.CoapMessageListener;
import org.eclipse.leshan.server.demo.servlet.log.CoapMessageTracer;
import org.eclipse.leshan.server.demo.utils.AppConfigs;
import org.eclipse.leshan.server.demo.utils.PathMapping;
import org.eclipse.leshan.server.observation.ObservationListener;
import org.eclipse.leshan.server.queue.PresenceListener;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.registration.RegistrationListener;
import org.eclipse.leshan.server.registration.RegistrationUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

public class EventServlet extends EventSourceServlet {

    private static final String EVENT_DEREGISTRATION = "DEREGISTRATION";

    private static final String EVENT_UPDATED = "UPDATED";

    private static final String EVENT_REGISTRATION = "REGISTRATION";

    private static final String EVENT_AWAKE = "AWAKE";

    private static final String EVENT_SLEEPING = "SLEEPING";

    private static final String EVENT_NOTIFICATION = "NOTIFICATION";

    private static final String EVENT_COAP_LOG = "COAPLOG";

    private static final String QUERY_PARAM_ENDPOINT = "ep";

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(EventServlet.class);

    private final Gson gson;

    private final CoapMessageTracer coapMessageTracer;

    private Set<LeshanEventSource> eventSources = Collections
            .newSetFromMap(new ConcurrentHashMap<LeshanEventSource, Boolean>());

    private final RegistrationListener registrationListener = new RegistrationListener() {

        @Override
        public void registered(Registration registration, Registration previousReg,
                Collection<Observation> previousObsersations) {
            String jReg = EventServlet.this.gson.toJson(registration);
            LOG.info("registered:jReg[" + jReg +"] and registration.getEndpoint[" + registration.getEndpoint() + "]");
            sendEvent(EVENT_REGISTRATION, jReg, registration.getEndpoint());
        }

        @Override
        public void updated(RegistrationUpdate update, Registration updatedRegistration,
                Registration previousRegistration) {
            RegUpdate regUpdate = new RegUpdate();
            regUpdate.registration = updatedRegistration;
            regUpdate.update = update;
            String jReg = EventServlet.this.gson.toJson(regUpdate);
            LOG.info("updated:jReg[" + jReg +"] and updatedRegistration.getEndpoint[" + updatedRegistration.getEndpoint() + "]");
            sendEvent(EVENT_UPDATED, jReg, updatedRegistration.getEndpoint());
        }

        @Override
        public void unregistered(Registration registration, Collection<Observation> observations, boolean expired,
                Registration newReg) {
            String jReg = EventServlet.this.gson.toJson(registration);
            LOG.info("unregistered:jReg[" + jReg +"] and registration.getEndpoint[" + registration.getEndpoint() + "]");
            sendEvent(EVENT_DEREGISTRATION, jReg, registration.getEndpoint());
        }

    };

    public final PresenceListener presenceListener = new PresenceListener() {

        @Override
        public void onSleeping(Registration registration) {
            String data = new StringBuilder("{\"ep\":\"").append(registration.getEndpoint()).append("\"}").toString();
            LOG.info("onSleeping:data[" + data +"] and registration.getEndpoint[" + registration.getEndpoint() + "]");
            sendEvent(EVENT_SLEEPING, data, registration.getEndpoint());
        }

        @Override
        public void onAwake(Registration registration) {
            String data = new StringBuilder("{\"ep\":\"").append(registration.getEndpoint()).append("\"}").toString();
            LOG.info("onAwake:data[" + data +"] and registration.getEndpoint[" + registration.getEndpoint() + "]");

            String deviceName = registration.getEndpoint();
            for (PathMapping pm : PathMapping.values()) {
                LOG.info("Auto Observation (via onAwake) is starting for enum-name:" + pm + " and enum-val:" + pm.getPathCode());
                String finalUrl = AppConfigs.getLwm2mBaseUrl() + "/api/clients/" + deviceName + pm.getPathCode() + "/observe?format=TLV&timeout=5";
                LOG.info("finalUrl (via onAwake):" + finalUrl);
                HttpStatus result = sendObserveAction(finalUrl);
                LOG.info("Auto Observation (via onAwake) result is: " + result.toString());
            }


            sendEvent(EVENT_AWAKE, data, registration.getEndpoint());
        }
    };

    private final ObservationListener observationListener = new ObservationListener() {

        @Override
        public void cancelled(Observation observation) {
        }

        @Override
        public void onResponse(Observation observation, Registration registration, ObserveResponse response) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Received notification from [{}] containing value [{}]", observation.getPath(),
                        response.getContent().toString());
            }

            LOG.info("onResponse-Received notification from [{}] containing value [{}]", observation.getPath(),
                    response.getContent().toString());

            if (registration != null) {
                String data = new StringBuilder("{\"ep\":\"").append(registration.getEndpoint()).append("\",\"res\":\"")
                        .append(observation.getPath().toString()).append("\",\"val\":")
                        .append(gson.toJson(response.getContent())).append("}").toString();

                LOG.info("onResponse:data[" + data +"] and registration.getEndpoint[" + registration.getEndpoint() + "]");

                sendEvent(EVENT_NOTIFICATION, data, registration.getEndpoint());
            }
        }

        @Override
        public void onError(Observation observation, Registration registration, Exception error) {
            if (LOG.isWarnEnabled()) {
                LOG.warn(String.format("Unable to handle notification of [%s:%s]", observation.getRegistrationId(),
                        observation.getPath()), error);
            }

            LOG.info(String.format("Unable to handle notification of [%s:%s]", observation.getRegistrationId(),
                    observation.getPath()), error);

            LOG.info(String.format("Unable to handle notification of [%s:%s]", observation.getRegistrationId(),
                    observation.getPath()), error);
        }

        @Override
        public void newObservation(Observation observation, Registration registration) {
            LOG.info(String.format("newObservation-registrationId:" + observation.getRegistrationId() + " and endpoint:" + registration.getEndpoint()));
        }
    };

    public EventServlet(LeshanServer server, int securePort) {

        LOG.info("EventServlet - DICE LWMTM Server is starting...");

        server.getRegistrationService().addListener(this.registrationListener);
        server.getObservationService().addListener(this.observationListener);
        server.getPresenceService().addListener(this.presenceListener);

        // add an interceptor to each endpoint to trace all CoAP messages
        coapMessageTracer = new CoapMessageTracer(server.getRegistrationService());
        for (Endpoint endpoint : server.coap().getServer().getEndpoints()) {
            endpoint.addInterceptor(coapMessageTracer);
        }

        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeHierarchyAdapter(Registration.class,
                new RegistrationSerializer(server.getPresenceService()));
        gsonBuilder.registerTypeHierarchyAdapter(LwM2mNode.class, new LwM2mNodeSerializer());
        gsonBuilder.setDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
        this.gson = gsonBuilder.create();

        LOG.info("EventServlet - gson:" + this.gson);

        LOG.info("EventServlet - DICE LWMTM Server started...");

    }

    private synchronized void sendEvent(String event, String data, String endpoint) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Dispatching {} event from endpoint {}", event, endpoint);
        }

        LOG.info("sendEvent: Dispatching {} event from endpoint {}", event, endpoint);


        for (LeshanEventSource eventSource : eventSources) {
            if (eventSource.getEndpoint() == null || eventSource.getEndpoint().equals(endpoint)) {
                LOG.info("sendEvent:data " + data);
                eventSource.sentEvent(event, data);
            }
        }
    }

    class ClientCoapListener implements CoapMessageListener {

        private final String endpoint;

        ClientCoapListener(String endpoint) {
            this.endpoint = endpoint;
        }

        @Override
        public void trace(CoapMessage message) {

            LOG.info("trace-initial[" + message.toString() + "]");
            JsonElement coapLog = EventServlet.this.gson.toJsonTree(message);
            coapLog.getAsJsonObject().addProperty("ep", this.endpoint);
            String coapLogWithEndPoint = EventServlet.this.gson.toJson(coapLog);
            LOG.info("trace-coap-endpoint:" + endpoint);
            LOG.info("trace-coapLogWithEndPoint:" + coapLogWithEndPoint);
            sendEvent(EVENT_COAP_LOG, coapLogWithEndPoint, endpoint);
        }

    }

    private void cleanCoapListener(String endpoint) {
        // remove the listener if there is no more eventSources for this endpoint
        LOG.info("cleanCoapListener-start");
        for (LeshanEventSource eventSource : eventSources) {
            LOG.info("cleanCoapListener-for:" + eventSource.getEndpoint());
            if (eventSource.getEndpoint() == null || eventSource.getEndpoint().equals(endpoint)) {
                LOG.info("cleanCoapListener-for:" + " returning because of endpoint is null");
                return;
            }
        }
        LOG.info("cleanCoapListener-enpoint:" + endpoint);
        coapMessageTracer.removeListener(endpoint);
        LOG.info("cleanCoapListener-start");
    }

    @Override
    protected EventSource newEventSource(HttpServletRequest req) {
        String endpoint = req.getParameter(QUERY_PARAM_ENDPOINT);
        LOG.info("newEventSource-endpoint:" + endpoint);
        return new LeshanEventSource(endpoint);
    }

    private class LeshanEventSource implements EventSource {

        private String endpoint;
        private Emitter emitter;

        public LeshanEventSource(String endpoint) {
            this.endpoint = endpoint;
        }

        @Override
        public void onOpen(Emitter emitter) throws IOException {
            LOG.info("onOpen");
            this.emitter = emitter;
            eventSources.add(this);
            if (endpoint != null) {
                coapMessageTracer.addListener(endpoint, new ClientCoapListener(endpoint));
                LOG.info("endpoint(" + endpoint + ") is not null so listener added");
            }
        }

        @Override
        public void onClose() {
            LOG.info("EventServlet - onClose");
            cleanCoapListener(endpoint);
            eventSources.remove(this);
        }

        public void sentEvent(String event, String data) {
            try {
                LOG.info("sentEvent-event:" + event + " and data:" + data);
                emitter.event(event, data);

            } catch (IOException e) {
                e.printStackTrace();
                onClose();
            }
        }

        public String getEndpoint() {
            return endpoint;
        }
    }

    @SuppressWarnings("unused")
    private class RegUpdate {
        public Registration registration;
        public RegistrationUpdate update;
    }

    public HttpStatus sendObserveAction(String url) {

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);

        HttpEntity<String> request = new HttpEntity<String>(null, headers);

        ResponseEntity<String> personResultAsJsonStr = restTemplate.postForEntity(
                url,
                request,
                String.class);

        return personResultAsJsonStr.getStatusCode();

    }
}
