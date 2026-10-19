package ajt.demo.ipc.echo;

import io.aeron.Aeron;
import io.aeron.Publication;
import io.aeron.Subscription;
import io.aeron.driver.MediaDriver;
import io.aeron.logbuffer.FragmentHandler;
import org.agrona.CloseHelper;
import org.agrona.concurrent.AgentRunner;
import org.agrona.concurrent.IdleStrategy;
import org.agrona.concurrent.SleepingIdleStrategy;
import org.agrona.concurrent.UnsafeBuffer;

import java.nio.ByteBuffer;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main2 {
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
        final UnsafeBuffer unsafeBuffer = new UnsafeBuffer(ByteBuffer.allocate(256));
        // set up driver config
        try (MediaDriver driver = MediaDriver.launch();
             // start aeron
             Aeron aeron = Aeron.connect();
             // create subscriber to channel 2
             Subscription sub = aeron.addSubscription(channel, 2);
             // create publisher to channel 1
             Publication pub = aeron.addPublication(channel, 1)){

            // Start the middle service first so stream 1 has an active subscriber.
            var reRouteService = new ReRouteService(aeron, channel);
            AgentRunner reRouteServiceRunner = new AgentRunner(
                    idle,
                    Throwable::printStackTrace,
                    null,
                    reRouteService);
            AgentRunner.startOnThread(reRouteServiceRunner);

            while (!pub.isConnected()) {
                idle.idle();
            }
            // prep message
            unsafeBuffer.putStringAscii(0, message);
            System.out.println("preparing message to send in channel 1:" + message);

            // send from initial publisher & start timer
            System.out.println("sending:" + message);
            final long startTime = System.currentTimeMillis();
            while (pub.offer(unsafeBuffer) < 0) {
                idle.idle();
            }
            final long[] endTime = {startTime};
            FragmentHandler handler = (buffer, offset, length, header) -> {
                // print result
                System.out.println("received:" + buffer.getStringAscii(offset));
                endTime[0] = System.currentTimeMillis();
            };
            // consumer from final subscriber
            while (sub.poll(handler, 1) <= 0) {
                idle.idle();
            }

            System.out.println("Process time: " + (endTime[0] - startTime) + "ms");
            CloseHelper.quietClose(reRouteServiceRunner);
        }
    }
}