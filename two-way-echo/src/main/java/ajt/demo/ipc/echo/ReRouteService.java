package ajt.demo.ipc.echo;

import io.aeron.Aeron;
import io.aeron.Publication;
import io.aeron.Subscription;
import io.aeron.logbuffer.FragmentHandler;
import org.agrona.concurrent.Agent;
import org.agrona.concurrent.UnsafeBuffer;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Deque;

public class ReRouteService implements Agent, AutoCloseable {

    private static final int FRAGMENT_LIMIT = 1;
    private static final int MAX_PENDING_RESPONSES = 1024;

    private final Aeron aeron;
    private final String channel;
    private Subscription sub;
    private Publication pub;
    private UnsafeBuffer unsafeBuffer;
    private final Deque<String> pendingResponses = new ArrayDeque<>();
    private final FragmentHandler handler = (buffer, offset, length, header) -> {
        String payload = buffer.getStringAscii(offset);
        System.out.println("received:" + payload);
        pendingResponses.addLast(payload + payload);
    };

    public ReRouteService(Aeron aeron, String channel) {
        this.aeron = aeron;
        this.channel = channel;
    }

    @Override
    public void onStart() {
        // create subscriber to channel 2
        sub = aeron.addSubscription(channel, 1);
        // create publisher to channel 1
        pub = aeron.addPublication(channel, 2);
        // create message to send body
        unsafeBuffer = new UnsafeBuffer(ByteBuffer.allocate(256));
    }

    @Override
    public void onClose() {
        if (sub != null) {
            sub.close();
        }
        if (pub != null) {
            pub.close();
        }
    }

    @Override
    public int doWork() {
        int workCount = 0;
        System.out.println("Inside ReRouteService");
        final String nextResponse = pendingResponses.peekFirst();
        if (nextResponse != null) {
            unsafeBuffer.putStringAscii(0, nextResponse);
            if (pub.offer(unsafeBuffer) > 0) {
                pendingResponses.removeFirst();
                workCount++;
            }
            else {
                // Back pressured: return so AgentRunner idle strategy can decide next step.
                return workCount;
            }
        }

        // Avoid unbounded growth by pausing intake until outbound backlog drains.
        if (pendingResponses.size() < MAX_PENDING_RESPONSES) {
            workCount += sub.poll(handler, FRAGMENT_LIMIT);
        }
        return workCount;
    }

    @Override
    public String roleName() {
        return "reroute-service";
    }

    @Override
    public void close() {
        onClose();
    }
}
