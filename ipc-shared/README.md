either run the sh file in resources, or the following mvn command in the terminal:

```bash
mvn -DskipTests package && mvn exec:java -Dexec.mainClass=ipc.mediaDriver.shared.Main
```

without jvm.config, the command would be
```bash
mvn -DskipTests package && mvn exec:java -Dexec.mainClass=ipc.mediaDriver.shared.Main -Dexec.vmArgs="--add-opens=java.base/sun.nio.ch=ALL-UNNAMED"
```