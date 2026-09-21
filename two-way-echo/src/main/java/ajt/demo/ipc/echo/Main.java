package ajt.demo.ipc.echo;

import io.aeron.Aeron;
import io.aeron.Publication;
import io.aeron.Subscription;
import io.aeron.driver.MediaDriver;
import io.aeron.logbuffer.FragmentHandler;
import org.agrona.concurrent.IdleStrategy;
import org.agrona.concurrent.SleepingIdleStrategy;
import org.agrona.concurrent.UnsafeBuffer;

import java.nio.ByteBuffer;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    /*
    Send an initial message of a value
    and publish a true or false whether it is odd or even
    with logic held in a different service
     */
    public static void main(String[] args) {
        // set channel type
        final String channel = "aeron:ipc";
        // set message
        final String message = "my message";
        // set up idle strategy
        final IdleStrategy idle = new SleepingIdleStrategy();
        // set up buffer to contain message to send
        final UnsafeBuffer unsafeBuffer1 = new UnsafeBuffer(ByteBuffer.allocate(256));
        final UnsafeBuffer unsafeBuffer2 = new UnsafeBuffer(ByteBuffer.allocate(256));
        // set up driver config
        System.out.println("Starting Media Driver");
        try (MediaDriver driver = MediaDriver.launch();
             // start aeron
             Aeron aeron = Aeron.connect();
             // create subscriber to channel 2
             Subscription sub1 = aeron.addSubscription(channel, 1);
             Subscription sub2 = aeron.addSubscription(channel, 2);
             // create publisher to channel 1
             Publication pub1 = aeron.addPublication(channel, 1);
             Publication pub2 = aeron.addPublication(channel, 2)) {
            System.out.println("Waiting for connection to channel 1 and channel 2");
            while (!pub1.isConnected() || !pub2.isConnected()) {
                idle.idle();
            }
            // prep message
            System.out.println("preparing message to send in channel 1:" + message);
            unsafeBuffer1.putStringAscii(0, message);
            // create middle service that subscribes to channel 1 and publishes to channel 2
            // send from initial publisher & start timer
            System.out.println("sending in channel 1:" + message);
            while (pub1.offer(unsafeBuffer1) < 0) {
                idle.idle();
            }
            System.out.println("starting timer");
            long startTime = System.currentTimeMillis();

            FragmentHandler handler1 = (buffer, offset, length, header) -> {
                // print result
                String payload = buffer.getStringAscii(offset);
                System.out.println("received:" + payload);
                unsafeBuffer2.putStringAscii(0, payload + payload);
                while (pub2.offer(unsafeBuffer2) < 0) {
                    idle.idle();
                }
            };
            // consumer from final subscriber
            System.out.println("waiting to read message from channel 1 and publish channel 2");
            while (sub1.poll(handler1, 1) <= 0) {
                idle.idle();
            }

            FragmentHandler handler2 = (buffer, offset, length, header) -> {
                // print result
                System.out.println("received:" + buffer.getStringAscii(offset));
            };
            System.out.println("waiting to read message from channel 2");
            while (sub2.poll(handler2, 1) <= 0) {
                idle.idle();
            }
            long endtime = System.currentTimeMillis();

            System.out.println(String.format("Process time: %sms", endtime - startTime));
        }
    }
}