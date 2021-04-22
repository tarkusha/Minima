/*
 * Copyright 2020 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.minima.system.network.base.peer;

import java.util.List;
import java.util.function.Supplier;
// import tech.pegasys.teku.networking.p2p.discovery.DiscoveryPeer;
// import tech.pegasys.teku.networking.p2p.network.P2PNetwork;
// import tech.pegasys.teku.networking.p2p.network.PeerAddress;
// import tech.pegasys.teku.networking.p2p.peer.Peer;

//import org.minima.system.network.base.DiscoveryPeer;
import org.minima.system.network.base.P2PNetwork;

public interface PeerSelectionStrategy {
  List<PeerAddress> selectPeersToConnect(
      P2PNetwork<?> network, PeerPools peerPools, Supplier<List<DiscoveryPeer>> candidates);

  List<Peer> selectPeersToDisconnect(P2PNetwork<?> network, PeerPools peerPools);
}