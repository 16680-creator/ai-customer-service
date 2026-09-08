package com.aics.mq.service;

import com.aics.common.exception.BusinessException;
import com.aics.mq.dto.WorkOrderCreateRequest;
import com.aics.mq.dto.WorkOrderMessage;
import com.aics.mq.entity.MqFailRecord;
import com.aics.mq.entity.WorkOrder;
import com.aics.mq.mapper.MqFailRecordMapper;
import com.aics.mq.mapper.WorkOrderMapper;
import com.aics.mq.producer.WorkOrderProducer;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 工单消息演示服务：创建（插库+发消息）、列表、失败记录查询与重推。
 * <p>
 * 创建采用"先插库后发消息"（教学场景不引入分布式事务；
 * 生产上可演进为事务消息——项目中 ai-cs-pay 已有完整范例，或本地消息表 + 对账补偿）。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkOrderService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** 列表最大返回条数（演示数据量小，倒序取最新即可） */
    private static final int LIST_LIMIT = 100;

    private final WorkOrderMapper workOrderMapper;
    private final MqFailRecordMapper mqFailRecordMapper;
    private final WorkOrderProducer workOrderProducer;

    /**
     * 创建工单并发送 RocketMQ 消息
     *
     * @return 已落库的工单（含工单号）
     */
    public WorkOrder create(WorkOrderCreateRequest request) {
        WorkOrder order = new WorkOrder();
        order.setTicketNo(generateTicketNo());
        order.setTitle(request.getTitle());
        order.setContent(request.getContent());
        order.setStatus("NEW");
        workOrderMapper.insert(order);

        WorkOrderMessage message = new WorkOrderMessage();
        message.setTicketNo(order.getTicketNo());
        message.setTitle(order.getTitle());
        message.setContent(order.getContent());
        message.setSimulateFail(Boolean.TRUE.equals(request.getSimulateFail()));
        workOrderProducer.send(message);
        return order;
    }

    /** 工单列表（最新在前） */
    public List<WorkOrder> listWorkOrders() {
        return workOrderMapper.selectList(new LambdaQueryWrapper<WorkOrder>()
                .orderByDesc(WorkOrder::getId)
                .last("LIMIT " + LIST_LIMIT));
    }

    /** 消费失败记录列表（最新在前） */
    public List<MqFailRecord> listFailRecords() {
        return mqFailRecordMapper.selectList(new LambdaQueryWrapper<MqFailRecord>()
                .orderByDesc(MqFailRecord::getId)
                .last("LIMIT " + LIST_LIMIT));
    }

    /**
     * 重推失败记录：按原消息体重新投递（去掉模拟失败标志），并把记录标记为 REPUSHED。
     * <p>重推后消费是否成功由消费者决定，前端可从工单列表观察最终状态。</p>
     *
     * @param id 失败记录ID
     */
    public void repush(Long id) {
        MqFailRecord record = mqFailRecordMapper.selectById(id);
        if (record == null) {
            throw new BusinessException("失败记录不存在: id=" + id);
        }
        if (!"PENDING".equals(record.getStatus())) {
            throw new BusinessException("该记录已重推，不能重复操作: id=" + id);
        }
        try {
            WorkOrderMessage message = MAPPER.readValue(record.getPayload(), WorkOrderMessage.class);
            message.setSimulateFail(false); // 重推走正常消费路径
            workOrderProducer.send(message);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("重推失败（消息未发出）：" + e.getMessage());
        }
        MqFailRecord update = new MqFailRecord();
        update.setId(record.getId());
        update.setStatus("REPUSHED");
        update.setRepushTime(LocalDateTime.now());
        mqFailRecordMapper.updateById(update);
        log.info("失败记录已重推: id={}, ticketNo={}", id, record.getBizKey());
    }

    /** 生成工单号：WO + 毫秒时间戳 + 3位随机数（唯一键兜底） */
    private String generateTicketNo() {
        return "WO" + System.currentTimeMillis() + ThreadLocalRandom.current().nextInt(100, 1000);
    }
}
