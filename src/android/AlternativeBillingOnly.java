package com.appyourself.cordova.alternativebilling;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;

import org.json.JSONArray;
import org.json.JSONException;

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

                callbackContext.error(
                    "BILLING_SETUP_FAILED: "
                        + billingResult.getResponseCode()
                        + " "
                        + billingResult.getDebugMessage()
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
            callbackContext.error("BILLING_NOT_CONNECTED");
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

                    callbackContext.error(
                        "ALTERNATIVE_BILLING_NOT_AVAILABLE: "
                            + billingResult.getResponseCode()
                            + " "
                            + billingResult.getDebugMessage()
                    );
                }
            }
        );
    }

    private void showInfoDialog(CallbackContext callbackContext) {
        if (billingClient == null || !billingClient.isReady()) {
            callbackContext.error("BILLING_NOT_CONNECTED");
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
                            callbackContext.error("USER_CANCELED");
                            return;
                        }

                        callbackContext.error(
                            "INFO_DIALOG_FAILED: "
                                + billingResult.getResponseCode()
                                + " "
                                + billingResult.getDebugMessage()
                        );
                    }
                };

            BillingResult billingResult =
                billingClient.showAlternativeBillingOnlyInformationDialog(
                    cordova.getActivity(),
                    listener
                );

            if (billingResult.getResponseCode() != BillingClient.BillingResponseCode.OK) {
                callbackContext.error(
                    "INFO_DIALOG_FAILED: "
                        + billingResult.getResponseCode()
                        + " "
                        + billingResult.getDebugMessage()
                );
            }
        });
    }

    private void getReportingToken(CallbackContext callbackContext) {
        if (billingClient == null || !billingClient.isReady()) {
            callbackContext.error("BILLING_NOT_CONNECTED");
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
                        callbackContext.error(
                            "REPORTING_TOKEN_FAILED: "
                                + billingResult.getResponseCode()
                                + " "
                                + billingResult.getDebugMessage()
                        );
                        return;
                    }

                    if (reportingDetails == null) {
                        callbackContext.error("REPORTING_DETAILS_MISSING");
                        return;
                    }

                    String externalTransactionToken =
                        reportingDetails.getExternalTransactionToken();

                    callbackContext.success(externalTransactionToken);
                }
            }
        );
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