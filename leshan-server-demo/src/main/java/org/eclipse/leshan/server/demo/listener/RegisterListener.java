package org.eclipse.leshan.server.demo.listener;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.observe.ObservationUtil;
import org.eclipse.leshan.core.californium.EndpointContextUtil;
import org.eclipse.leshan.core.node.*;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mNodeDecoder;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.request.ObserveRequest;
import org.eclipse.leshan.core.response.ObserveResponse;
import org.eclipse.leshan.server.californium.LeshanServer;
import org.eclipse.leshan.server.californium.observation.ObservationServiceImpl;
import org.eclipse.leshan.server.californium.observation.ObserveUtil;
import org.eclipse.leshan.server.californium.registration.CaliforniumRegistrationStore;
import org.eclipse.leshan.server.californium.registration.InMemoryRegistrationStore;
import org.eclipse.leshan.server.demo.servlet.json.LwM2mNodeSerializer;
import org.eclipse.leshan.server.demo.servlet.json.RegistrationSerializer;
import org.eclipse.leshan.server.demo.utils.AppConfigs;
import org.eclipse.leshan.server.model.StandardModelProvider;
import org.eclipse.leshan.server.observation.ObservationListener;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.registration.RegistrationListener;
import org.eclipse.leshan.server.registration.RegistrationUpdate;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Collection;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class RegisterListener implements RegistrationListener {

    private static final Logger LOG = LoggerFactory.getLogger(RegisterListener.class);

    private final Gson gson;

    public RegisterListener(LeshanServer server) {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeHierarchyAdapter(Registration.class,
                new RegistrationSerializer(server.getPresenceService()));
        gsonBuilder.registerTypeHierarchyAdapter(LwM2mNode.class, new LwM2mNodeSerializer());
        gsonBuilder.setDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
        this.gson = gsonBuilder.create();
    }


    @Override
    public void registered(Registration registration, Registration previousReg, Collection<Observation> previousObsersations) {

    }

    @Override
    public void updated(RegistrationUpdate update, Registration updatedReg, Registration previousReg) {

        LOG.info("Auto Observation is starting...");

        String deviceName = updatedReg.getEndpoint();
        String path = "/3303/0/"; // All Temperature

        HttpStatus result = sendObserveAction(AppConfigs.getLwm2mBaseUrl() + "/api/clients/" + deviceName + path + "observe?format=TLV&timeout=5");

        LOG.info("Auto Observation result is: " + result.toString());

        /*LwM2mPath target = new LwM2mPath(3303, 0, 5700);

        Request coapRequest = Request.newGet();
        coapRequest.setToken(createToken());
        coapRequest.getOptions().addUriPath(String.valueOf(target.getObjectId()));
        coapRequest.getOptions().addUriPath(String.valueOf(target.getObjectInstanceId()));
        coapRequest.getOptions().addUriPath(String.valueOf(target.getResourceId()));
        coapRequest.setObserve();
        coapRequest
                .setDestinationContext(EndpointContextUtil.extractContext(updatedReg.getIdentity(), true));
        Map<String, String> context = ObserveUtil.createCoapObserveRequestContext(updatedReg.getEndpoint(),
                updatedReg.getId(), new ObserveRequest(target.toString()));
        coapRequest.setUserContext(context);

        Observation obs = ObserveUtil.createLwM2mObservation(coapRequest);
        LOG.info("OBS:" + obs.toString());
        LOG.info("OBS-Context:" + convertWithIteration(obs.getContext()));

        CaliforniumRegistrationStore store = new InMemoryRegistrationStore();
        ObservationServiceImpl observationService = new ObservationServiceImpl(store, new StandardModelProvider(),
                new DefaultLwM2mNodeDecoder());
        observationService.addObservation(updatedReg, obs);

        Set<Observation> observations = observationService.getObservations(updatedReg);
        LOG.info("observations size:" + observations.size());*/

    }

    @Override
    public void unregistered(Registration registration, Collection<Observation> observations, boolean expired, Registration newReg) {

    }

    private byte[] createToken() {
        Random random = ThreadLocalRandom.current();
        byte[] token;
        token = new byte[random.nextInt(8) + 1];
        // random value
        random.nextBytes(token);
        return token;
    }


    public String convertWithIteration(Map<String, String> map) {
        StringBuilder mapAsString = new StringBuilder("");
        for (String key : map.keySet()) {
            mapAsString.append(key + "=" + map.get(key) + ", ");
        }
        return mapAsString.toString();
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
