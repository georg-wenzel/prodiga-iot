package uibk.ac.at.prodigaclient;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uibk.ac.at.prodigaclient.BluetoothUtility.CubeManager;
import uibk.ac.at.prodigaclient.api.AuthControllerApi;
import uibk.ac.at.prodigaclient.api.CubeControllerApi;
import uibk.ac.at.prodigaclient.api.IntrinsicsControllerApi;
import uibk.ac.at.prodigaclient.threads.AuthThread;
import uibk.ac.at.prodigaclient.threads.FeedThread;
import uibk.ac.at.prodigaclient.threads.HistorySyncThread;

public class Client {

    private static final Logger logger = LogManager.getLogger();

    public static void main(String[] args) throws InterruptedException {

        // We start the client and read the MAC Address - this is the "ID" of the current client
        try {
            logger.info("RaspberryPi registered with internal ID " + Constants.getInternalId());
        } catch (Exception ex) {
            // If this fails we log it and kill the client
            logger.error("Error while reading MAC-Address! Aborting!", ex);
            return;
        }

        // Set up auth thread
        AuthThread authThread = new AuthThread();

        // Get a Function Pointer to invoke the auth thread - used by other threads to invoke the auth process
        Constants.setAuthAction(authThread::invokeAuth);

        // Set aup all other threads
        HistorySyncThread historySyncThread = new HistorySyncThread();
        FeedThread feedThread = new FeedThread();

        Thread authThreadThread = new Thread(authThread, "AuthThread");
        Thread historySyncThreadThread = new Thread(historySyncThread, "HistorySyncThread");
        Thread feedThreadThread = new Thread(feedThread, "FeedThreadThread");

        // Then we start the auth thread
        authThreadThread.start();
        historySyncThreadThread.start();
        feedThreadThread.start();
        authThreadThread.join();
        historySyncThreadThread.join();
        feedThreadThread.join();
    }
}