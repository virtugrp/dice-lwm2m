package org.eclipse.leshan.server.demo.listener;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.response.ObserveResponse;
import org.eclipse.leshan.server.californium.LeshanServer;
import org.eclipse.leshan.server.demo.servlet.json.LwM2mNodeSerializer;
import org.eclipse.leshan.server.demo.servlet.json.RegistrationSerializer;
import org.eclipse.leshan.server.demo.utils.AppConfigs;
import org.eclipse.leshan.server.demo.utils.PathMapping;
import org.eclipse.leshan.server.observation.ObservationListener;
import org.eclipse.leshan.server.registration.Registration;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

public class ObserveListener implements ObservationListener {

    private static final Logger LOG = LoggerFactory.getLogger(ObserveListener.class);

    private final Gson gson;

    public ObserveListener(LeshanServer server) {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeHierarchyAdapter(Registration.class,
                new RegistrationSerializer(server.getPresenceService()));
        gsonBuilder.registerTypeHierarchyAdapter(LwM2mNode.class, new LwM2mNodeSerializer());
        gsonBuilder.setDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
        this.gson = gsonBuilder.create();
    }

    @Override
    public void cancelled(Observation observation) {
    }

    @Override
    public void onResponse(Observation observation, Registration registration, ObserveResponse response) {
        /*if (LOG.isDebugEnabled()) {
            LOG.debug("Received notification from [{}] containing value [{}]", observation.getPath(),
                    response.getContent().toString());
        }*/

        LOG.info("ObserveListener - Received notification from [{}] containing value [{}]", observation.getPath(), response.getContent().toString());

        LOG.info("ObserveListener - registration.getEndpoint():" + registration.getEndpoint());
        LOG.info("ObserveListener - response.getObservation().getPath().toString():" + response.getObservation().getPath().toString());
        //LOG.info("ObserveListener - ((LwM2mSingleResource)response.getContent()).getValue().toString():" + ((LwM2mSingleResource)response.getContent()).getValue().toString());
        //LOG.info("ObserveListener - ((LwM2mMultipleResource)response.getContent()).getValues().toString():" + convertWithIteration(((LwM2m)response.getContent()).getValues()).toString());

        String telemetries = "";
        try {
            Map<Integer, LwM2mResource> lwM2mResources = ((LwM2mObjectInstance)response.getContent()).getResources();
            LOG.info("lwM2mResources:" + convertWithIteration(lwM2mResources));

            telemetries = createJsonObjectForTelemetry(lwM2mResources, observation.getPath().toString()).toString();
            LOG.info("ObserveListener - testObj.toString():" + telemetries.toString());

        } catch(ClassCastException e) {

            JSONObject jsonObject = new JSONObject();
            try {
                Integer key = ((LwM2mSingleResource)response.getContent()).getId();
                String value = ((LwM2mSingleResource)response.getContent()).getValue().toString();
                jsonObject.put(String.valueOf(key), value);
            } catch (JSONException ex) {
                ex.printStackTrace();
            }
            telemetries = jsonObject.toString();
        }

        HttpStatus httpStatus = sendTelemetryData(telemetries, AppConfigs.getDiceTestDeviceToken());

        /*Util.sendTelemetryData(registration.getEndpoint(),
                response.getObservation().getPath().toString(),
                ((LwM2mSingleResource)response.getContent()).getValue().toString());*/


        if (registration != null) {
            String data = new StringBuilder("{\"ep\":\"").append(registration.getEndpoint()).append("\",\"res\":\"")
                    .append(observation.getPath().toString()).append("\",\"val\":")
                    .append(gson.toJson(response.getContent())).append("}").toString();

            System.out.println(data);
        }
    }

    public String convertWithIteration(Map<Integer, ?> map) {
        StringBuilder mapAsString = new StringBuilder("{");
        for (Integer key : map.keySet()) {
            mapAsString.append(key + "=" + map.get(key) + ", ");
        }
        mapAsString.delete(mapAsString.length()-2, mapAsString.length()).append("}");
        return mapAsString.toString();
    }

    public JSONObject createJsonObjectForTelemetry(Map<Integer, ?> map, String rootPath) {
        JSONObject telemetryMap = new JSONObject();
        for (Integer key : map.keySet()) {
            String keyStr = String.valueOf(key);
            String fullPath = rootPath + "/" + keyStr;
            if(isPathAllowed(fullPath)) {
                LOG.info("Path is allowed: " + fullPath);
                try {
                    telemetryMap.put(PathMapping.getPathMapping(fullPath).name(), ((LwM2mSingleResource)map.get(key)).getValue());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else {
                LOG.info("Path is skipping: " + fullPath);
            }
        }

        return telemetryMap;
    }

    @Override
    public void onError(Observation observation, Registration registration, Exception error) {
        /*if (LOG.isWarnEnabled()) {
            LOG.warn(String.format("Unable to handle notification of [%s:%s]", observation.getRegistrationId(),
                    observation.getPath()), error);
        }*/

        LOG.error(String.format("Unable to handle notification of [%s:%s]", observation.getRegistrationId(),
                observation.getPath()), error);
    }

    @Override
    public void newObservation(Observation observation, Registration registration) {
    }

    public HttpStatus sendTelemetryData(String telemetryValues, String thingsBoardDeviceAccessToken) {

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);

        HttpEntity<String> request = new HttpEntity<String>(telemetryValues, headers);

        ResponseEntity<String> personResultAsJsonStr = restTemplate.postForEntity(
                AppConfigs.getDiceBaseUrl() + "/api/v1/" + thingsBoardDeviceAccessToken + "/telemetry",
                request,
                String.class);

        return personResultAsJsonStr.getStatusCode();

/*
        System.out.println("****sendTelemetryData start");
        OkHttpClient client = new OkHttpClient();
        MediaType mediaType = MediaType.parse("text/plain");
        RequestBody body = RequestBody.create(mediaType, "{\"" + key + "\"" + ":\"" + value + "\"}");


        System.out.print("****sendTelemetryData url:");
        System.out.println(url);

        Request requestPost = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("content-type", "application/json")
                .addHeader("cache-control", "no-cache")
                .build();

        client.newCall(requestPost)
                .enqueue(new Callback() {
                    public void onFailure(final Call call, IOException e) {
                        // Error
                        System.out.print("****sendTelemetryData failure:");
                        System.out.println(e.getMessage());

                    }

                    public void onResponse(Call call, final Response response) throws IOException {
                        String res = response.body().string();
                        System.out.print("****sendTelemetryData success:");
                        System.out.println(res);

                        // Do something with the response
                    }
                });

        */

    }

    private boolean isPathAllowed (String currentPath) {

        String[] allowedPaths = AppConfigs.getLwm2mAllowedPathsArray();

        for (int i=0; i<allowedPaths.length;++i) {
            if (allowedPaths[i].equals(currentPath)) {
                return true;
            }
        }

        return false;
    }
}
