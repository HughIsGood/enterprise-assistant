package org.weihua.service.tools;

import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Service;

@Service
public class TicketTool {

    @Tool("创建支持工单")
    public String createSupportTicket(String title, String content) {
        String ticketNo = "TICKET-" + System.currentTimeMillis();
        return "工单创建成功，工单号: " + ticketNo;
    }
}
