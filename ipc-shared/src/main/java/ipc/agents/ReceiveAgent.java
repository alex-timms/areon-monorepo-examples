package ipc.agents;

import io.aeron.Subscription;
import io.aeron.logbuffer.Header;
import org.agrona.DirectBuffer;
import org.agrona.concurrent.Agent;
import org.agrona.concurrent.ShutdownSignalBarrier;

public class ReceiveAgent implements Agent {
    private final Subscription subscription;
    private final ShutdownSignalBarrier barrier;
    private final int sendCount;

    /**
     * Creates a receiver that listens for counter messages until the final value arrives.
     *
     * @param subscription the Aeron subscription used to read messages
     * @param barrier the shutdown barrier to signal once all messages are received
     * @param sendCount the final counter value expected from the sender
     */
    public ReceiveAgent(final Subscription subscription, ShutdownSignalBarrier barrier, int sendCount) {
        this.subscription = subscription;
        this.barrier = barrier;
        this.sendCount = sendCount;
    }

    /**
     * Polls the subscription for incoming fragments and dispatches them to the handler.
     *
     * @return always returns {@code 0} because no work count is tracked
     * @throws Exception if polling fails
     */
    @Override
    public int doWork() throws Exception {
        subscription.poll(this::handler, 1000);
        return 0;
    }

    /**
     * Checks the received counter value and signals shutdown after the last expected message.
     *
     * @param buffer the buffer containing the received fragment
     * @param offset the offset in the buffer where the fragment starts
     * @param length the fragment length in bytes
     * @param header the Aeron header associated with the fragment
     */
    private void handler(DirectBuffer buffer, int offset, int length, Header header) {
        final int lastValue = buffer.getInt(offset);
        if (lastValue >= sendCount) {
            barrier.signal();
        }
    }

    /**
     * Returns the logical name of this agent.
     *
     * @return the receiver role name
     */
    @Override
    public String roleName() {
        return "receiver";
    }
}
