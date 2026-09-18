package ajt.ipc.basic;

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
    public static void main(String[] args) {
        // https://github.com/aeron-io/aeron/wiki/Channel-Configuration
        final String channel = "aeron:ipc";
        final String message = "my message";
        // https://aeron.io/docs/agrona/agents-idle-strategies/#idle-strategies
        final IdleStrategy idle = new SleepingIdleStrategy();
        // https://aeron.io/docs/agrona/direct-buffer/
        final UnsafeBuffer unsafeBuffer = new UnsafeBuffer(ByteBuffer.allocate(256));
        try (MediaDriver driver = MediaDriver.launch();
             Aeron aeron = Aeron.connect();
             // Subscription which will be polled in order to receive message
             Subscription sub = aeron.addSubscription(channel, 10);
             // Publication from which the message will be sent
             Publication pub = aeron.addPublication(channel, 10)) {
            while (!pub.isConnected()) {
                idle.idle();
            }
            unsafeBuffer.putStringAscii(0, message);
            System.out.println("sending:" + message);
            // When the value returned is less than zero, something is preventing the publication from accepting the buffer.
            // The idle strategy polls until it is accepted
            while (pub.offer(unsafeBuffer) < 0) {
                idle.idle();
            }
            FragmentHandler handler = (buffer, offset, length, header) ->
                    System.out.println("received:" + buffer.getStringAscii(offset));
            while (sub.poll(handler, 1) <= 0) {
                idle.idle();
            }
        }
    }

}
