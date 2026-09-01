package com.appyourself.cordova.alternativebilling;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.UUID;

public class AlternativeBillingOnlyMock extends CordovaPlugin {

    @Override
    public boolean execute(
        String action,
        JSONArray args,
        CallbackContext callbackContext
    ) throws JSONException {

        switch (action) {
            case "connect":
                connect(callbackContext);
                return true;

            case "isAvailable":
                isAvailable(callbackContext);
                return true;

            case "showInfoDialog":
                showInfoDialog(callbackContext);
                return true;

            case "getReportingToken":
                getReportingToken(callbackContext);
                return true;

            default:
                return false;
        }
    }

    private void connect(CallbackContext callbackContext) {
        callbackContext.success();
    }

    private void isAvailable(CallbackContext callbackContext) {
        callbackContext.success();
    }

    private void showInfoDialog(CallbackContext callbackContext) {
        callbackContext.success();
    }

    private void getReportingToken(CallbackContext callbackContext) {
        callbackContext.success(
            "mock-external-transaction-token-" + UUID.randomUUID()
        );
    }
}