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
        final int message = 2;
        // set up idle strategu
        final IdleStrategy idle = new SleepingIdleStrategy();
        // set up buffer to contain message to send
        final UnsafeBuffer unsafeBuffer = new UnsafeBuffer(ByteBuffer.allocate(256));
        // set up driver config
        try (MediaDriver driver = MediaDriver.launch();
             // start aeron
             Aeron aeron = Aeron.connect();
             // create subscriber to channel 2
             Subscription sub = aeron.addSubscription(channel, 2);
             // create publisher to channel 1
             Publication pub = aeron.addPublication(channel, 1)) {
            while (!pub.isConnected()) {
                idle.idle();
            }
            // prep message
            unsafeBuffer.putInt(0, message);
            System.out.println("sending:" + message);
            // create middle service that subscribes to channel 1 and publishes to channel 2
            var oddEvenService = new OddEvenService(aeron, channel);
            oddEvenService.start();
            // send from initial publisher
            while (pub.offer(unsafeBuffer) < 0) {
                idle.idle();
            }
            FragmentHandler handler = (buffer, offset, length, header) ->
                    // print result
                    System.out.println("received:" + buffer.getStringAscii(offset));
            // consumer from final subscriber
            while (sub.poll(handler, 1) <= 0) {
                idle.idle();
            }
        }
    }
}