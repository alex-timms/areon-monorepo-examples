package ipc.mediaDriver.shared;

import io.aeron.Aeron;
import io.aeron.Publication;
import io.aeron.Subscription;
import io.aeron.driver.MediaDriver;
import io.aeron.driver.ThreadingMode;
import ipc.agents.ReceiveAgent;
import ipc.agents.SendAgent;
import org.agrona.concurrent.AgentRunner;
import org.agrona.concurrent.BusySpinIdleStrategy;
import org.agrona.concurrent.IdleStrategy;
import org.agrona.concurrent.ShutdownSignalBarrier;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {

        final String channel = "aeron:ipc";
        final int streamId = 10;
        final int sendCount = 1000000;
        final IdleStrategy idleStrategySend = new BusySpinIdleStrategy();
        final IdleStrategy idleStrategyReceive = new BusySpinIdleStrategy();
        final ShutdownSignalBarrier barrier = new ShutdownSignalBarrier();

        //Step 1: Construct Media Driver, cleaning up media driver folder on start/stop
        final MediaDriver.Context mediaDriverCtx = new MediaDriver.Context()
                // the driver attempt to immediately delete aeronDirectoryName() on startup
                .dirDeleteOnStart(true)
                // One thread shared by the Sender and Receiver agents and the DriverConductor.
                .threadingMode(ThreadingMode.SHARED)
                // to do
                .sharedIdleStrategy(new BusySpinIdleStrategy())
                // the driver attempt to delete aeronDirectoryName() on shutdown
                .dirDeleteOnShutdown(true);
        // Launch an isolated MediaDriver embedded in the current process
        final MediaDriver mediaDriver = MediaDriver.launchEmbedded(mediaDriverCtx);

        //Step 2: Construct Aeron, pointing at the media driver's folder
        final Aeron.Context aeronCtx = new Aeron.Context()
                .aeronDirectoryName(mediaDriver.aeronDirectoryName());
        final Aeron aeron = Aeron.connect(aeronCtx);

        //Step 3: Construct the subs and pubs
        final Subscription subscription = aeron.addSubscription(channel, streamId);
        final Publication publication = aeron.addPublication(channel, streamId);

        //Step 4: Construct the agents
        final SendAgent sendAgent = new SendAgent(publication, sendCount);
        final ReceiveAgent receiveAgent = new ReceiveAgent(subscription, barrier,
                sendCount);

        //Step 5: Construct agent runners
        final AgentRunner sendAgentRunner = new AgentRunner(idleStrategySend,
                Throwable::printStackTrace, null, sendAgent);
        final AgentRunner receiveAgentRunner = new AgentRunner(idleStrategyReceive,
                Throwable::printStackTrace, null, receiveAgent);

        System.out.println("starting");

        //Step 6: Start the runners
        AgentRunner.startOnThread(sendAgentRunner);
        System.out.println("send agent started");

        long startTime = System.currentTimeMillis();
        AgentRunner.startOnThread(receiveAgentRunner);
        System.out.println("receive agent started");

        //wait for the final item to be received before closing
        System.out.println("waiting for barrier");
        barrier.await();
        System.out.println("barrier released");
        long endTime = System.currentTimeMillis();
        System.out.println(String.format("Process time: %sms", endTime - startTime));

        //close the resources
        receiveAgentRunner.close();
        sendAgentRunner.close();

        aeron.close();
        mediaDriver.close();
    }

}
