package src.me.txt.test.controller;

import src.me.txt.test.model.Data;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;

@WebServlet("/proxy/*")
public class ProxyServlet extends HttpServlet {

    // TODO constants can be placed in ENV variables
    private static final Integer PRODUCERS_MAX = 10;
    private static final Integer CONSUMERS_MAX = 10;

    private LinkedBlockingDeque<Data> queue;
    private ExecutorService producerExecutorService;
    private ExecutorService consumerExecutorService;

    @Override
    public void init() throws ServletException {
        queue = new LinkedBlockingDeque<>(Integer.MAX_VALUE);
        producerExecutorService = Executors.newFixedThreadPool(PRODUCERS_MAX);
        consumerExecutorService = Executors.newFixedThreadPool(CONSUMERS_MAX);
        startConsumers();
    }

    private void startConsumers() {
        for (int i = 0; i < CONSUMERS_MAX; i++) {
            consumerExecutorService.submit(() -> {
               while(true) {
                   // check backend service (ping)
                   if (isBackendServiceAlive()) {
                       Data dataToSend = null;
                       try {
                           dataToSend = queue.take();
                           // send data to backend service
                           postDataToBackendService(dataToSend);
                       } catch (Exception e) {
                           System.out.println(Thread.currentThread().getName() + " Failed to send data to backend");
                           // return data in queue
                           if (Objects.nonNull(dataToSend)) {
                               queue.putFirst(dataToSend);
                           }
                           // delay
                           Thread.sleep(1000);
                       }
                   } else {
                       // delay
                       Thread.sleep(1000);
                   }
               }
            });
        }
    }


    private boolean isBackendServiceAlive() {
        // TODO here we can use apache httpClient to ping backend service
        return true;
    }

    private void postDataToBackendService(Data dataToSend) {
        // TODO here we can use apache httpClient to post data
        System.out.println(Thread.currentThread().getName() + " Successfully sent " + dataToSend);
    }


    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
//        if (Objects.isNull(req.getContentType()) || !req.getContentType().equals("application/json")) {
//            resp.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
//            System.out.println("Client: " + req.getRemoteAddr() + " sent not supported media type");
//            return;
//        }
        if (!req.getRequestURI().equals("/proxy/backend")) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            System.out.println("Client: " + req.getRemoteAddr() + " requested not known service");
            return;
        }
        try {
            UUID deviceId;
            try {
                deviceId = UUID.fromString(req.getHeader("Device-Id"));
            } catch (IllegalArgumentException iae) {
                System.out.println("Client: " + req.getRemoteAddr() + " is unknown");
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
            String payload = req.getParameter("payload");
            Long dateTime = Instant.now(Clock.systemUTC()).getEpochSecond();
            Data data = new Data();
            data.setDeviceId(deviceId);
            data.setPayload(payload);
            data.setUnixTime(dateTime);
            // async work with queue in another thread
            producerExecutorService.submit(() -> {
                try {
                    System.out.println(Thread.currentThread().getName() + " Publishing request data into queue....");
                    queue.put(data);
                } catch (InterruptedException e) {
                    System.err.println("Failed to save request data in queue. " + data.toString() +
                            "Exception: " + e.getMessage());
                }
            });
            resp.setStatus(HttpServletResponse.SC_OK);
        } catch (Exception e) {
            System.out.println("Client: " + req.getRemoteAddr() + " sent bad request");
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    @Override
    public void destroy() {
        consumerExecutorService.shutdown();
        producerExecutorService.shutdown();
    }
}
