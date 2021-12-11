/*
* MIT License
*
* Copyright (c) 2021 MyBerry. All rights reserved.
* https://myberry.org/
*
* Permission is hereby granted, free of charge, to any person obtaining a copy
* of this software and associated documentation files (the "Software"), to deal
* in the Software without restriction, including without limitation the rights
* to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
* copies of the Software, and to permit persons to whom the Software is
* furnished to do so, subject to the following conditions:

*   * Redistributions of source code must retain the above copyright notice, this
* list of conditions and the following disclaimer.

*   * Redistributions in binary form must reproduce the above copyright notice,
* this list of conditions and the following disclaimer in the documentation
* and/or other materials provided with the distribution.

*   * Neither the name of MyBerry. nor the names of its contributors may be used
* to endorse or promote products derived from this software without specific
* prior written permission.

* The above copyright notice and this permission notice shall be included in all
* copies or substantial portions of the Software.

* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
* IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
* FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
* AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
* LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
* OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
* SOFTWARE.
*/
package org.myberry.console.service.impl;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Resource;
import org.myberry.client.admin.DefaultAdminClient;
import org.myberry.client.admin.SendResult;
import org.myberry.client.admin.SendStatus;
import org.myberry.common.Component;
import org.myberry.common.protocol.body.admin.CRComponentData;
import org.myberry.common.protocol.body.admin.ClusterListData.ClusterDatabase;
import org.myberry.common.protocol.body.admin.ClusterListData.ClusterDatabase.ClusterBlock;
import org.myberry.common.protocol.body.admin.ClusterListData.ClusterRoute;
import org.myberry.common.protocol.body.admin.NSComponentData;
import org.myberry.common.route.NodeState;
import org.myberry.common.route.NodeType;
import org.myberry.common.structure.Structure;
import org.myberry.console.exception.CalloutException;
import org.myberry.console.service.PreviewService;
import org.myberry.console.vo.BlockVO;
import org.myberry.console.vo.ClusterStatusVO;
import org.myberry.console.vo.ComponentManagementVO;
import org.myberry.console.vo.ComponentVO;
import org.myberry.console.vo.ConsoleVO;
import org.myberry.console.vo.RouteVO;
import org.myberry.console.vo.WeightVO;
import org.springframework.stereotype.Service;

@Service("previewService")
public class PreviewServiceImpl implements PreviewService {

  private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

  @Resource private DefaultAdminClient defaultAdminClient;

  @Override
  public ConsoleVO console() throws Exception {
    SendResult sendResult = defaultAdminClient.queryClusterList();

    ClusterStatusVO clusterStatusVO =
        calcClusterStatus(sendResult.getClusterList().getClusterRouteList());
    int leader = getLeaderSid(sendResult.getClusterList().getClusterRouteList());
    boolean cyncResult =
        calcSyncResult(sendResult.getClusterList().getClusterDatabaseList(), leader);
    clusterStatusVO.setSyncData(cyncResult);

    ComponentManagementVO componentManagementVO = getTotalComponents();

    List<RouteVO> routeList =
        getRouteList(leader, sendResult.getClusterList().getClusterRouteList());

    ConsoleVO consoleVO = new ConsoleVO();
    consoleVO.setClusterStatus(clusterStatusVO);
    consoleVO.setComponentManagement(componentManagementVO);
    consoleVO.setRouteList(routeList);
    return consoleVO;
  }

  @Override
  public ComponentVO queryComponentByKey(String key) throws Exception {
    SendResult sendResult = defaultAdminClient.queryComponentByKey(key.trim());
    if (SendStatus.SEND_OK == sendResult.getSendStatus()) {
      return buildComponentVO(sendResult.getComponent());
    } else if (SendStatus.KEY_NOT_EXISTED == sendResult.getSendStatus()) {
      throw new CalloutException(String.format("Key: [%s] not existed", key));
    } else {
      throw new CalloutException(String.format("Unknown status [%s]", sendResult.getSendStatus()));
    }
  }

  @Override
  public ComponentManagementVO componentCreate(ComponentVO componentReq) throws Exception {
    Component component = buildComponent(componentReq);
    SendResult sendResult = defaultAdminClient.createComponent(component);
    if (SendStatus.SEND_OK == sendResult.getSendStatus()) {
      return getTotalComponents();
    } else if (SendStatus.KEY_EXISTED == sendResult.getSendStatus()) {
      throw new CalloutException(String.format("Key: [%s] existed", componentReq.getKey()));
    } else if (SendStatus.UNKNOWN_STRUCTURE == sendResult.getSendStatus()) {
      throw new CalloutException(
          String.format("Unknown structure [%d]", componentReq.getStructure()));
    } else if (SendStatus.PARAMETER_LENGTH_TOO_LONG == sendResult.getSendStatus()) {
      throw new CalloutException("Args too long");
    } else if (SendStatus.INVALID_EXPRESSION == sendResult.getSendStatus()) {
      throw new CalloutException("Expression invalid");
    } else {
      throw new CalloutException(String.format("Unknown status [%s]", sendResult.getSendStatus()));
    }
  }

