/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**********************************************************
 * DataNode is a class (and program) that stores a set of
 * blocks for a DFS deployment.  A single deployment can
 * have one or many DataNodes.  Each DataNode communicates
 * regularly with a single NameNode.  It also communicates
 * with client code and other DataNodes from time to time.
 *
 * DataNodes store a series of named blocks.  The DataNode
 * allows client code to read these blocks, or to write new
 * block data.  The DataNode may also, in response to instructions
 * from its NameNode, delete blocks or copy blocks to/from other
 * DataNodes.
 *
 * The DataNode maintains just one critical table:
 *   block-> stream of bytes (of BLOCK_SIZE or less)
 *
 * This info is stored on a local disk.  The DataNode
 * reports the table's contents to the NameNode upon startup
 * and every so often afterwards.
 *
 * DataNodes spend their lives in an endless loop of asking
 * the NameNode for something to do.  A NameNode cannot connect
 * to a DataNode directly; a NameNode simply returns values from
 * functions invoked by a DataNode.
 *
 * DataNodes maintain an open server socket so that client code 
 * or other DataNodes can read/write data.  The host/port for
 * this server is reported to the NameNode, which then sends that
 * information to clients or other DataNodes that might be interested.
 *
 **********************************************************/
package org.apache.hadoop.hdfs.server.datanode;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.hdfs.protocol.Block;
import org.apache.hadoop.hdfs.protocol.BlockListAsLongs;
import org.apache.hadoop.hdfs.protocol.ClientDatanodeProtocol;
import org.apache.hadoop.hdfs.protocol.DataTransferProtocol;
import org.apache.hadoop.hdfs.protocol.DatanodeID;
import org.apache.hadoop.hdfs.protocol.DatanodeInfo;
import org.apache.hadoop.hdfs.protocol.FSConstants;
import org.apache.hadoop.hdfs.protocol.LocatedBlock;
import org.apache.hadoop.hdfs.protocol.UnregisteredDatanodeException;
import org.apache.hadoop.hdfs.server.common.HdfsConstants.StartupOption;
import org.apache.hadoop.hdfs.server.common.HdfsConstants;
import org.apache.hadoop.hdfs.server.common.GenerationStamp;
import org.apache.hadoop.hdfs.server.common.IncorrectVersionException;
import org.apache.hadoop.hdfs.server.common.Storage;
import org.apache.hadoop.hdfs.server.datanode.metrics.DataNodeMetrics;
import org.apache.hadoop.hdfs.server.namenode.FSNamesystem;
import org.apache.hadoop.hdfs.server.namenode.FileChecksumServlets;
import org.apache.hadoop.hdfs.server.namenode.NameNode;
import org.apache.hadoop.hdfs.server.namenode.StreamFile;
import org.apache.hadoop.hdfs.server.protocol.BlockCommand;
import org.apache.hadoop.hdfs.server.protocol.BlockMetaDataInfo;
import org.apache.hadoop.hdfs.server.protocol.DatanodeCommand;
import org.apache.hadoop.hdfs.server.protocol.DatanodeProtocol;
import org.apache.hadoop.hdfs.server.protocol.DatanodeRegistration;
import org.apache.hadoop.hdfs.server.protocol.DisallowedDatanodeException;
import org.apache.hadoop.hdfs.server.protocol.InterDatanodeProtocol;
import org.apache.hadoop.hdfs.server.protocol.NamespaceInfo;
import org.apache.hadoop.hdfs.server.protocol.UpgradeCommand;
import org.apache.hadoop.http.HttpServer;
import org.apache.hadoop.io.IOUtils;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.ipc.RPC;
import org.apache.hadoop.ipc.RemoteException;
import org.apache.hadoop.ipc.Server;
import org.apache.hadoop.net.DNS;
import org.apache.hadoop.net.NetUtils;
import org.apache.hadoop.util.Daemon;
import org.apache.hadoop.util.DiskChecker;
import org.apache.hadoop.util.ReflectionUtils;
import org.apache.hadoop.util.StringUtils;
import org.apache.hadoop.util.DiskChecker.DiskErrorException;
import org.apache.hadoop.util.DiskChecker.DiskOutOfSpaceException;

class DataTransfer implements Runnable {
    DatanodeInfo targets[];
    Block b;
    DataNode datanode;

    /**
     * Connect to the first item in the target list.  Pass along the 
     * entire target list, the block, and the data.
     */
    public DataTransfer(DatanodeInfo targets[], Block b, DataNode datanode) throws IOException {
      this.targets = targets;
      this.b = b;
      this.datanode = datanode;
    }

    /**
     * Do the deed, write the bytes
     */
    public void run() {
      xmitsInProgress++;
      Socket sock = null;
      DataOutputStream out = null;
      BlockSender blockSender = null;
      
      try {
        InetSocketAddress curTarget = 
          NetUtils.createSocketAddr(targets[0].getName());
        sock = newSocket();
        sock.connect(curTarget, socketTimeout);
        sock.setSoTimeout(targets.length * socketTimeout);

        long writeTimeout = socketWriteTimeout + 
                            HdfsConstants.WRITE_TIMEOUT_EXTENSION * (targets.length-1);
        OutputStream baseStream = NetUtils.getOutputStream(sock, writeTimeout);
        out = new DataOutputStream(new BufferedOutputStream(baseStream, 
                                                            SMALL_BUFFER_SIZE));

        blockSender = new BlockSender(b, 0, -1, false, false, false, 
            datanode);
        DatanodeInfo srcNode = new DatanodeInfo(dnRegistration);

        //
        // Header info
        //
        out.writeShort(DataTransferProtocol.DATA_TRANSFER_VERSION);
        out.writeByte(DataTransferProtocol.OP_WRITE_BLOCK);
        out.writeLong(b.getBlockId());
        out.writeLong(b.getGenerationStamp());
        out.writeInt(0);           // no pipelining
        out.writeBoolean(false);   // not part of recovery
        Text.writeString(out, ""); // client
        out.writeBoolean(true); // sending src node information
        srcNode.write(out); // Write src node DatanodeInfo
        // write targets
        out.writeInt(targets.length - 1);
        for (int i = 1; i < targets.length; i++) {
          targets[i].write(out);
        }
        // send data & checksum
        blockSender.sendBlock(out, baseStream, null);

        // no response necessary
        LOG.info(dnRegistration + ":Transmitted block " + b + " to " + curTarget);

      } catch (IOException ie) {
        LOG.warn(dnRegistration + ":Failed to transfer " + b + " to " + targets[0].getName()
            + " got " + StringUtils.stringifyException(ie));
      } finally {
        IOUtils.closeStream(blockSender);
        IOUtils.closeStream(out);
        IOUtils.closeSocket(sock);
        xmitsInProgress--;
      }
    }
  }