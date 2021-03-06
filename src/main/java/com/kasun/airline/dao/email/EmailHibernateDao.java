package com.kasun.airline.dao.email;

import com.kasun.airline.dao.AbstractDao;
import com.kasun.airline.model.email.EmailModel;
import org.springframework.stereotype.Repository;

@Repository("emailHibernateDao")
public class EmailHibernateDao extends AbstractDao<Long, EmailModel> implements EmailDao {

    @Override
    public void saveEmailData(EmailModel emailModel) {
        getSession().save(emailModel);
    }
}
