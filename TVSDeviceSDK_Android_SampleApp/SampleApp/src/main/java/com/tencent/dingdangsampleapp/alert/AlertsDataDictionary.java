package com.tencent.dingdangsampleapp.alert;

import android.provider.BaseColumns;

public class AlertsDataDictionary {

    public static final String TABLE_ALERTS = "alerts";

    public static final class AlertsColumns implements BaseColumns {
        public static final String ID = "id";
        public static final String TOKEN = "token";
        public static final String TYPE = "type";
        public static final String STATE = "state";
        public static final String SCHEDULED_TIME_ISO8601 = "scheduled_time_iso_8601";
        public static final String LOOP_COUNT = "loop_count";
        public static final String LOOP_PAUSE_MILLIS = "loop_pause_millis";
        public static final String BACKGROUND_ASSET = "background_asset";
        public static final String ASSET_PLAY_ORDER = "asset_play_order";
    }

    public static final String TABLE_ALERT_ASSETS = "alert_assets";

    public static final class AlertAssetsColumns implements BaseColumns {
        public static final String ID = "id";
        public static final String ALERT_ID = "alert_id";
        public static final String ASSET_ID = "asset_id";
        public static final String ASSET_URL = "asset_url";
    }

}
