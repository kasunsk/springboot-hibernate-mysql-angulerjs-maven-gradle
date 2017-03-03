package com.kasun.airline.logic.airline;

import com.kasun.airline.common.dto.Price;
import com.kasun.airline.common.dto.UserTicketStatus;
import com.kasun.airline.common.execption.ErrorCode;
import com.kasun.airline.common.execption.ServiceRuntimeException;
import com.kasun.airline.common.service.StatelessServiceLogic;
import com.kasun.airline.dao.account.AccountDao;
import com.kasun.airline.dao.airline.AirlineDao;
import com.kasun.airline.dto.airline.TicketBuy;
import com.kasun.airline.dto.airline.TicketBuyingRequest;
import com.kasun.airline.model.account.BankAccount;
import com.kasun.airline.model.account.Currency;
import com.kasun.airline.model.airline.AirlineOfferModel;
import com.kasun.airline.model.user.UserTicket;
import com.kasun.airline.service.account.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;
import java.math.BigDecimal;

/**
 * Created by kasun on 3/2/17.
 */
@Component
public class AirlineTicketBuyingLogic extends StatelessServiceLogic<UserTicket, TicketBuy> {

    @Autowired
    private AccountDao accountDao;

    @Autowired
    private AccountService accountService;

    @Autowired
    private AirlineOfferLogicHelper helper;

    @Autowired
    private AirlineDao airlineDao;

    @Transactional
    @Override
    public UserTicket invoke(TicketBuy request) {

        TicketBuyingRequest ticketBuyingRequest = request.getTicketBuyingRequest();

        BankAccount applicantBankAccount = accountDao.loadAccountById(Long.parseLong(ticketBuyingRequest.getAccountId()));
        Double availableAmount = applicantBankAccount.getAvailableAmount();
        AirlineOfferModel airlineOffer = helper.loadOfferByRout(ticketBuyingRequest.getAirlineRout());
        validateAirlineOfferInventoryAvailability(airlineOffer, ticketBuyingRequest);
        Price offerPrice = getConvertedOfferPrice(airlineOffer, applicantBankAccount.getCurrency());
        Price price = calculatePaymentAmount(availableAmount, offerPrice, ticketBuyingRequest.getTicketAmount());
        processPayment(price, applicantBankAccount);

        UserTicket userTicket = getUserTicket(request.getApplicantId(), airlineOffer, ticketBuyingRequest);
        accountDao.saveUserTicket(userTicket);
        updateInventory(airlineOffer, ticketBuyingRequest);
        return userTicket;
    }

    private void validateAirlineOfferInventoryAvailability(AirlineOfferModel airlineOffer, TicketBuyingRequest request) {

        Integer availableInventory = airlineOffer.getAvailbaleInventory();

        if (availableInventory < request.getTicketAmount()) {
            throw new ServiceRuntimeException(ErrorCode.NO_ENOUGH_INV, "No enough inventory for Airline Offer ");
        }
    }

    private Price getConvertedOfferPrice(AirlineOfferModel airlineOffer, Currency accountCurrency) {

        Price price = new Price();
        price.setPrice(airlineOffer.getPrice());
        price.setCurrency(airlineOffer.getCurrency());

        if (!airlineOffer.getCurrency().equals(accountCurrency)) {
            price = accountService.currencyExchange(price, accountCurrency);
        }
        return price;
    }

    private Price calculatePaymentAmount(Double availableAmount, Price price, Integer ticketAmount) {

        BigDecimal payableAmount = BigDecimal.valueOf(price.getPrice()).multiply(new BigDecimal(ticketAmount));

        if (BigDecimal.valueOf(availableAmount).compareTo(payableAmount) == -1) {
            throw new ServiceRuntimeException(ErrorCode.NOT_ENOUGH_CREDIT, "Credit not enough in given account");
        }

        Price amount = new Price();
        amount.setCurrency(price.getCurrency());
        amount.setPrice(payableAmount.doubleValue());
        return amount;
    }

    private void processPayment(Price price, BankAccount applicantBankAccount) {

        Double newAccountBalance = applicantBankAccount.getAvailableAmount() - price.getPrice();
        applicantBankAccount.setAvailableAmount(newAccountBalance);
        accountDao.updateAccount(applicantBankAccount);
    }

    private UserTicket getUserTicket(String applicantId, AirlineOfferModel airlineOffer, TicketBuyingRequest request) {
        UserTicket userTicket = new UserTicket();
        userTicket.setUserId(Long.parseLong(applicantId));
        userTicket.setOfferId(airlineOffer.getOfferId());
        userTicket.setStatus(UserTicketStatus.BOUGHT);
        userTicket.setDestination(airlineOffer.getDestination());
        userTicket.setOrigin(airlineOffer.getOrigin());
        userTicket.setPrice(airlineOffer.getPrice());
        userTicket.setCurrency(airlineOffer.getCurrency());
        userTicket.setTicketsAmount(request.getTicketAmount());
        return userTicket;
    }

    private void updateInventory(AirlineOfferModel airlineOffer, TicketBuyingRequest request) {

        Integer newAvailableInventory = airlineOffer.getAvailbaleInventory() - request.getTicketAmount();
        airlineOffer.setAvailbaleInventory(newAvailableInventory);
        airlineDao.updateAirlineOffer(airlineOffer);

    }
}