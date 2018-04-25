package org.lastmilehealth.kiosk;

import java.util.Comparator;

/**
 * Created by Thamizhan on 28/02/17.
 */

public class AppOrderComparator implements Comparator<AppDetail> {
    @Override
    public int compare(AppDetail appDetail1, AppDetail appDetail2) {
        return Integer.compare(appDetail1.displayOrder, appDetail2.displayOrder);
    }
}