  @Override
  public ComponentVO componentUpdate(ComponentVO componentReq) throws Exception {
    NSComponentData nscd = (NSComponentData) buildComponent(componentReq);
    SendResult sendResult = defaultAdminClient.updateComponent(nscd);
    if (SendStatus.SEND_OK == sendResult.getSendStatus()) {
      return queryComponentByKey(componentReq.getKey());
    } else if (SendStatus.KEY_NOT_EXISTED == sendResult.getSendStatus()) {
      throw new CalloutException(String.format("Key: [%s] not existed", componentReq.getKey()));
    } else if (SendStatus.UNKNOWN_STRUCTURE == sendResult.getSendStatus()) {
      throw new CalloutException(
          String.format("Unknown structure [%d]", componentReq.getStructure()));
    } else {
      throw new CalloutException(String.format("Unknown status [%s]", sendResult.getSendStatus()));
    }
  }

  @Override
  public WeightVO weightUpdate(WeightVO weightReq) throws Exception {
    SendResult sendResult =
        defaultAdminClient.updateWeight(weightReq.getSid(), weightReq.getWeight());
    if (SendStatus.SEND_OK == sendResult.getSendStatus()) {
      WeightVO weightResp = new WeightVO();
      weightResp.setSid(sendResult.getRoute().getSid());
      weightResp.setWeight(sendResult.getRoute().getWeight());
      return weightResp;
    } else {
      throw new CalloutException(String.format("Unknown status [%s]", sendResult.getSendStatus()));
    }
  }

  @Override
  public List<BlockVO> blockQuery(int sid) throws Exception {
    SendResult sendResult = defaultAdminClient.queryClusterList();
    if (SendStatus.SEND_OK == sendResult.getSendStatus()) {
      return buildComponentVO(sid, sendResult.getClusterList().getClusterDatabaseList());
    } else {
      throw new CalloutException(String.format("Unknown status [%s]", sendResult.getSendStatus()));
    }
  }

  @Override
  public void nodeKickOut(int sid) throws Exception {
    defaultAdminClient.kickOutInvoker(sid);
  }

  @Override
  public void nodeRemove(int sid) throws Exception {
    defaultAdminClient.removeInvoker(sid);
  }

  private ClusterStatusVO calcClusterStatus(List<ClusterRoute> clusterRouteList) {
    int totalNodes = clusterRouteList.size();
    int leaderNodes = 0;
    int learnerNodes = 0;
    int lostNodes = 0;

    for (ClusterRoute clusterRoute : clusterRouteList) {
      if (NodeState.LOST.getCode() != clusterRoute.getNodeState()) {
        if (NodeType.LEADING_NAME.equals(clusterRoute.getType())) {
          leaderNodes++;
        } else if (NodeType.LEARNING_NAME.equals(clusterRoute.getType())) {
          learnerNodes++;
        }
      } else {
        lostNodes++;
      }
    }

    ClusterStatusVO clusterStatusVO = new ClusterStatusVO();
    clusterStatusVO.setTotalNodes(totalNodes);
    clusterStatusVO.setLeaderNodes(leaderNodes);
    clusterStatusVO.setLearnerNodes(learnerNodes);
    clusterStatusVO.setLostNodes(lostNodes);
    return clusterStatusVO;
  }

  private boolean calcSyncResult(List<ClusterDatabase> clusterDatabaseList, int leader) {
    List<ClusterBlock> leaderBlockList = null;
    for (ClusterDatabase clusterDatabase : clusterDatabaseList) {
      if (clusterDatabase.getSid() == leader) {
        leaderBlockList = clusterDatabase.getBlockList();
      }
    }
    if (leaderBlockList == null) {
      return false;
    }

    boolean result = true;
    for (ClusterDatabase clusterDatabase : clusterDatabaseList) {
      if (clusterDatabase.getSid() == leader) {
        continue;
      } else {
        if (clusterDatabase.getBlockList().size() == leaderBlockList.size()) {
          for (int i = 0; i < leaderBlockList.size(); i++) {
            ClusterBlock leaderBlock = leaderBlockList.get(i);
            ClusterBlock learnerBlock = clusterDatabase.getBlockList().get(i);
            if (leaderBlock.getBlockIndex() != learnerBlock.getBlockIndex()
                || leaderBlock.getComponentCount() != learnerBlock.getComponentCount()
                || leaderBlock.getBeginPhyOffset() != learnerBlock.getBeginPhyOffset()
                || leaderBlock.getEndPhyOffset() != learnerBlock.getEndPhyOffset()) {
              result = false;
              break;
            }
          }

        } else {
          result = false;
          break;
        }
      }
    }

    return result;
  }

