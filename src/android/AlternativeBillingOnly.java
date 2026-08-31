package com.appyourself.cordova.alternativebilling;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.android.billingclient.api.AlternativeBillingOnlyAvailabilityListener;
import com.android.billingclient.api.AlternativeBillingOnlyInformationDialogListener;
import com.android.billingclient.api.AlternativeBillingOnlyReportingDetails;
import com.android.billingclient.api.AlternativeBillingOnlyReportingDetailsListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingResult;

public class AlternativeBillingOnly extends CordovaPlugin {

    private BillingClient billingClient;

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
        if (billingClient != null && billingClient.isReady()) {
            callbackContext.success();
            return;
        }

        billingClient = BillingClient.newBuilder(cordova.getActivity())
            .enableAlternativeBillingOnly()
            .enableAutoServiceReconnection()
            .build();

        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    callbackContext.success();
                    return;
                }

                sendBillingError(
                    callbackContext,
                    getErrorCode("BILLING_SETUP_FAILED", billingResult),
                    billingResult
                );
            }

            @Override
            public void onBillingServiceDisconnected() {
                // Automatic reconnection is enabled.
            }
        });
    }

    private void isAvailable(CallbackContext callbackContext) {
        if (billingClient == null || !billingClient.isReady()) {
            sendError(
                callbackContext,
                "BILLING_NOT_CONNECTED",
                "Billing client is not connected."
            );
            return;
        }

        billingClient.isAlternativeBillingOnlyAvailableAsync(
            new AlternativeBillingOnlyAvailabilityListener() {
                @Override
                public void onAlternativeBillingOnlyAvailabilityResponse(
                    BillingResult billingResult
                ) {
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                        callbackContext.success();
                        return;
                    }

                    sendBillingError(
                        callbackContext,
                        getErrorCode("ALTERNATIVE_BILLING_NOT_AVAILABLE", billingResult),
                        billingResult
                    );
                }
            }
        );
    }

    private void showInfoDialog(CallbackContext callbackContext) {
        if (billingClient == null || !billingClient.isReady()) {
            sendError(
                callbackContext,
                "BILLING_NOT_CONNECTED",
                "Billing client is not connected."
            );
            return;
        }

        cordova.getActivity().runOnUiThread(() -> {
            AlternativeBillingOnlyInformationDialogListener listener =
                new AlternativeBillingOnlyInformationDialogListener() {
                    @Override
                    public void onAlternativeBillingOnlyInformationDialogResponse(
                        BillingResult billingResult
                    ) {
                        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                            callbackContext.success();
                            return;
                        }

                        if (
                            billingResult.getResponseCode()
                                == BillingClient.BillingResponseCode.USER_CANCELED
                        ) {
                            sendBillingError(
                                callbackContext,
                                "USER_CANCELED",
                                billingResult
                            );
                            return;
                        }

                        sendBillingError(
                            callbackContext,
                            getErrorCode("INFO_DIALOG_FAILED", billingResult),
                            billingResult
                        );
                    }
                };

            BillingResult billingResult =
                billingClient.showAlternativeBillingOnlyInformationDialog(
                    cordova.getActivity(),
                    listener
                );

            if (billingResult.getResponseCode() != BillingClient.BillingResponseCode.OK) {
                sendBillingError(
                    callbackContext,
                    getErrorCode("INFO_DIALOG_FAILED", billingResult),
                    billingResult
                );
            }
        });
    }

    private void getReportingToken(CallbackContext callbackContext) {
        if (billingClient == null || !billingClient.isReady()) {
            sendError(
                callbackContext,
                "BILLING_NOT_CONNECTED",
                "Billing client is not connected."
            );
            return;
        }

        billingClient.createAlternativeBillingOnlyReportingDetailsAsync(
            new AlternativeBillingOnlyReportingDetailsListener() {
                @Override
                public void onAlternativeBillingOnlyTokenResponse(
                    BillingResult billingResult,
                    AlternativeBillingOnlyReportingDetails reportingDetails
                ) {
                    if (
                        billingResult.getResponseCode()
                            != BillingClient.BillingResponseCode.OK
                    ) {
                        sendBillingError(
                            callbackContext,
                            getErrorCode("REPORTING_TOKEN_FAILED", billingResult),
                            billingResult
                        );
                        return;
                    }

                    if (reportingDetails == null) {
                        sendError(
                            callbackContext,
                            "REPORTING_DETAILS_MISSING",
                            "Alternative billing reporting details are missing."
                        );
                        return;
                    }

                    String externalTransactionToken =
                        reportingDetails.getExternalTransactionToken();

                    callbackContext.success(externalTransactionToken);
                }
            }
        );
    }

    private String getErrorCode(
        String defaultCode,
        BillingResult billingResult
    ) {
        if (
            billingResult.getResponseCode()
                == BillingClient.BillingResponseCode.NETWORK_ERROR
        ) {
            return "NETWORK_ERROR";
        }

        return defaultCode;
    }

    private void sendBillingError(
        CallbackContext callbackContext,
        String code,
        BillingResult billingResult
    ) {
        JSONObject error = new JSONObject();

        try {
            error.put("code", code);
            error.put(
                "billingResponseCode",
                billingResult.getResponseCode()
            );
            error.put(
                "message",
                billingResult.getDebugMessage()
            );

            callbackContext.error(error);
        } catch (JSONException exception) {
            callbackContext.error(code);
        }
    }

    private void sendError(
        CallbackContext callbackContext,
        String code,
        String message
    ) {
        JSONObject error = new JSONObject();

        try {
            error.put("code", code);
            error.put("message", message);

            callbackContext.error(error);
        } catch (JSONException exception) {
            callbackContext.error(code);
        }
    }

    @Override
    public void onDestroy() {
        if (billingClient != null) {
            billingClient.endConnection();
            billingClient = null;
        }

        super.onDestroy();
    }
}