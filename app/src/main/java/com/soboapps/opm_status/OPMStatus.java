package com.soboapps.opm_status;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

import static com.soboapps.opm_status.R.string.statusTxt;

public class OPMStatus {
    public MainActivity.Status status;
    public String appliesto;
    public String location;
    public String txt;
    //public String exttxt;

    public OPMStatus(JSONObject obj) {

        try {
            this.appliesto = obj.getString("AppliesTo");
            this.location = obj.getString("Location");
            this.txt = obj.getString("ShortStatusMessage");
            //this.exttxt = obj.getString("ExtendedInformation");
            String iconText = obj.getString("Icon");

            switch (iconText) {
                case "Open":
                    this.status = MainActivity.Status.Open;
                    break;
                case "Alert":
                    this.status = MainActivity.Status.Alert;
                    break;
                default:
                    this.status = MainActivity.Status.Closed;
            }

        } catch (JSONException e) {
            e.printStackTrace();
            txt = "" + new OPMStatus(obj);
            this.status = MainActivity.Status.Open;
        } catch (OutOfMemoryError e){
            e.printStackTrace();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }

    }

}