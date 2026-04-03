package com.packed_go.event_service.services;

import com.packed_go.event_service.dtos.ticketConsumptionDetail.RedeemTicketDetailDTO;
import com.packed_go.event_service.dtos.ticketConsumptionDetail.TicketConsumptionDetailDTO;
import com.packed_go.event_service.entities.TicketConsumptionDetail;
import jakarta.transaction.Transactional;

import java.util.List;

public interface TicketConsumptionDetailService {


    TicketConsumptionDetailDTO create(TicketConsumptionDetailDTO ticketConsumptionDetail);

    TicketConsumptionDetailDTO update(Long id, TicketConsumptionDetailDTO ticketConsumptionDetail);

    List<TicketConsumptionDetailDTO> findAllByTicketId(Long id);
    List<TicketConsumptionDetailDTO> findAllByEntryTicketId(Long ticketId);
    List<TicketConsumptionDetailDTO> findAllByConsumptionName(String name);

    @Transactional
    TicketConsumptionDetail deleteLogical(Long id);

    @Transactional
    RedeemTicketDetailDTO redeemDetail(Long detailId);

    @Transactional
    TicketConsumptionDetailDTO redeemDetailPartial(Long detailId, Integer quantityToRedeem);

    TicketConsumptionDetailDTO findById(Long detailId);

}
