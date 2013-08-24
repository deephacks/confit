/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.deephacks.confit.internal.hbase;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.PleaseHoldException;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.HBaseAdmin;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.RetriesExhaustedException;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.master.HMaster;
import org.apache.hadoop.hbase.master.HMasterCommandLine;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.deephacks.confit.test.JUnitUtils.getMavenProjectChildFile;

public class HBaseUtil {
    private static final Configuration conf = HBaseConfiguration.create();
    private static final String ZOOKEEPER_DATA_DIR_PROP = "hbase.zookeeper.property.dataDir";
    private static final File ZOOKEEPER_DATA_DIR = getMavenProjectChildFile(HBaseUtil.class, "target/zk");
    private static final String HBASE_ROOT_DIR_PROP = "hbase.rootdir";
    private static final String HBASE_TMP_DIR_PROP = "hbase.tmp.dir";
    private static final File HBASE_DATA_DIR = getMavenProjectChildFile(HBaseUtil.class, "target/hbase");
    /**
     * Use this to create HBaseBeanManager tables manually. A standalone
     * HBase master (or a distributed cluster) must be running locally.
     *
     * Check that /etc/hosts is configured correctly:
     *
     * 127.0.0.1 localhost
     * 127.0.0.1 <hostname>
     *
     * Check Master status on http://localhost:60010/master-status.
     */
    public static void main(String[] args) {
        HBaseUtil.createtables();
    }

    public static HBaseBeanManager getLocalTestManager() {
        try {
            ExecutorService service = Executors.newCachedThreadPool();
            service.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        HMasterCommandLine master = new HMasterCommandLine(HMaster.class);
                        File hbaseDir = recreateDir(HBASE_DATA_DIR);
                        File zkDir = recreateDir(ZOOKEEPER_DATA_DIR);
                        conf.set(HBASE_TMP_DIR_PROP, hbaseDir.getAbsolutePath());
                        conf.set(HBASE_ROOT_DIR_PROP, hbaseDir.getAbsolutePath());
                        conf.set(ZOOKEEPER_DATA_DIR_PROP, zkDir.getAbsolutePath());
                        master.setConf(conf);
                        master.run(new String[] { "start" });
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            });
            createtables();
            return new HBaseBeanManager(conf);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void deleteAllRows() throws Exception {
        HTable table = HBeanTable.getBeanTable(conf);
        Scan scan = new Scan();
        ResultScanner scanner = table.getScanner(scan);
        for (Result result : scanner) {
            Delete del = new Delete(result.getRow());
            table.delete(del);
        }
    }

    public static void createtables() {
        while (true) {
            try {
                recreateTable(HBeanRow.BEAN_TABLE, new byte[][] { HBeanRow.DUMMY_COLUMN_FAMILY,
                        HBeanRow.PROP_COLUMN_FAMILY, HBeanRow.REF_COLUMN_FAMILY,
                        HBeanRow.PRED_COLUMN_FAMILY, HBeanRow.SINGLETON_COLUMN_FAMILY,
                        HBeanKeyValue.BEAN_COLUMN_FAMILY });
                recreateTable(HBeanRow.IID_TABLE, new byte[][] { UniqueId.UID_ID_FAMILY,
                        UniqueId.UID_NAME_FAMILY });

                recreateTable(HBeanRow.SID_TABLE, new byte[][] { UniqueId.UID_ID_FAMILY,
                        UniqueId.UID_NAME_FAMILY });

                recreateTable(HBeanRow.PID_TABLE, new byte[][] { UniqueId.UID_ID_FAMILY,
                        UniqueId.UID_NAME_FAMILY });
                return;
            } catch (PleaseHoldException e) {
                hold(1000);
                continue;
            } catch (RetriesExhaustedException e) {
                hold(1000);
                continue;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static void hold(long time) {
        try {
            System.out.println("Please hold");
            Thread.sleep(1000);
        } catch (InterruptedException e1) {

        }
    }

    private static void recreateTable(final byte[] tableName, final byte[][] columns)
            throws Exception {
        System.out.println("Creating table " + new String(tableName));
        final HBaseAdmin admin = new HBaseAdmin(conf);

        try {
            admin.disableTable(tableName);
        } catch (Exception e) {
            // ignore
        }
        try {
            admin.deleteTable(tableName);
        } catch (Exception e) {
            // ignore
        }

        HTableDescriptor desc = new HTableDescriptor(tableName);
        for (int i = 0; i < columns.length; i++) {
            HColumnDescriptor hcolumn = new HColumnDescriptor(columns[i]);
            desc.addFamily(hcolumn);
        }
        admin.createTable(desc);
        admin.close();
    }

    private static File recreateDir(File dir) {
        dir.mkdirs();
        Path path = Paths.get(dir.toURI());
        try {
            File file = Files.createTempDirectory(path, dir.getName()).toFile();
            System.out.println("Created directory " + file.getAbsolutePath());
            return file;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
