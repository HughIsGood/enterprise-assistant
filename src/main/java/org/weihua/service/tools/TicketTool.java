package org.weihua.service.tools;

import org.springframework.stereotype.Service;

@Service
public class TicketTool {

    public String createSupportTicket(String title, String content) {
        String ticketNo = "TICKET-" + System.currentTimeMillis();
        return "工单创建成功，工单号: " + ticketNo;
    }
}
