package org.weihua.service.tools;

import org.springframework.stereotype.Service;
import org.weihua.model.tools.ToolCallResult;

@Service
public class ToolExecutionService {

    private final DocumentTool documentTool;
    private final TicketTool ticketTool;

    public ToolExecutionService(DocumentTool documentTool, TicketTool ticketTool) {
        this.documentTool = documentTool;
        this.ticketTool = ticketTool;
    }

    public ToolCallResult executeDocumentList(String documentType) {
        String result = documentTool.listDocumentsByType(documentType);
        return new ToolCallResult("listDocumentsByType", result);
    }

    public ToolCallResult executeCreateTicket(String title, String content) {
        String result = ticketTool.createSupportTicket(title, content);
        return new ToolCallResult("createSupportTicket", result);
    }
}