  private List<RouteVO> getRouteList(int leader, List<ClusterRoute> clusterRouteList) {
    RouteVO[] routeArray = new RouteVO[clusterRouteList.size()];

    int i = 1;
    for (ClusterRoute clusterRoute : clusterRouteList) {
      RouteVO routeVO = new RouteVO();
      routeVO.setSid(clusterRoute.getSid());
      routeVO.setType(clusterRoute.getType());
      routeVO.setIp(clusterRoute.getIp());
      routeVO.setListenPort(clusterRoute.getListenPort());
      routeVO.setHaPort(clusterRoute.getHaPort());
      routeVO.setWeight(clusterRoute.getWeight());
      routeVO.setNodeState(clusterRoute.getNodeState());
      routeVO.setLastUpdateTimestamp(SDF.format(clusterRoute.getLastUpdateTimestamp()));

      if (routeVO.getSid() == leader) {
        routeArray[0] = routeVO;
      } else {
        routeArray[i] = routeVO;
        i++;
      }
    }

    return Arrays.asList(routeArray);
  }

  private int getLeaderSid(List<ClusterRoute> clusterRouteList) throws Exception {
    for (ClusterRoute clusterRoute : clusterRouteList) {
      if (NodeType.LEADING_NAME.equals(clusterRoute.getType())) {
        return clusterRoute.getSid();
      }
    }

    throw new Exception("Leader not found");
  }

  private ComponentManagementVO getTotalComponents() throws Exception {
    SendResult sendResult = defaultAdminClient.queryComponentSize();

    ComponentManagementVO componentManagementVO = new ComponentManagementVO();
    componentManagementVO.setTotal(sendResult.getSize());
    return componentManagementVO;
  }

  private ComponentVO buildComponentVO(Component component) {
    ComponentVO componentVO = new ComponentVO();
    componentVO.setStructure(component.getStructure());
    if (Structure.CR == component.getStructure()) {
      CRComponentData crcd = (CRComponentData) component;
      componentVO.setKey(crcd.getKey());
      componentVO.setCreateTime(SDF.format(crcd.getCreateTime()));
      componentVO.setUpdateTime(SDF.format(crcd.getUpdateTime()));
      componentVO.setExpression(crcd.getExpression());
    } else if (Structure.NS == component.getStructure()) {
      NSComponentData nscd = (NSComponentData) component;
      componentVO.setKey(nscd.getKey());
      componentVO.setCreateTime(SDF.format(nscd.getCreateTime()));
      componentVO.setUpdateTime(SDF.format(nscd.getUpdateTime()));
      componentVO.setInitNumber(nscd.getInitNumber());
      componentVO.setStepSize(nscd.getStepSize());
      componentVO.setResetType(nscd.getResetType());
    }

    return componentVO;
  }

  private Component buildComponent(ComponentVO componentVO) {
    int structure = componentVO.getStructure();
    Component component = null;
    if (Structure.CR == structure) {
      component = new CRComponentData();
      CRComponentData crcd = (CRComponentData) component;
      crcd.setKey(componentVO.getKey());
      crcd.setExpression(componentVO.getExpression());
    } else if (Structure.NS == structure) {
      component = new NSComponentData();
      NSComponentData nscd = (NSComponentData) component;
      nscd.setKey(componentVO.getKey());
      nscd.setInitNumber(componentVO.getInitNumber());
      nscd.setStepSize(componentVO.getStepSize());
      nscd.setResetType(componentVO.getResetType());
    }

    return component;
  }

  private List<BlockVO> buildComponentVO(int sid, List<ClusterDatabase> clusterDatabaseList) {
    List<ClusterBlock> clusterBlockList = null;
    for (ClusterDatabase clusterDatabase : clusterDatabaseList) {
      if (clusterDatabase.getSid() == sid) {
        clusterBlockList = clusterDatabase.getBlockList();
      }
    }

    if (clusterBlockList == null) {
      return null;
    }

    List<BlockVO> blockList = new ArrayList<>(clusterBlockList.size());
    for (ClusterBlock clusterBlock : clusterBlockList) {
      BlockVO blockVO = new BlockVO();
      blockVO.setBlockIndex(clusterBlock.getBlockIndex());
      blockVO.setComponentCount(clusterBlock.getComponentCount());
      blockVO.setBeginPhyOffset(clusterBlock.getBeginPhyOffset());
      blockVO.setEndPhyOffset(clusterBlock.getEndPhyOffset());
      blockVO.setBeginTimestamp(SDF.format(clusterBlock.getBeginTimestamp()));
      blockVO.setEndTimestamp(SDF.format(clusterBlock.getEndTimestamp()));

      blockList.add(blockVO);
    }

    return blockList;
  }
}
