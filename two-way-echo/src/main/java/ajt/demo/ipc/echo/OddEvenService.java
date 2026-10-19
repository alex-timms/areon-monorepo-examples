package ajt.demo.ipc.echo;

import io.aeron.Aeron;
import io.aeron.Publication;
import io.aeron.Subscription;
import io.aeron.logbuffer.FragmentHandler;
import org.agrona.concurrent.Agent;
import org.agrona.concurrent.IdleStrategy;
import org.agrona.concurrent.SleepingIdleStrategy;
import org.agrona.concurrent.UnsafeBuffer;

import java.nio.ByteBuffer;

public class OddEvenService implements Agent {

    private Aeron aeron;
    private String channel;
    final IdleStrategy idle = new SleepingIdleStrategy();

    public OddEvenService(Aeron aeron, String channel) {
        this.aeron = aeron;
        this.channel = channel;
    }

    public void start() {
        // create message to send body
        final UnsafeBuffer unsafeBuffer = new UnsafeBuffer(ByteBuffer.allocate(256));
        // create subscriber to channel 2
        Subscription sub = aeron.addSubscription(channel, 1);
        // create publisher to channel 1
        Publication pub = aeron.addPublication(channel, 2);

        FragmentHandler handler = (buffer, offset, length, header) -> {
            System.out.println("received:" + buffer.getStringAscii(offset));
            int value = buffer.getInt(0);
            String state = Boolean.toString(value % 2 == 0);
            unsafeBuffer.putStringAscii(0, state);
            while (pub.offer(unsafeBuffer) < 0) {
                idle.idle();
            }
        };

        while (sub.poll(handler, 1) <= 0) {
            idle.idle();
        }
    }


    @Override
    public int doWork() throws Exception {
        return 0;
    }

    @Override
    public String roleName() {
        return "";
    }
}
