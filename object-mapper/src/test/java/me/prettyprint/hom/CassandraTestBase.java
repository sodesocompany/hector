package me.prettyprint.hom;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import me.prettyprint.cassandra.service.ThriftCfDef;
import me.prettyprint.cassandra.service.ThriftKsDef;
import me.prettyprint.hector.api.Cluster;

import org.apache.cassandra.contrib.utils.service.CassandraServiceDataCleaner;
import org.apache.cassandra.io.util.FileUtils;
import org.apache.cassandra.service.EmbeddedCassandraService;
import org.apache.cassandra.thrift.CfDef;
import org.apache.cassandra.thrift.KsDef;
import org.apache.thrift.transport.TTransportException;

public class CassandraTestBase {
    private static boolean cassandraStarted = false;

    public static void startCassandraInstance(String pathToDataDir) throws TTransportException, IOException,
            InterruptedException, SecurityException, IllegalArgumentException, NoSuchMethodException,
            IllegalAccessException, InvocationTargetException {
        if (cassandraStarted) {
            return;
        }

        try {
            FileUtils.deleteRecursive(new File(pathToDataDir));
        }
        catch (AssertionError e) {
            // eat
        }
        catch (IOException e) {
            // eat
        }

        CassandraServiceDataCleaner cleaner = new CassandraServiceDataCleaner();
        cleaner.prepare();
        EmbeddedCassandraService cassandra = new EmbeddedCassandraService();
        try {
            cassandra.init();
        }
        catch (TTransportException e) {
            throw e;
        }

        cassandraStarted = true;

        Thread t = new Thread(cassandra);
        t.setName(cassandra.getClass().getSimpleName());
        t.setDaemon(true);
        t.start();
        // Thread.sleep(1000);
    }

    public static void createKeyspace(Cluster cluster, String name, String strategy, int replicationFactor,
            List<CfDef> cfDefList) {
        try {
            KsDef ksDef = new KsDef(name, strategy, replicationFactor, cfDefList);
            cluster.addKeyspace(new ThriftKsDef(ksDef));
            return;
        }
        catch (Throwable e) {
            System.out.println("exception while creating keyspace, " + name + " - probably already exists : "
                    + e.getMessage());
        }

        for (CfDef cfDef : cfDefList) {
            try {
                cluster.addColumnFamily(new ThriftCfDef(cfDef));
            }
            catch (Throwable e) {
                System.out.println("exception while creating CF, " + cfDef.getName() + " - probably already exists : "
                        + e.getMessage());
            }
        }
    }
}
