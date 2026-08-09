package com.aics.mq.service;

import com.aics.common.exception.BusinessException;
import com.aics.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.remoting.protocol.admin.ConsumeStats;
import org.apache.rocketmq.remoting.protocol.admin.OffsetWrapper;
import org.apache.rocketmq.remoting.protocol.admin.TopicOffset;
import org.apache.rocketmq.remoting.protocol.admin.TopicStatsTable;
import org.apache.rocketmq.remoting.protocol.body.ClusterInfo;
import org.apache.rocketmq.remoting.protocol.body.GroupList;
import org.apache.rocketmq.remoting.protocol.body.KVTable;
import org.apache.rocketmq.remoting.protocol.body.TopicList;
import org.apache.rocketmq.remoting.protocol.route.BrokerData;
import org.apache.rocketmq.remoting.protocol.route.QueueData;
import org.apache.rocketmq.remoting.protocol.route.TopicRouteData;
import org.apache.rocketmq.tools.admin.DefaultMQAdminExt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * RocketMQ 管理服务：通过 Admin API 查询集群 / Topic / 消费组 / 堆积
 */
@Slf4j
@Service
public class RocketMqAdminService {

    /** 系统内置 Topic 前缀，概览/列表时过滤 */
    private static final Set<String> SYSTEM_TOPIC_PREFIX = Set.of("RMQ_SYS_", "TBW102", "%RETRY%", "%DLQ%");

    @Value("${mq.namesrv-addr:127.0.0.1:9876}")
    private String namesrvAddr;

