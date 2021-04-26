package com.tmax.ck;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Pair;
import okhttp3.Call;

public class GenericCaller {
    public static Call makeCall(String path, ApiClient client, boolean watch) throws ApiException {
        Object localVarPostBody = null;

        // create path and map variables
        String localVarPath = path;
    
        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();
    
        Map<String, String> localVarHeaderParams = new HashMap<String, String>();
        Map<String, String> localVarCookieParams = new HashMap<String, String>();
        Map<String, Object> localVarFormParams = new HashMap<String, Object>();
        final String[] localVarAccepts = {
          "application/json",
          "application/yaml",
          "application/vnd.kubernetes.protobuf",
          "application/json;stream=watch",
          "application/vnd.kubernetes.protobuf;stream=watch"
        };
        final String localVarAccept = client.selectHeaderAccept(localVarAccepts);
        if (localVarAccept != null) {
          localVarHeaderParams.put("Accept", localVarAccept);
        }
    
        if (watch) {
            localVarQueryParams.addAll(client.parameterToPair("watch", watch));
        }

        final String[] localVarContentTypes = {};
    
        final String localVarContentType =
        client.selectHeaderContentType(localVarContentTypes);
        localVarHeaderParams.put("Content-Type", localVarContentType);
    
        String[] localVarAuthNames = new String[] {"BearerToken"};
        return client.buildCall(
            localVarPath,
            "GET",
            localVarQueryParams,
            localVarCollectionQueryParams,
            localVarPostBody,
            localVarHeaderParams,
            localVarCookieParams,
            localVarFormParams,
            localVarAuthNames,
            null);
    }
}
