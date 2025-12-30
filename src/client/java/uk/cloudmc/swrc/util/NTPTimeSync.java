// Derived with permission from "Bodkin Boats" by BillBodkin
// Accessible at https://gitlab.com/billyg270/bodkin-boats as of 19/12/2025

package uk.cloudmc.swrc.util;

import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;
import org.apache.commons.net.ntp.TimeStamp;
import uk.cloudmc.swrc.SWRCConfig;

import java.net.InetAddress;
import java.util.concurrent.TimeUnit;

import static java.lang.System.currentTimeMillis;

public class NTPTimeSync {

    private static volatile boolean precise = false;
    private static volatile long offset = 0;

    public static void attemptTimeSync() throws InterruptedException {
        for (int i= 1; i < 5; i++) {
            try {
                System.out.println("Attempt " + i + " at getting time from NTP server");

                NTPUDPClient client = new NTPUDPClient();
                client.setDefaultTimeout(10_000);

                InetAddress inetAddress = InetAddress.getByName(SWRCConfig.getInstance().ntp_server);
                TimeInfo timeInfo = client.getTime(inetAddress);
                timeInfo.computeDetails();
                if (timeInfo.getOffset() != null) {
                    offset = timeInfo.getOffset();
                    precise = true;
                } else {
                    System.err.println("[SWRC NTP Time Sync] Failed to fetch offset. Using system time.");
                    offset = 0L;
                }

                TimeStamp systemNtpTime = TimeStamp.getCurrentTime();
                System.out.println("[SWRC NTP Time Sync] System time:\t" + systemNtpTime + "  " + systemNtpTime.toDateString());

                // Calculate the remote server NTP time
                long currentTime = currentTimeMillis();
                TimeStamp atomicNtpTime = TimeStamp.getNtpTime(currentTime + offset);

                System.out.println("[SWRC NTP Time Sync] Atomic time:\t" + atomicNtpTime + "  " + atomicNtpTime.toDateString());
                System.out.println("[SWRC NTP Time Sync] Offset:\t" + offset + "ms");

                return;
            } catch (Exception ignored) {
                System.out.println("[SWRC NTP Time Sync] Failed to get time from NTP server");
                TimeUnit.SECONDS.sleep(1);
            }
        }
    }

    public static long getTrueTime(){
        return currentTimeMillis() + offset;
    }

    public static boolean isPrecise() {
        return precise;
    }

    public static long getOffset() {
        return offset;
    }
}