    private DefaultMQAdminExt createAdmin() {
        try {
            DefaultMQAdminExt admin = new DefaultMQAdminExt();
            admin.setNamesrvAddr(namesrvAddr);
            admin.setInstanceName("mq-admin-" + UUID.randomUUID());
            admin.setVipChannelEnabled(false);
            admin.start();
            return admin;
        } catch (Exception e) {
            log.error("连接 RocketMQ NameServer 失败: {}", namesrvAddr, e);
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "RocketMQ 连接失败: " + e.getMessage());
        }
    }

    /** 概览：集群/Broker 数、Topic 数、消费组数、总堆积 */
    public Map<String, Object> overview() {
        DefaultMQAdminExt admin = createAdmin();
        try {
            Map<String, Object> data = new LinkedHashMap<>();
            List<Map<String, Object>> brokers = cluster(admin);
            List<Map<String, Object>> topics = topics(admin);
            List<Map<String, Object>> groups = groups(admin);
            data.put("brokerCount", brokers.size());
            data.put("topicCount", topics.size());
            data.put("groupCount", groups.size());
            data.put("totalDiff", groups.stream().mapToLong(g -> ((Number) g.get("diff")).longValue()).sum());
            return data;
        } catch (Exception e) {
            throw wrap(e);
        } finally {
            admin.shutdown();
        }
    }

    /** 集群/Broker 列表 */
    public List<Map<String, Object>> cluster() {
        DefaultMQAdminExt admin = createAdmin();
        try {
            return cluster(admin);
        } catch (Exception e) {
            throw wrap(e);
        } finally {
            admin.shutdown();
        }
    }

    private List<Map<String, Object>> cluster(DefaultMQAdminExt admin) throws Exception {
        List<Map<String, Object>> list = new ArrayList<>();
        ClusterInfo clusterInfo = admin.examineBrokerClusterInfo();
        for (Map.Entry<String, BrokerData> entry : clusterInfo.getBrokerAddrTable().entrySet()) {
            BrokerData broker = entry.getValue();
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("brokerName", broker.getBrokerName());
            item.put("cluster", broker.getCluster());
            String masterAddr = broker.getBrokerAddrs().get(0L);
            item.put("masterAddr", masterAddr);
            item.put("slaveAddrs", broker.getBrokerAddrs().values().stream()
                    .filter(a -> !a.equals(masterAddr)).toList());
            // 运行时信息
            try {
                KVTable kv = admin.fetchBrokerRuntimeStats(masterAddr);
                Map<String, String> t = kv.getTable();
                item.put("version", t.getOrDefault("brokerVersionDesc", "-"));
                item.put("commitLogDiskRatio", t.getOrDefault("commitLogDiskRatio", "-"));
                item.put("putMessageEntireTimeMax", t.getOrDefault("putMessageEntireTimeMax", "-"));
                item.put("qps", t.getOrDefault("qps", "-"));
            } catch (Exception e) {
                item.put("version", "-");
                item.put("runtimeError", e.getMessage());
            }
            list.add(item);
        }
        return list;
    }

    /** Topic 列表（含队列数与消息量） */
    public List<Map<String, Object>> topics() {
        DefaultMQAdminExt admin = createAdmin();
        try {
            return topics(admin);
        } catch (Exception e) {
            throw wrap(e);
        } finally {
            admin.shutdown();
        }
    }

    private List<Map<String, Object>> topics(DefaultMQAdminExt admin) throws Exception {
        List<Map<String, Object>> list = new ArrayList<>();
        TopicList topicList = admin.fetchAllTopicList();
        for (String topic : topicList.getTopicList()) {
            if (isSystemTopic(topic)) {
                continue;
            }
            Map<String, Object> item = topicSummary(admin, topic);
            list.add(item);
        }
        return list;
    }

    private Map<String, Object> topicSummary(DefaultMQAdminExt admin, String topic) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("topic", topic);
        try {
            TopicRouteData route = admin.examineTopicRouteInfo(topic);
            List<String> brokers = new ArrayList<>();
            int readQueues = 0;
            int writeQueues = 0;
            for (QueueData qd : route.getQueueDatas()) {
                brokers.add(qd.getBrokerName());
                readQueues = Math.max(readQueues, qd.getReadQueueNums());
                writeQueues = Math.max(writeQueues, qd.getWriteQueueNums());
            }
            item.put("brokers", brokers);
            item.put("readQueues", readQueues);
            item.put("writeQueues", writeQueues);
            // 消息量 = Σ(maxOffset - minOffset)
            long count = 0;
            try {
                TopicStatsTable stats = admin.examineTopicStats(topic);
                for (TopicOffset off : stats.getOffsetTable().values()) {
                    count += Math.max(0, off.getMaxOffset() - off.getMinOffset());
                }
            } catch (Exception e) {
                log.warn("获取 Topic 统计失败: {}", topic, e);
            }
            item.put("messageCount", count);
        } catch (Exception e) {
            item.put("error", e.getMessage());
        }
        return item;
    }

    /** Topic 详情（队列分布） */
    public Map<String, Object> topicDetail(String topic) {
        DefaultMQAdminExt admin = createAdmin();
        try {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("topic", topic);
            TopicRouteData route = admin.examineTopicRouteInfo(topic);
            List<Map<String, Object>> queues = new ArrayList<>();
            for (QueueData qd : route.getQueueDatas()) {
                Map<String, Object> q = new LinkedHashMap<>();
                q.put("broker", qd.getBrokerName());
                q.put("readQueueNums", qd.getReadQueueNums());
                q.put("writeQueueNums", qd.getWriteQueueNums());
                q.put("perm", qd.getPerm());
                queues.add(q);
            }
            data.put("queues", queues);
            return data;
        } catch (Exception e) {
            throw wrap(e);
        } finally {
            admin.shutdown();
        }
    }

    /** 消费组列表（含消费 TPS 与堆积） */
    public List<Map<String, Object>> groups() {
        DefaultMQAdminExt admin = createAdmin();
        try {
            return groups(admin);
        } catch (Exception e) {
            throw wrap(e);
        } finally {
            admin.shutdown();
        }
    }

    private List<Map<String, Object>> groups(DefaultMQAdminExt admin) throws Exception {
        // 通过 Topic 反向收集消费组（去重）
        Set<String> groupSet = new LinkedHashSet<>();
        TopicList topicList = admin.fetchAllTopicList();
        for (String topic : topicList.getTopicList()) {
            if (isSystemTopic(topic)) {
                continue;
            }
            try {
                GroupList gl = admin.queryTopicConsumeByWho(topic);
                if (gl != null && gl.getGroupList() != null) {
                    groupSet.addAll(gl.getGroupList());
                }
            } catch (Exception e) {
                log.warn("查询 Topic 消费组失败: topic={}", topic, e);
            }
        }

        List<Map<String, Object>> list = new ArrayList<>();
        for (String group : groupSet) {
            list.add(groupSummary(admin, group));
        }
        return list;
    }

    private Map<String, Object> groupSummary(DefaultMQAdminExt admin, String group) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("group", group);
        try {
            ConsumeStats stats = admin.examineConsumeStats(group);
            long diff = 0;
            Set<String> topics = new LinkedHashSet<>();
            for (Map.Entry<MessageQueue, OffsetWrapper> e : stats.getOffsetTable().entrySet()) {
                OffsetWrapper ow = e.getValue();
                diff += Math.max(0, ow.getBrokerOffset() - ow.getConsumerOffset());
                topics.add(e.getKey().getTopic());
            }
            item.put("consumeTps", String.format("%.2f", stats.getConsumeTps()));
            item.put("diff", diff);
            item.put("topics", topics);
        } catch (Exception e) {
            item.put("error", e.getMessage());
        }
        return item;
    }

    /** 消费组详情（队列级 offset / 堆积） */
    public Map<String, Object> groupDetail(String group) {
        DefaultMQAdminExt admin = createAdmin();
        try {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("group", group);
            ConsumeStats stats = admin.examineConsumeStats(group);
            List<Map<String, Object>> queues = new ArrayList<>();
            for (Map.Entry<MessageQueue, OffsetWrapper> e : stats.getOffsetTable().entrySet()) {
                MessageQueue mq = e.getKey();
                OffsetWrapper ow = e.getValue();
                Map<String, Object> q = new LinkedHashMap<>();
                q.put("topic", mq.getTopic());
                q.put("broker", mq.getBrokerName());
                q.put("queueId", mq.getQueueId());
                q.put("brokerOffset", ow.getBrokerOffset());
                q.put("consumerOffset", ow.getConsumerOffset());
                q.put("diff", Math.max(0, ow.getBrokerOffset() - ow.getConsumerOffset()));
                q.put("lastTimestamp", ow.getLastTimestamp());
                queues.add(q);
            }
            data.put("consumeTps", String.format("%.2f", stats.getConsumeTps()));
            data.put("queues", queues);
            return data;
        } catch (Exception e) {
            throw wrap(e);
        } finally {
            admin.shutdown();
        }
    }

    private boolean isSystemTopic(String topic) {
        for (String p : SYSTEM_TOPIC_PREFIX) {
            if (topic.startsWith(p)) {
                return true;
            }
        }
        return false;
    }

    private BusinessException wrap(Exception e) {
        log.error("RocketMQ Admin 操作失败", e);
        return new BusinessException(ResultCode.INTERNAL_ERROR, "RocketMQ 操作失败: " + e.getMessage());
    }
}