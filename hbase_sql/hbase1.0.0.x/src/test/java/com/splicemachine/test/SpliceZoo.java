/*
 * Copyright (c) 2012 - 2017 Splice Machine, Inc.
 *
 * This file is part of Splice Machine.
 * Splice Machine is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Affero General Public License as published by the Free Software Foundation, either
 * version 3, or (at your option) any later version.
 * Splice Machine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 * You should have received a copy of the GNU Affero General Public License along with Splice Machine.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package com.splicemachine.test;

import com.splicemachine.utils.SpliceLogUtils;
import org.apache.log4j.Logger;
import org.apache.zookeeper.server.DatadirCleanupManager;
import org.apache.zookeeper.server.ServerCnxnFactory;
import org.apache.zookeeper.server.ZKDatabase;
import org.apache.zookeeper.server.persistence.FileTxnSnapLog;
import org.apache.zookeeper.server.quorum.QuorumPeer;
import org.apache.zookeeper.server.quorum.QuorumPeerConfig;

import java.io.File;
import java.io.IOException;

public class SpliceZoo implements Runnable {
	private static final Logger LOG = Logger.getLogger(SpliceZoo.class);
	protected QuorumPeerConfig config;
	protected QuorumPeer peer;
	public SpliceZoo(QuorumPeerConfig config, int number) throws IOException {
		this.config = config;
		ServerCnxnFactory cnxnFactory = ServerCnxnFactory.createFactory();
		cnxnFactory.configure(config.getClientPortAddress(),config.getMaxClientCnxns());

		peer = new QuorumPeer(config.getServers(), null, null, config.getElectionAlg(),
				number, config.getTickTime(), config.getInitLimit(), config.getSyncLimit(), cnxnFactory);

		peer.setClientPortAddress(config.getClientPortAddress());
		peer.setTxnFactory(new FileTxnSnapLog(new File(config.getDataLogDir()),
                     new File(config.getDataDir())));
		peer.setQuorumPeers(config.getServers());
		peer.setElectionType(config.getElectionAlg());
		peer.setMyid(config.getServerId());
		peer.setTickTime(config.getTickTime());
		peer.setMinSessionTimeout(config.getMinSessionTimeout());
		peer.setMaxSessionTimeout(config.getMaxSessionTimeout());
		peer.setInitLimit(config.getInitLimit());
		peer.setSyncLimit(config.getSyncLimit());
		peer.setQuorumVerifier(config.getQuorumVerifier());
		peer.setCnxnFactory(cnxnFactory);
		peer.setZKDatabase(new ZKDatabase(peer.getTxnFactory()));
		peer.setLearnerType(config.getPeerType());
		peer.setMyid(number);

	}

	@Override
	public void run() {
        DatadirCleanupManager purgeMgr = new DatadirCleanupManager(config
                .getDataDir(), config.getDataLogDir(), config
                .getSnapRetainCount(), config.getPurgeInterval());
        purgeMgr.start();
        SpliceLogUtils.trace(LOG, "Client Address: %s",config.getClientPortAddress());
		try {
			 peer.start();
		     SpliceLogUtils.trace(LOG, "Attempting to Join: %s",config.getClientPortAddress());
			 peer.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
